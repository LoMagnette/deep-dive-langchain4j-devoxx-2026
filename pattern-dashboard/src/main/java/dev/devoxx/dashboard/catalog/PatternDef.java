package dev.devoxx.dashboard.catalog;

import dev.devoxx.dashboard.run.StreamingListener;
import dev.devoxx.dashboard.support.Errors;
import dev.langchain4j.model.chat.ChatModel;

/**
 * One entry of the catalogue: display metadata, a static topology for the SVG, and the live
 * wiring. {@link Runner} is that wiring; {@link PatternInfo} is what the page receives as JSON,
 * which is the same thing minus the runnable part.
 *
 * <p>{@code buildsOn} names what this demo inherits from earlier ones — the agents it reuses
 * and where they came from — or null when it stands alone. It is shown on the page because the
 * reuse is the point: by the capstone, almost everything on the diagram is something the room has
 * already watched run on its own, and nothing says so unless the page does.
 *
 * <p>{@code story} is the demo's beat in the running narration — where we are in Zao's life and
 * what has just happened. Read in catalogue order the seventeen of them tell one continuous
 * story, which is why the field exists at all: the rail order is the <b>autonomy dial</b>, and
 * the narration has to be written to fit that order rather than the order being rearranged to
 * fit the narration. Reordering the catalogue to make a better story would cost the talk its
 * thesis, and chaining one demo's output into the next one's input would mean a skipped or
 * failed demo strands everything after it. Each demo stays independently runnable.
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
