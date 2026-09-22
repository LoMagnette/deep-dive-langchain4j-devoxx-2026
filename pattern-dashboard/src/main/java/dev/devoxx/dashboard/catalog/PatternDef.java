package dev.devoxx.dashboard.catalog;

import dev.devoxx.dashboard.run.StreamingListener;
import dev.devoxx.dashboard.support.Errors;
import dev.langchain4j.model.chat.ChatModel;

/**
 * One entry of the catalogue: display metadata, a static topology for the SVG, and the live
 * wiring. {@link Runner} is that wiring; {@link PatternInfo} is what the page receives as JSON,
 * which is the same thing minus the runnable part.
 */
public record PatternDef(String id, String name, String category, String story, String buildsOn,
                         String useful, String caveat, Topology.Graph topology,
                         String defaultInput, Runner runner, boolean streams) {

    /**
     * The usual form. {@code streams} is false for all but one demo, and a secondary constructor
     * is what keeps that one fact from costing nineteen other files a {@code , false} they would
     * have to be read past for ever.
     */
    public PatternDef(String id, String name, String category, String story, String buildsOn,
                      String useful, String caveat, Topology.Graph topology,
                      String defaultInput, Runner runner) {
        this(id, name, category, story, buildsOn, useful, caveat, topology, defaultInput, runner,
                false);
    }

    /** A pattern's live behaviour: wire the agents, invoke them, return what came back. */
    @FunctionalInterface
    public interface Runner {
        String run(ChatModel model, String input, StreamingListener listener) throws Exception;
    }

    /**
     * JSON view of a pattern (no runnable code). {@code streams} is here so the page can offer
     * the token toggle on the one demo that honours it, rather than showing a control that
     * silently does nothing on the other nineteen.
     */
    public record PatternInfo(String id, String name, String category, String story,
                              String buildsOn, String useful, String caveat,
                              Topology.Graph topology, String defaultInput, boolean streams) {
    }

    public PatternInfo toInfo() {
        return new PatternInfo(id, name, category, story, buildsOn, useful, caveat, topology,
                defaultInput, streams);
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
