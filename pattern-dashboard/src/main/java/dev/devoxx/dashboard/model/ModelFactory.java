package dev.devoxx.dashboard.model;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.devoxx.dashboard.support.Errors;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Decides which {@link ChatModel} the patterns run against, and keeps that decision honest.
 *
 * <p>{@code dashboard.model} picks the backend:
 * <ul>
 *   <li>{@code auto} (the default) — find a live Ollama endpoint serving the wanted model; if
 *       there isn't one, fall back to the deterministic {@link MockChatModel} with a loud
 *       warning. A dead Ollama then costs a degraded demo instead of thirteen failing ones.</li>
 *   <li>{@code ollama} — use the endpoint, no fallback (fail loudly if it's down).</li>
 *   <li>{@code mock} — always the deterministic offline model.</li>
 * </ul>
 *
 * <p>Two things here exist because they bit us on this project:
 *
 * <p><b>The base URL is discovered, not hardcoded.</b> The right host depends on where the app
 * runs, and getting it wrong is invisible: {@code localhost} is correct on the speaker's laptop
 * but reaches nothing from inside a container, while {@code host.docker.internal} is correct
 * inside a container but does not resolve at all on the laptop — and an unresolvable host fails
 * exactly like a stopped Ollama. So {@link #CANDIDATES} are probed in order unless
 * {@code OLLAMA_BASE_URL} pins one explicitly.
 *
 * <p><b>The probe checks the model, not just the socket.</b> {@code /api/tags} is one cheap
 * round-trip that both proves the server is up and lists what it can serve, so a typo or an
 * un-pulled model is reported at boot by name instead of surfacing mid-demo.
 */
@ApplicationScoped
public class ModelFactory {

    private static final Logger LOG = Logger.getLogger(ModelFactory.class);

    /** Kept short: a missing Ollama should cost a second per candidate, not a minute. */
    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(5);

    /** After a failed probe, wait this long before trying again rather than re-probing per run. */
    private static final Duration RETRY_AFTER = Duration.ofSeconds(10);

    /** Tried in order when nothing is pinned: "Ollama is on this machine", then "…on the host". */
    private static final List<String> CANDIDATES =
            List.of("http://localhost:11434", "http://host.docker.internal:11434");

    private static final ObjectMapper JSON = new ObjectMapper();

    @ConfigProperty(name = "dashboard.model", defaultValue = "auto")
    String modelKind;

    /** Empty means "discover it". Set {@code OLLAMA_BASE_URL} to pin a specific endpoint. */
    @ConfigProperty(name = "dashboard.ollama.base-url")
    Optional<String> configuredBaseUrl;

    @ConfigProperty(name = "dashboard.ollama.model-name", defaultValue = "llama3.1")
    String configuredModelName;

    /** Generous by default: "thinking" models can spend a minute on one agent step. */
    @ConfigProperty(name = "dashboard.ollama.timeout", defaultValue = "PT2M")
    Duration timeout;

    private final Object lock = new Object();

    private volatile ChatModel model;
    private volatile String activeModel = "not initialised";
    /** True only when we wanted a real model and had to settle for the mock. */
    private volatile boolean fellBack;
    private volatile long retryAtNanos;

    /** Human-readable description of what is actually being used, for the UI and the logs. */
    public String activeModel() {
        return activeModel;
    }

    /**
     * The model to run a pattern against.
     *
     * <p>Re-probes when the last attempt fell back to the mock, so starting Ollama (or fixing the
     * model name) recovers on the next run instead of requiring a restart — losing a demo to a
     * decision cached at boot is a bad way to spend time on stage.
     */
    public ChatModel currentModel() {
        ChatModel current = model;
        if (current != null && !fellBack) {
            return current;
        }
        synchronized (lock) {
            if (model != null && (!fellBack || System.nanoTime() - retryAtNanos < 0)) {
                return model;
            }
            return resolve();
        }
    }

    /**
     * Observing {@link StartupEvent} makes this bean eager, so the probe (and its warning) happens
     * at boot where the speaker will see it — not lazily on the first run, by which time the page
     * has already reported the wrong model.
     */
    void onStartup(@Observes StartupEvent event) {
        synchronized (lock) {
            resolve();
        }
    }

    private ChatModel resolve() {
        String wanted = configuredModelName.trim();

        if ("mock".equalsIgnoreCase(modelKind)) {
            return settle(loggingMock(), "mock (deterministic, offline)", false);
        }

        boolean auto = "auto".equalsIgnoreCase(modelKind);
        if (!auto && !"ollama".equalsIgnoreCase(modelKind)) {
            LOG.warnf("Unknown dashboard.model '%s' — using the mock. Expected: auto, ollama, mock.",
                    modelKind);
            return settle(loggingMock(), "mock (unknown dashboard.model '" + modelKind + "')",
                    false);
        }

        List<String> tried = new ArrayList<>();
        String found = discover(wanted, tried);

        if (found == null) {
            String problems = String.join(", ", tried);
            if (!auto) {
                // dashboard.model=ollama means "tell me it's broken", so build a model that will
                // fail on use rather than quietly swapping in the mock.
                String base = baseUrls().get(0);
                LOG.errorf("No Ollama serving '%s' (%s), and dashboard.model=ollama disables the "
                        + "mock fallback. Runs against %s will fail.", wanted, problems, base);
                return settle(build(base, wanted), "ollama " + wanted + " @ " + base
                        + " (UNVERIFIED — probe failed: " + problems + ")", false);
            }
            LOG.warnf("""
                    Cannot reach an Ollama serving model '%s'.
                      tried: %s
                    Falling back to the deterministic MockChatModel so the demo still runs.
                    To use a real model:  ollama serve  &&  ollama pull %s
                    Point somewhere else: OLLAMA_BASE_URL=http://host:11434  OLLAMA_MODEL=name
                    Set dashboard.model=ollama to disable this fallback and fail loudly instead.""",
                    wanted, problems, wanted);
            return settle(loggingMock(), "mock — FALLBACK, no Ollama serving '" + wanted
                    + "': " + problems, true);
        }

        LOG.infof("dashboard model: ollama %s @ %s", wanted, found);
        return settle(build(found, wanted), "ollama " + wanted + " @ " + found, false);
    }

    /** Returns the first base URL serving {@code wanted}, or null — appending each failure. */
    private String discover(String wanted, List<String> tried) {
        for (String base : baseUrls()) {
            String problem = probe(base, wanted);
            if (problem == null) {
                return base;
            }
            tried.add(base + " (" + problem + ")");
        }
        return null;
    }

    private List<String> baseUrls() {
        return configuredBaseUrl
                .map(String::trim)
                .filter(url -> !url.isEmpty())
                .map(url -> List.of(trimTrailingSlash(url)))
                .orElse(CANDIDATES);
    }

    /**
     * One cheap round-trip against Ollama's model list. Returns null when the endpoint is up and
     * serving {@code wanted}, else why it isn't.
     *
     * <p>Deliberately not a real {@code chat()} call: that runs inference, and a thinking model can
     * take 10-20s to answer even "ping" — comfortably past {@link #PROBE_TIMEOUT}, which made the
     * probe fail (and silently fall back) while Ollama was up and healthy the whole time.
     */
    private String probe(String baseUrl, String wanted) {
        List<String> available;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/tags"))
                    .timeout(PROBE_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response = HttpClient.newBuilder()
                    .connectTimeout(PROBE_TIMEOUT)
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                return "HTTP " + response.statusCode() + " from /api/tags";
            }
            available = modelNames(response.body());
        } catch (Exception e) {
            return Errors.explain(e);
        }
        if (available.contains(wanted) || available.contains(wanted + ":latest")) {
            return null;
        }
        return "up, but model '" + wanted + "' not found — has: "
                + (available.isEmpty() ? "nothing pulled" : String.join(", ", available));
    }

    private static List<String> modelNames(String tagsJson) throws Exception {
        List<String> names = new ArrayList<>();
        for (JsonNode node : JSON.readTree(tagsJson).path("models")) {
            String name = node.path("name").asText("");
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        return names;
    }

    private ChatModel build(String baseUrl, String modelName) {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .timeout(timeout)
                // Not logRequests/logResponses: those log at DEBUG, which means the most
                // interesting lines in the demo are invisible unless somebody remembered to
                // raise a log level first. The listener logs at INFO, in one line per call,
                // formatted for the Server log tab.
                .listeners(List.of(new ChatCallLog()))
                .build();
    }

    /**
     * The offline model, wired to the same {@link ChatCallLog} the real one uses — so the Server
     * log tab shows prompts and answers whether or not Ollama is running, which is exactly the
     * situation the mock exists for.
     */
    private static MockChatModel loggingMock() {
        return new MockChatModel(List.of(new ChatCallLog()));
    }

    /** Records the decision so {@link #activeModel()} and the retry clock stay in step with it. */
    private ChatModel settle(ChatModel resolved, String description, boolean isFallback) {
        model = resolved;
        activeModel = description;
        fellBack = isFallback;
        retryAtNanos = System.nanoTime() + RETRY_AFTER.toNanos();
        return resolved;
    }

    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
