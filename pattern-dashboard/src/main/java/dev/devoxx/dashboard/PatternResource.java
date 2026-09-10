package dev.devoxx.dashboard;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.resteasy.reactive.RestStreamElementType;

import dev.devoxx.dashboard.PatternDef.PatternInfo;
import dev.langchain4j.model.chat.ChatModel;
import io.smallrye.mutiny.Multi;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/api/patterns")
public class PatternResource {

    @Inject
    PatternCatalog catalog;

    @Inject
    ModelFactory models;

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
    public Multi<RunEvent> run(@PathParam("id") String id, @QueryParam("input") String input) {
        PatternDef def = catalog.byId(id).orElse(null);
        if (def == null) {
            return Multi.createFrom().item(RunEvent.of(0, "agent-error", "-",
                    "unknown pattern: " + id, Map.of(), null));
        }
        String in = (input == null || input.isBlank()) ? def.defaultInput() : input;

        return Multi.createFrom().emitter(em -> pool.submit(() -> {
            AtomicLong seq = new AtomicLong();
            StreamingListener listener = new StreamingListener(em::emit, seq);
            try {
                // Asked for per run, not injected once: a run that fell back to the mock re-probes
                // here, so starting Ollama recovers without restarting the app. Naming the live
                // model means a silent mock fallback can't be mistaken for a real run.
                ChatModel model = models.currentModel();
                em.emit(RunEvent.of(seq.getAndIncrement(), "run-start", def.name(),
                        "running '" + def.id() + "' [model: " + models.activeModel()
                                + "] with input: " + in, Map.of(), null));
                String out = def.run(model, in, listener);
                em.emit(RunEvent.of(seq.getAndIncrement(), "run-result", def.name(),
                        "final result", Map.of(), out));
                em.emit(RunEvent.of(seq.getAndIncrement(), "run-done", def.name(),
                        "done", Map.of(), null));
            } catch (Throwable t) {
                em.emit(RunEvent.of(seq.getAndIncrement(), "agent-error", def.name(),
                        "run failed: " + t, Map.of(), null));
            } finally {
                em.complete();
            }
        }));
    }
}
