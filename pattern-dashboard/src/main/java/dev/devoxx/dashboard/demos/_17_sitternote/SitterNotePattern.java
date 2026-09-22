package dev.devoxx.dashboard.demos._17_sitternote;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static dev.devoxx.dashboard.support.Parsing.category;
import static dev.devoxx.dashboard.support.Parsing.score;

import java.util.List;
import java.util.Map;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._01_single.Keys.Notes;
import dev.devoxx.dashboard.demos._02_sequential.FridgeMagnet;
import dev.devoxx.dashboard.demos._03_loop.RuffDraftCritic;
import dev.devoxx.dashboard.demos._03_loop.Keys.Score;
import dev.devoxx.dashboard.demos._04_parallel.Keys.Meals;
import dev.devoxx.dashboard.demos._04_parallel.Keys.Stay;
import dev.devoxx.dashboard.demos._04_parallel.Keys.Walks;
import dev.devoxx.dashboard.demos._04_parallel.ChowHound;
import dev.devoxx.dashboard.demos._04_parallel.LeadDeveloper;
import dev.devoxx.dashboard.demos._06_conditional.DogTrainer;
import dev.devoxx.dashboard.demos._06_conditional.EmergencyVet;
import dev.devoxx.dashboard.demos._06_conditional.EverydayCare;
import dev.devoxx.dashboard.demos._06_conditional.Keys.Answer;
import dev.devoxx.dashboard.demos._06_conditional.Keys.Category;
import dev.devoxx.dashboard.demos._06_conditional.Keys.Worry;
import dev.devoxx.dashboard.demos._06_conditional.WorryRouter;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for the <b>sitter note (composite)</b> demo — the capstone: four patterns composed into the note on the fridge door.
 */
public final class SitterNotePattern {

    private SitterNotePattern() {
    }

    /** The wiring. Everything below it is the dashboard telling itself how to draw this. */
    static String run(ChatModel model, String input, StreamingListener listener) {
        // 1. Conditional routing — one LLM judgement decides who answers the worry.
        var router = AgenticServices.agentBuilder(WorryRouter.class)
                .chatModel(model)
                .name("WorryRouter")
                .outputKey(Category.class)
                .build();
        var vet = AgenticServices.agentBuilder(EmergencyVet.class)
                .chatModel(model)
                .name("EmergencyVet")
                .outputKey(Answer.class)
                .build();
        var trainer = AgenticServices.agentBuilder(DogTrainer.class)
                .chatModel(model)
                .name("DogTrainer")
                .outputKey(Answer.class)
                .build();
        var care = AgenticServices.agentBuilder(EverydayCare.class)
                .chatModel(model)
                .name("EverydayCare")
                .outputKey(Answer.class)
                .build();
        UntypedAgent triage = AgenticServices.conditionalBuilder()
                .subAgents(s -> category(s.readState(Category.class)).equals("emergency"), vet)
                .subAgents(s -> category(s.readState(Category.class)).equals("training"), trainer)
                .subAgents(s -> category(s.readState(Category.class)).equals("everyday"), care)
                .build();

        // 2. Parallel — meals and walks do not need each other, so fan them out.
        var meals = AgenticServices.agentBuilder(ChowHound.class)
                .chatModel(model)
                .name("ChowHound")
                .outputKey(Meals.class)
                .build();
        var walks = AgenticServices.agentBuilder(LeadDeveloper.class)
                .chatModel(model)
                .name("LeadDeveloper")
                .outputKey(Walks.class)
                .build();
        UntypedAgent plan = AgenticServices.parallelBuilder()
                .subAgents(meals, walks)
                .build();

        // 3. Loop — refine until the four fridge-door rules hold, never forever. This IS
        //    demo 3's loop, both agents unchanged, with the merged note fed in.
        var tighten = AgenticServices.agentBuilder(FridgeMagnet.class)
                .chatModel(model)
                .name("FridgeMagnet")
                .outputKey(Notes.class)
                .build();
        var check = AgenticServices.agentBuilder(RuffDraftCritic.class)
                .chatModel(model)
                .name("RuffDraftCritic")
                .outputKey(Score.class)
                .build();
        UntypedAgent refine = AgenticServices.loopBuilder()
                .subAgents(tighten, check)
                .maxIterations(3)
                .exitCondition(s -> score(s.readState(Score.class)) >= 0.8)
                .testExitAtLoopEnd(true)
                .build();

        // 4. Sequence — the spine that holds the three composites plus the merge step.
        var merge = AgenticServices.agentBuilder(SitterNoteMerger.class)
                .chatModel(model)
                .name("SitterNoteMerger")
                .outputKey(Notes.class)
                .build();
        UntypedAgent app = AgenticServices.sequenceBuilder()
                .subAgents(router, triage, plan, merge, refine)
                .outputKey(Notes.class)
                .listener(listener)
                .build();
        // The same text under two keys: the router and specialists ask "what is the worry",
        // the planners ask "what is the stay". Reusing an agent means accepting the key it
        // already declared — get it wrong and MissingArgumentException blames another step.
        var r = app.invokeWithAgenticScope(
                Map.of(new Worry().name(), input, new Stay().name(), input));
        return String.valueOf(r.result());
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        Topology.Graph topo = graph("stages",
                List.of(node("in", "the weekend", "input", 0),
                        node("router", "WorryRouter", "router", 1),
                        node("vet", "EmergencyVet", "agent", 2),
                        node("trainer", "DogTrainer", "agent", 2),
                        node("care", "EverydayCare", "agent", 2),
                        node("meals", "ChowHound", "agent", 2),
                        node("walks", "LeadDeveloper", "agent", 2),
                        node("merge", "SitterNoteMerger", "join", 3),
                        node("tighten", "FridgeMagnet", "agent", 4),
                        node("check", "RuffDraftCritic", "agent", 4)),
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

        return new PatternDef("sitterNote", "Sitter Note (composite)", "composite",
                "Back to that weekend away, the whole thing end to end. Seventeen demos later, "
                        + "somebody finally writes down when the dog goes out.",
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
                "we're away Friday to Sunday and my sister is having Zao. Two scoops morning "
                        + "and evening, he pulls like a train on the lead, and it's New Year, so "
                        + "there will be fireworks both nights and he will be under the table.",
                SitterNotePattern::run);
    }
}
