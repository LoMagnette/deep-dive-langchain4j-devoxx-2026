package dev.devoxx.dashboard.demos.loop;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static dev.devoxx.dashboard.support.Parsing.score;
import static dev.devoxx.dashboard.support.Wiring.agent;
import static dev.devoxx.dashboard.support.Wiring.result;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.PatternDef.Runner;
import dev.devoxx.dashboard.catalog.Topology;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.scope.AgenticScope;

/**
 * Wiring for the <b>loop</b> demo — refine until four rules the room agrees with are satisfied.
 */
public final class LoopPattern {

    private LoopPattern() {
    }

    public static PatternDef define() {
        Topology.Graph topo = graph("loop",
                List.of(node("in", "note", "input"),
                        node("writer", "SitterNoteWriter", "agent"),
                        node("check", "FridgeRuleCheck", "agent")),
                List.of(edge("in", "writer"), edge("writer", "check", "note"),
                        edge("check", "writer", "score < 0.8")));
        Runner runner = (model, input, listener) -> {
            var writer = agent(SitterNoteWriter.class, model, "SitterNoteWriter", "note");
            var check = agent(FridgeRuleCheck.class, model, "FridgeRuleCheck", "score");
            Predicate<AgenticScope> good = s -> score(s) >= 0.8;
            UntypedAgent app = AgenticServices.loopBuilder()
                    .subAgents(writer, check)
                    .maxIterations(5)
                    .exitCondition(good)
                    .testExitAtLoopEnd(true)
                    .outputKey("note")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("note", input));
            return result(r, "note");
        };
        return new PatternDef("loop", "Loop / Iterative Refinement", "workflow",
                "Refine until a quality bar is met. The bar is four rules nobody has to be "
                        + "persuaded of — every meal with a time and an amount, where the lead "
                        + "is, the vet's number, short enough for the fridge door — so the score "
                        + "is a fraction of rules satisfied, and you can see which one each pass "
                        + "fixes.",
                "Can spin forever or oscillate — always cap iterations and define a clear exit. A "
                        + "critic scoring 'quality' out of 1.0 gives you a number nobody in the "
                        + "room can check; score against named rules instead.",
                topo,
                // Fails three of the four rules on sight, which is the point: the audience can
                // count the failures before the first agent runs.
                "just feed him twice like normal and take him out when you can, he knows the "
                        + "routine. ring me if anything's up!",
                runner);
    }
}
