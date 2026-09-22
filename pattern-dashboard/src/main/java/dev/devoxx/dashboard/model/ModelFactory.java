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
import dev.devoxx.dashboard.run.ModelTiers;
import dev.devoxx.dashboard.support.Errors;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Decides which {@link ChatModel} the patterns run against, and keeps that decision honest.
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

    /**
     * Optional second, smaller model for the model-routing demo. Empty means "no cheap tier" —
     * that demo then runs both tiers against the one model and says so, rather than implying a
     * saving that never happened. Set it to something genuinely small (llama3.2:1b, qwen2.5:0.5b)
     * and pull it first.
     */
    @ConfigProperty(name = "dashboard.ollama.cheap-model-name")
    Optional<String> configuredCheapModelName;

    /** Generous by default: "thinking" models can spend a minute on one agent step. */
    @ConfigProperty(name = "dashboard.ollama.timeout", defaultValue = "PT2M")
    Duration timeout;

    private final Object lock = new Object();

    private volatile ChatModel model;
    private volatile ModelTiers tiers;
    private volatile StreamingChatModel streamingModel;
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
        ChatModel resolved = settle(build(found, wanted), "ollama " + wanted + " @ " + found, false);
        tiers = cheapTier(found, wanted, resolved);
        streamingModel = OllamaStreamingChatModel.builder()
                .baseUrl(found)
                .modelName(wanted)
                .timeout(timeout)
                .listeners(List.of(new ChatCallLog()))
                .build();
        return resolved;
    }

    /**
     * The streaming counterpart of {@link #currentModel()}, for the one demo that shows tokens
     * arriving. Resolved off the back of the same probe, so it can only be a real model when the
     * ordinary one is — a streaming demo pointed at a dead endpoint fails in a way that looks
     * like the pattern is broken rather than the server.
     */
    public StreamingChatModel currentStreamingModel() {
        ChatModel current = currentModel();
        StreamingChatModel known = streamingModel;
        return known != null && !fellBack && !(current instanceof MockChatModel)
                ? known
                : new MockStreamingChatModel(List.of(new ChatCallLog()));
    }

    /**
     * The cheap tier, when one is configured AND actually pulled. Probed the same way the main
     * model is, and for the same reason: a cheap tier that 404s mid-demo is worse than not
     * having one, because the demo will have already claimed it made a choice.
     */
    private ModelTiers cheapTier(String base, String wanted, ChatModel strong) {
        String cheap = configuredCheapModelName.map(String::trim).filter(s -> !s.isEmpty())
                .orElse(null);
        if (cheap == null || cheap.equals(wanted)) {
            return ModelTiers.single(strong, wanted);
        }
        String problem = probe(base, cheap);
        if (problem != null) {
            LOG.warnf("Cheap tier '%s' not usable at %s (%s) — the model-routing demo will run "
                    + "both tiers on '%s' and say so.", cheap, base, problem, wanted);
            return ModelTiers.single(strong, wanted);
        }
        LOG.infof("model tiers: cheap=%s strong=%s @ %s", cheap, wanted, base);
        return ModelTiers.of(build(base, cheap), cheap, strong, wanted);
    }

    /**
     * The two models a run may choose between. Never null: with nothing configured both tiers
     * are the live model, and {@code ModelTiers.distinct()} is false so the demo can be honest
     * about it.
     */
    public ModelTiers tiers() {
        ChatModel current = currentModel();
        ModelTiers known = tiers;
        // Not just `tiers`: a run that fell back to the mock, or recovered from it, must not keep
        // handing out tiers built around a model that is no longer live.
        if (known != null && known.strong() == current) {
            return known;
        }
        return ModelTiers.single(current, activeModel);
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
                // Not logRequests/logResponses: those log at DEBUG, so the best lines in the
                // demo are off unless someone raised a log level. This logs at INFO.
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
