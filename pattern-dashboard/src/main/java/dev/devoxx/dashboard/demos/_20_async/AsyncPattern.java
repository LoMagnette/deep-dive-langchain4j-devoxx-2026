package dev.devoxx.dashboard.demos._20_async;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static java.util.Objects.requireNonNullElse;

import java.util.List;
import java.util.Map;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._20_async.Keys.VetLine;
import dev.devoxx.dashboard.demos._04_parallel.Keys.Meals;
import dev.devoxx.dashboard.demos._04_parallel.Keys.Stay;
import dev.devoxx.dashboard.demos._04_parallel.Keys.Walks;
import dev.devoxx.dashboard.demos._04_parallel.MealPlanner;
import dev.devoxx.dashboard.demos._04_parallel.WalkPlanner;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for the <b>asynchronous agent</b> demo — a sequence with one step that does not block.
 */
public final class AsyncPattern {

    private AsyncPattern() {
    }

    /** The wiring. Everything below it is the dashboard telling itself how to draw this. */
    static String run(ChatModel model, String input, StreamingListener listener) {
        // The only line that differs from an ordinary sequence. The agent is not written any
        // differently and the builder is not a different builder — async is one call on this one
        // step, and the two planners below it are demo 4's, unchanged.
        var vet = AgenticServices.agentBuilder(VetCallback.class)
                .chatModel(model)
                .name("VetCallback")
                .outputKey(VetLine.class)
                .async(true)
                .build();
        var meals = AgenticServices.agentBuilder(MealPlanner.class)
                .chatModel(model)
                .name("MealPlanner")
                .outputKey(Meals.class)
                .build();
        var walks = AgenticServices.agentBuilder(WalkPlanner.class)
                .chatModel(model)
                .name("WalkPlanner")
                .outputKey(Walks.class)
                .build();
        UntypedAgent app = AgenticServices.sequenceBuilder()
                // Declaration order is still a sequence: the vet is asked FIRST. It just does not
                // hold the other two up, because its answer is not needed until the note.
                .subAgents(vet, meals, walks)
                .output(AsyncPattern::note)
                .listener(listener)
                .build();
        var r = app.invokeWithAgenticScope(Map.of(new Stay().name(), input));
        return String.valueOf(r.result());
    }

    /**
     * The join, and it does not look like one — which is the lesson.
     *
     * <p>While {@code VetCallback} is still running the scope holds an {@code AsyncResponse}
     * under {@code vetline}, and {@code readState} blocks on it. So the waiting happens HERE, on
     * the line that needs the value, and not at the step that started the work. Read the meals
     * and the walks first and the vet has that much longer to answer for free.
     */
    private static String note(AgenticScope scope) {
        return "**Meals**\n\n" + requireNonNullElse(scope.readState(Meals.class), "")
                + "\n\n**Walks**\n\n" + requireNonNullElse(scope.readState(Walks.class), "")
                + "\n\n**Vet cover** *(the run waited here, and only here)*\n\n"
                + requireNonNullElse(scope.readState(VetLine.class), "");
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // Stages, not chain: the async step's whole point is that it spans the ones after it, and
        // an edge that skips columns arcs over the top rather than hiding behind the boxes
        // between its ends. That long arc IS the agent's lifetime.
        Topology.Graph topo = graph("stages",
                List.of(node("in", "the stay", "input", 0),
                        node("vet", "VetCallback", "agent", 1).withSub("async · starts here"),
                        node("meals", "MealPlanner", "agent", 2),
                        node("walks", "WalkPlanner", "agent", 3),
                        node("join", "the note", "join", 4).withSub("reads vetline")),
                List.of(edge("in", "vet"),
                        edge("vet", "meals", "does not wait"),
                        edge("meals", "walks"),
                        edge("walks", "join"),
                        edge("vet", "join", "the read that joins")));
        return new PatternDef("async", "Asynchronous Agents", "production",
                "The vet's out-of-hours line takes a minute to answer. You do not stand there "
                        + "holding the phone while the rest of the note writes itself.",
                "Demo 4's MealPlanner and WalkPlanner, unchanged — only the vet step is new.",
                "One step in an ordinary sequence marked `async(true)`. The agent is unchanged, "
                        + "the builder is unchanged, and the declaration order is unchanged — the "
                        + "slow step is still asked first. What changes is that it writes an "
                        + "`AsyncResponse` into the scope instead of a value, so **the join is "
                        + "the line that reads the key**, not a step you declare. Watch the badge: "
                        + "the whole run is shorter than the agents were busy, in a sequence.",
                "The waiting moves, it does not disappear — and it moves somewhere less obvious. "
                        + "A failure inside an async agent surfaces at the **read**, which is "
                        + "usually a long way from the step that caused it, and the Scope tab "
                        + "shows `<pending>` until then. Only mark a step async when nothing "
                        + "between it and its reader needs its answer.",
                topo,
                "Friday to Sunday, my sister has him. Two scoops morning and evening, walks "
                        + "morning and evening, and I want the out-of-hours cover on the note.",
                AsyncPattern::run);
    }
}
