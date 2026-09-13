package dev.devoxx.dashboard.demos.loop;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static dev.devoxx.dashboard.support.Parsing.score;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import dev.devoxx.dashboard.demos.sequential.FridgeChecklist;
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
                // The same FridgeChecklist the sequential demo used, with a critic added and a
                // loop drawn round it. The lesson is that nothing about the agent changed.
                List.of(node("in", "notes", "input"),
                        node("writer", "FridgeChecklist", "agent"),
                        node("check", "FridgeRuleCheck", "agent")),
                List.of(edge("in", "writer"), edge("writer", "check", "notes"),
                        edge("check", "writer", "score < 0.8")));
        Runner runner = (model, input, listener) -> {
            // Demo 2's agent, unchanged. The only difference is what surrounds it: it now reads
            // its own previous answer, which is why its input key is 'notes' rather than 'card'.
            var writer = AgenticServices.agentBuilder(FridgeChecklist.class)
                    .chatModel(model)
                    .name("FridgeChecklist")
                    .outputKey("notes")
                    .build();
            var check = AgenticServices.agentBuilder(FridgeRuleCheck.class)
                    .chatModel(model)
                    .name("FridgeRuleCheck")
                    .outputKey("score")
                    .build();
            Predicate<AgenticScope> good = s -> score(s.readState("score", "")) >= 0.8;
            UntypedAgent app = AgenticServices.loopBuilder()
                    .subAgents(writer, check)
                    .maxIterations(5)
                    .exitCondition(good)
                    .testExitAtLoopEnd(true)
                    .outputKey("notes")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("notes", input));
            return String.valueOf(r.result());
        };
        return new PatternDef("loop", "Loop / Iterative Refinement", "workflow",
                // The beat this demo plays in the running narration.
                "This is the note you actually sent last time. You already know the four "
                        + "things wrong with it.",
                // What this demo inherits from the ones before it.
                "Demo 2's FridgeChecklist, unchanged. Nothing about the agent changed; a "
                        + "critic and a loop were drawn around it.",
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
