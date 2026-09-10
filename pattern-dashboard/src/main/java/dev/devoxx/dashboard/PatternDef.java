package dev.devoxx.dashboard;

import dev.langchain4j.model.chat.ChatModel;

/**
 * One entry of the catalogue: display metadata, a static topology for the SVG, and the live
 * wiring. {@link Runner} is that wiring; {@link PatternInfo} is what the page receives as JSON,
 * which is the same thing minus the runnable part.
 */
public record PatternDef(String id, String name, String category, String useful, String caveat,
                         Topology.Graph topology, String defaultInput, Runner runner) {

    /** A pattern's live behaviour: wire the agents, invoke them, return what came back. */
    @FunctionalInterface
    public interface Runner {
        String run(ChatModel model, String input, StreamingListener listener) throws Exception;
    }

    /** JSON view of a pattern (no runnable code). */
    public record PatternInfo(String id, String name, String category, String useful,
                              String caveat, Topology.Graph topology, String defaultInput) {
    }

    public PatternInfo toInfo() {
        return new PatternInfo(id, name, category, useful, caveat, topology, defaultInput);
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
