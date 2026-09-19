package dev.devoxx.dashboard.web;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import dev.devoxx.dashboard.catalog.PatternCatalog;
import dev.devoxx.dashboard.catalog.PatternDef.PatternInfo;
import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.model.ModelFactory;
import dev.devoxx.dashboard.run.HumanQuestions;
import dev.devoxx.dashboard.run.RunEvent;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.model.chat.ChatModel;
import io.smallrye.mutiny.Multi;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestStreamElementType;

@Path("/api/patterns")
public class PatternResource {

    @Inject
    PatternCatalog catalog;

    @Inject
    ModelFactory models;

    @Inject
    HumanQuestions humans;

    // Small shared pool: runs are short and there are only a handful of concurrent viewers.
    private final ExecutorService pool = Executors.newFixedThreadPool(4);

    @PreDestroy
    void shutdown() {
        pool.shutdownNow();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<PatternInfo> list() {
        return catalog.infos();
    }

    @GET
    @Path("/{id}/run")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<RunEvent> run(@PathParam("id") String id, @QueryParam("input") String input,
                               @QueryParam("stream") boolean stream) {
        PatternDef def = catalog.byId(id).orElse(null);
        if (def == null) {
            return Multi.createFrom().item(RunEvent.of(0, "agent-error", "-",
                    "unknown pattern: " + id, Map.of(), null));
        }
        String in = (input == null || input.isBlank()) ? def.defaultInput() : input;

        // The SSE stream is one-way, so a run that wants to ask a person something needs an
        // address the answer can be posted back to. The id goes out in run-start, before any
        // question can be asked on it.
        String runId = UUID.randomUUID().toString();

        return Multi.createFrom().emitter(em -> {
            // Closing the tab cancels the subscription but tells the running thread nothing, so
            // without this a run blocked on a question nobody will now answer sits out the full
            // HumanQuestions.WAIT holding one of four pool threads — and the next few runs
            // silently never start. Fires on normal completion too, where it is a no-op.
            em.onTermination(() -> humans.cancel(runId));
            pool.submit(() -> {
            AtomicLong seq = new AtomicLong();
            // Asked for AND supported: a stream requested on a demo whose last agent cannot
            // produce one would otherwise take the streaming path, find no TokenStream and
            // silently behave like the ordinary run, which reads as a broken toggle.
            StreamingListener listener = new StreamingListener(em::emit, seq,
                    q -> humans.await(runId), models.tiers(),
                    stream && def.streams() ? models.currentStreamingModel() : null);
            try {
                // Asked for per run, not injected once: a run that fell back to the mock re-probes
                // here, so starting Ollama recovers without restarting the app. Naming the live
                // model means a silent mock fallback can't be mistaken for a real run.
                ChatModel model = models.currentModel();
                em.emit(RunEvent.of(seq.getAndIncrement(), "run-start", def.name(),
                        "running '" + def.id() + "' [model: " + models.activeModel()
                                + "] with input: " + in, Map.of(), runId));
                // Wall clock, deliberately: it is what the room experiences, and the gap between
                // it and the sum of the agent times is exactly what a parallel step buys you.
                long from = System.nanoTime();
                String out = def.run(model, in, listener);
                long took = (System.nanoTime() - from) / 1_000_000;
                em.emit(RunEvent.of(seq.getAndIncrement(), "run-result", def.name(),
                        "final result", Map.of(), out, took));
                em.emit(RunEvent.of(seq.getAndIncrement(), "run-done", def.name(),
                        "done in " + took + " ms", Map.of(), null, took));
            } catch (Throwable t) {
                em.emit(RunEvent.of(seq.getAndIncrement(), "agent-error", def.name(),
                        "run failed: " + t, Map.of(), null));
            } finally {
                // Belt and braces with onTermination above: that one covers the viewer
                // disappearing, this one covers the run ending while a question is outstanding.
                humans.cancel(runId);
                em.complete();
            }
            });
        });
    }

    /**
     * The other half of a human-in-the-loop run: the answer, posted back against the run id the
     * stream announced.
     *
     * <p><b>409</b> rather than 404 when the run is not asking anything, because the common cause
     * is a late answer to a question that already timed out — the run exists, it just is not
     * waiting any more, and a 404 would send the caller looking for the wrong problem.
     */
    @POST
    @Path("/runs/{runId}/answer")
    @Produces(MediaType.APPLICATION_JSON)
    public Response answer(@PathParam("runId") String runId, @QueryParam("text") String text) {
        boolean accepted = humans.answer(runId, text);
        return Response.status(accepted ? Response.Status.OK : Response.Status.CONFLICT)
                .entity(Map.of("accepted", accepted,
                        "detail", accepted ? "delivered"
                                : "that run is not waiting for an answer"))
                .build();
    }
}
