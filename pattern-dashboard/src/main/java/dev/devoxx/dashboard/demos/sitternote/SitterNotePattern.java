package dev.devoxx.dashboard.demos.sitternote;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static dev.devoxx.dashboard.support.Parsing.category;
import static dev.devoxx.dashboard.support.Parsing.score;

import java.util.List;
import java.util.Map;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.demos.parallel.MealPlanner;
import dev.devoxx.dashboard.demos.sequential.FridgeChecklist;
import dev.devoxx.dashboard.demos.parallel.WalkPlanner;
import dev.devoxx.dashboard.catalog.PatternDef.Runner;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos.conditional.DogTrainer;
import dev.devoxx.dashboard.demos.conditional.EmergencyVet;
import dev.devoxx.dashboard.demos.conditional.EverydayCare;
import dev.devoxx.dashboard.demos.conditional.WorryRouter;
import dev.devoxx.dashboard.demos.loop.FridgeRuleCheck;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;

/**
 * Wiring for the <b>sitter note (composite)</b> demo — the capstone: four patterns composed into the note on the fridge door.
 */
public final class SitterNotePattern {

    private SitterNotePattern() {
    }

    public static PatternDef define() {
        Topology.Graph topo = graph("stages",
                List.of(node("in", "the weekend", "input", 0),
                        node("router", "WorryRouter", "router", 1),
                        node("vet", "EmergencyVet", "agent", 2),
                        node("trainer", "DogTrainer", "agent", 2),
                        node("care", "EverydayCare", "agent", 2),
                        node("meals", "MealPlanner", "agent", 2),
                        node("walks", "WalkPlanner", "agent", 2),
                        node("merge", "SitterNoteMerger", "join", 3),
                        node("tighten", "FridgeChecklist", "agent", 4),
                        node("check", "FridgeRuleCheck", "agent", 4)),
                List.of(edge("in", "router"),
                        edge("router", "vet", "emergency"),
                        edge("router", "trainer", "training"),
                        edge("router", "care", "everyday"),
                        edge("in", "meals", "in parallel"),
                        edge("in", "walks"),
                        edge("vet", "merge"), edge("trainer", "merge"),
                        edge("care", "merge", "answer"),
                        edge("meals", "merge"), edge("walks", "merge"),
                        edge("merge", "tighten", "notes"),
                        edge("tighten", "check"),
                        edge("check", "tighten", "score < 0.8")));

        Runner runner = (model, input, listener) -> {
            // 1. Conditional routing — one LLM judgement decides who answers the worry.
            var router = AgenticServices.agentBuilder(WorryRouter.class)
                    .chatModel(model)
                    .name("WorryRouter")
                    .outputKey("category")
                    .build();
            var vet = AgenticServices.agentBuilder(EmergencyVet.class)
                    .chatModel(model)
                    .name("EmergencyVet")
                    .outputKey("answer")
                    .build();
            var trainer = AgenticServices.agentBuilder(DogTrainer.class)
                    .chatModel(model)
                    .name("DogTrainer")
                    .outputKey("answer")
                    .build();
            var care = AgenticServices.agentBuilder(EverydayCare.class)
                    .chatModel(model)
                    .name("EverydayCare")
                    .outputKey("answer")
                    .build();
            UntypedAgent triage = AgenticServices.conditionalBuilder()
                    .subAgents(s -> category(s.readState("category", "")).equals("emergency"), vet)
                    .subAgents(s -> category(s.readState("category", "")).equals("training"), trainer)
                    .subAgents(s -> category(s.readState("category", "")).equals("everyday"), care)
                    .build();

            // 2. Parallel — meals and walks do not need each other, so fan them out.
            var meals = AgenticServices.agentBuilder(MealPlanner.class)
                    .chatModel(model)
                    .name("MealPlanner")
                    .outputKey("meals")
                    .build();
            var walks = AgenticServices.agentBuilder(WalkPlanner.class)
                    .chatModel(model)
                    .name("WalkPlanner")
                    .outputKey("walks")
                    .build();
            UntypedAgent plan = AgenticServices.parallelBuilder()
                    .subAgents(meals, walks)
                    .build();

            // 3. Loop — refine the note until the four fridge-door rules hold, never forever.
            //    This IS demo 3's loop, both agents unchanged: the same checklist writer and
            //    the same critic, with the merged note fed in instead of a typed one. A
            //    composite reuses the parts rather than re-implementing them.
            var tighten = AgenticServices.agentBuilder(FridgeChecklist.class)
                    .chatModel(model)
                    .name("FridgeChecklist")
                    .outputKey("notes")
                    .build();
            var check = AgenticServices.agentBuilder(FridgeRuleCheck.class)
                    .chatModel(model)
                    .name("FridgeRuleCheck")
                    .outputKey("score")
                    .build();
            UntypedAgent refine = AgenticServices.loopBuilder()
                    .subAgents(tighten, check)
                    .maxIterations(3)
                    .exitCondition(s -> score(s.readState("score", "")) >= 0.8)
                    .testExitAtLoopEnd(true)
                    .build();

            // 4. Sequence — the spine that holds the three composites plus the merge step.
            var merge = AgenticServices.agentBuilder(SitterNoteMerger.class)
                    .chatModel(model)
                    .name("SitterNoteMerger")
                    .outputKey("notes")
                    .build();
            UntypedAgent app = AgenticServices.sequenceBuilder()
                    .subAgents(router, triage, plan, merge, refine)
                    .outputKey("notes")
                    .listener(listener)
                    .build();
            // The same text under two keys, and not by accident: the router and the three
            // specialists ask "what is the worry", the two planners ask "what is the stay".
            // Reusing an agent means accepting the key IT already declared — this one line is
            // the seam the caveat is about, and getting it wrong is a MissingArgumentException
            // pointing at a step that looks unrelated.
            var r = app.invokeWithAgenticScope(Map.of("worry", input, "stay", input));
            return String.valueOf(r.result());
        };

        return new PatternDef("sitterNote", "Sitter Note (composite)", "composite",
                // The beat this demo plays in the running narration.
                "Back to that weekend away — this time the whole thing, end to end.",
                // What this demo inherits from the ones before it.
                "Almost everything: demo 6's router and desks, demo 4's meal and walk "
                        + "planners, and demo 3's checklist and critic in the refining loop.",
                "A real system, not a pattern: the owner's worry is routed to the right person, a "
                        + "parallel step plans the meals and the walks, a sequence merges all "
                        + "three into one note for the fridge door, and a loop tightens it until "
                        + "it passes the same four rules as the loop demo. Deterministic "
                        + "scaffolding with LLM judgement at exactly three points.",
                // caveat: the interesting failures in composites are at the seams, not inside them.
                "Composites fail at the seams: every step depends on a key an earlier one wrote, "
                        + "so one agent answering off-format breaks a step that looks unrelated.",
                topo,
                "we're away Friday to Sunday and my sister is having Zao. He's on two scoops "
                        + "morning and evening, he pulls like a train on the lead, and it's New "
                        + "Year so there will be fireworks both nights.",
                runner);
    }
}
