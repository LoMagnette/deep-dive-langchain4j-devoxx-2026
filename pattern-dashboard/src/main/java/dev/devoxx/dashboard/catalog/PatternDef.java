package dev.devoxx.dashboard.catalog;

import dev.devoxx.dashboard.run.StreamingListener;
import dev.devoxx.dashboard.support.Errors;
import dev.langchain4j.model.chat.ChatModel;

/**
 * One entry of the catalogue: display metadata, a static topology for the SVG, and the live
 * wiring. {@link Runner} is that wiring; {@link PatternInfo} is what the page receives as JSON,
 * which is the same thing minus the runnable part.
 *
 * <p>{@code story} is the demo's beat in the running narration — where we are in Zao's life and
 * what has just happened. Read in catalogue order the seventeen of them tell one continuous
 * story, which is why the field exists at all: the rail order is the <b>autonomy dial</b>, and
 * the narration has to be written to fit that order rather than the order being rearranged to
 * fit the narration. Reordering the catalogue to make a better story would cost the talk its
 * thesis, and chaining one demo's output into the next one's input would mean a skipped or
 * failed demo strands everything after it. Each demo stays independently runnable.
 */
public record PatternDef(String id, String name, String category, String story, String useful,
                         String caveat, Topology.Graph topology, String defaultInput,
                         Runner runner) {

    /** A pattern's live behaviour: wire the agents, invoke them, return what came back. */
    @FunctionalInterface
    public interface Runner {
        String run(ChatModel model, String input, StreamingListener listener) throws Exception;
    }

    /** JSON view of a pattern (no runnable code). */
    public record PatternInfo(String id, String name, String category, String story, String useful,
                              String caveat, Topology.Graph topology, String defaultInput) {
    }

    public PatternInfo toInfo() {
        return new PatternInfo(id, name, category, story, useful, caveat, topology, defaultInput);
    }

    /** Never throws: on failure it emits an error event and returns an explanation. */
    public String run(ChatModel model, String input, StreamingListener listener) {
        try {
            return runner.run(model, input, listener);
        } catch (Exception e) {
            // Errors.explain, not getMessage(): the top-level AgentInvocationException only
            // says "Failed to invoke agent method", which is true of every failure.
            String why = Errors.explain(e);
            listener.emitError(id, "pattern '" + id + "' failed: " + why);
            return "This pattern could not complete: " + why;
        }
    }
}
