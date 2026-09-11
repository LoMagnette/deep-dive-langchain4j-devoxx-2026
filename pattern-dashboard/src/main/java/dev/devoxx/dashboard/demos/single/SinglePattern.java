package dev.devoxx.dashboard.demos.single;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static dev.devoxx.dashboard.support.Wiring.agent;
import static dev.devoxx.dashboard.support.Wiring.result;

import java.util.List;
import java.util.Map;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.PatternDef.Runner;
import dev.devoxx.dashboard.catalog.Topology;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;

/**
 * Wiring for the <b>single</b> demo — one call, one job.
 */
public final class SinglePattern {

    private SinglePattern() {
    }

    /**
     * The message every household sends the friend who is watching the dog. Shared with the
     * sequential demo, which runs the same input through a second agent — seeing the identical
     * text produce a different artefact is the point of putting them side by side.
     */
    public static final String SITTER_MESSAGE =
            "hey so thanks again for having zao!! he's the big black belgian shepherd, food's "
                    + "in the tub by the back door he has two scoops morning and evening, oh and "
                    + "he CANNOT have the dried liver treats anymore they upset him. don't let "
                    + "him off the lead in the park he won't come back yet. vet is 061 22 33 44 "
                    + "if anything happens. he'll cry the first night, ignore it, he's fine!!";

    public static PatternDef define() {
        Topology.Graph topo = graph("chain",
                List.of(node("in", "message", "input"),
                        node("clerk", "SitterCardClerk", "agent")),
                List.of(edge("in", "clerk")));
        Runner runner = (model, input, listener) -> {
            var clerk = agent(SitterCardClerk.class, model, "SitterCardClerk", "card");
            UntypedAgent app = AgenticServices.sequenceBuilder()
                    .subAgents(clerk).outputKey("card").listener(listener).build();
            var r = app.invokeWithAgenticScope(Map.of("message", input));
            return result(r, "card");
        };
        return new PatternDef("single", "Single Agent", "workflow",
                "One LLM call wrapped as an agent — the simplest useful unit, doing the job an "
                        + "LLM is genuinely best at: turning what a human actually typed into a "
                        + "shape a system can use.",
                "No decomposition: one agent struggles with multi-step or long tasks — and watch "
                        + "the Walks line, because a model would rather invent a walk time than "
                        + "admit the message never gave one.",
                topo,
                // A real message: no punctuation, out of order, and one field genuinely absent
                // (nobody said when to walk him), so the room can check whether the agent obeys
                // "write not given" or quietly makes something up.
                SITTER_MESSAGE,
                runner);
    }
}
