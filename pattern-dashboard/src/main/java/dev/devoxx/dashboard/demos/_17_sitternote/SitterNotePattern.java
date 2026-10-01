package dev.devoxx.dashboard.demos._17_sitternote;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static dev.devoxx.dashboard.support.Parsing.category;
import static dev.devoxx.dashboard.support.Parsing.score;

import java.util.List;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._01_single.Keys.Notes;
import dev.devoxx.dashboard.demos._02_sequential.BattlePlanner;
import dev.devoxx.dashboard.demos._03_loop.RuffDraftCritic;
import dev.devoxx.dashboard.demos._03_loop.Keys.Score;
import dev.devoxx.dashboard.demos._04_parallel.Keys.Bait;
import dev.devoxx.dashboard.demos._04_parallel.Keys.Lookout;
import dev.devoxx.dashboard.demos._04_parallel.ChowHound;
import dev.devoxx.dashboard.demos._04_parallel.LeadDeveloper;
import dev.devoxx.dashboard.demos._06_conditional.DogTrainer;
import dev.devoxx.dashboard.demos._06_conditional.RescueDog;
import dev.devoxx.dashboard.demos._06_conditional.EverydayCare;
import dev.devoxx.dashboard.demos._06_conditional.Keys.Answer;
import dev.devoxx.dashboard.demos._06_conditional.Keys.Category;
import dev.devoxx.dashboard.demos._06_conditional.WorryRouter;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for the <b>Operation Squirrel (composite)</b> demo — the capstone: four patterns composed into the note on the fridge door.
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
        var rescue = AgenticServices.agentBuilder(RescueDog.class)
                .chatModel(model)
                .name("RescueDog")
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
        TriageDesk triage = AgenticServices.conditionalBuilder(TriageDesk.class)
                .name("Conditional")
                .subAgents(s -> category(s.readState(Category.class)).equals("emergency"), rescue)
                .subAgents(s -> category(s.readState(Category.class)).equals("training"), trainer)
                .subAgents(s -> category(s.readState(Category.class)).equals("everyday"), care)
                .build();

        // 2. Parallel — the bait and the chase do not need each other, so fan them out.
        var bait = AgenticServices.agentBuilder(ChowHound.class)
                .chatModel(model)
                .name("ChowHound")
                .outputKey(Bait.class)
                .build();
        var chase = AgenticServices.agentBuilder(LeadDeveloper.class)
                .chatModel(model)
                .name("LeadDeveloper")
                .outputKey(Lookout.class)
                .build();
        StayPlan plan = AgenticServices.parallelBuilder(StayPlan.class)
                .name("Parallel")
                .subAgents(bait, chase)
                .build();

        // 3. Loop — refine until the four battle-plan rules hold, never forever. This IS
        //    demo 3's loop, both agents unchanged, with the merged order fed in.
        var tighten = AgenticServices.agentBuilder(BattlePlanner.class)
                .chatModel(model)
                .name("BattlePlanner")
                .outputKey(Notes.class)
                .build();
        var check = AgenticServices.agentBuilder(RuffDraftCritic.class)
                .chatModel(model)
                .name("RuffDraftCritic")
                .outputKey(Score.class)
                .build();
        NoteRefinement refine = AgenticServices.loopBuilder(NoteRefinement.class)
                .name("Loop")
                .subAgents(tighten, check)
                .maxIterations(3)
                .exitCondition(s -> score(s.readState(Score.class)) >= 0.8)
                .testExitAtLoopEnd(true)
                .build();

        // 4. Sequence — the spine that holds the three composites plus the merge step.
        var merge = AgenticServices.agentBuilder(PackNoteMerger.class)
                .chatModel(model)
                .name("PackNoteMerger")
                .outputKey(Notes.class)
                .build();
        SitterNotePipeline app = AgenticServices.sequenceBuilder(SitterNotePipeline.class)
                .name("Sequential")
                .subAgents(router, triage, plan, merge, refine)
                .outputKey(Notes.class)
                .listener(listener)
                .build();
        // The same text under two keys: the router and specialists ask "what is the worry",
        // the planners ask "what is the mission". Reusing an agent means accepting the key it
        // already declared — get it wrong and MissingArgumentException blames another step.
        return app.write(input, input);
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        Topology.Graph topo = graph("stages",
                List.of(node("in", "the operation", "input", 0),
                        node("router", "WorryRouter", "router", 1).withSub("Corgi · herds worries"),
                        node("vet", "RescueDog", "agent", 2).withSub("St Bernard · rescue"),
                        node("trainer", "DogTrainer", "agent", 2).withSub("Border Collie · trains"),
                        node("care", "EverydayCare", "agent", 2).withSub("Golden · the everyday"),
                        node("meals", "ChowHound", "agent", 2).withSub("Labrador · the bait"),
                        node("walks", "LeadDeveloper", "agent", 2).withSub("Greyhound · the chase"),
                        node("merge", "PackNoteMerger", "join", 3).withSub("Zao · one note"),
                        node("tighten", "BattlePlanner", "agent", 4).withSub("Collie · rewrites"),
                        node("check", "RuffDraftCritic", "agent", 4)
                                .withSub("Poodle · 4 rules, scored")),
                List.of(edge("in", "router"),
                        edge("router", "vet", "emergency"),
                        edge("router", "trainer", "training"),
                        edge("router", "care", "everyday"),
                        edge("in", "meals", "in parallel"),
                        edge("in", "walks"),
                        edge("vet", "merge"), edge("trainer", "merge"),
                        edge("care", "merge", "answer"),
                        edge("meals", "merge"), edge("walks", "merge"),
                        edge("merge", "tighten", "the order"),
                        edge("tighten", "check"),
                        edge("check", "tighten", "score < 0.8")));

        return new PatternDef("sitterNote", "Operation Squirrel (composite)", "composite",
                "Operation Squirrel, end to end. Seventeen demos later, somebody finally writes "
                        + "down where everyone stands.",
                "Almost everything: demo 6's router and desks, demo 4's bait and chase "
                        + "planners, and demo 3's battle plan and critic in the refining loop.",
                "A real system, not a pattern: the pack's worry is routed to the right dog, a "
                        + "parallel step plans the bait and the chase, a sequence merges all "
                        + "three into one operation order, and a loop tightens it until it "
                        + "passes the same four rules as the loop demo. Deterministic "
                        + "scaffolding with LLM judgement at exactly three points.",
                // caveat: the interesting failures in composites are at the seams, not inside them.
                "Composites fail at the seams: every step depends on a key an earlier one wrote, "
                        + "so one agent answering off-format breaks a step that looks unrelated.",
                topo,
                "Operation Squirrel, Saturday at dawn. It comes down the big oak by the back "
                        + "fence and runs along the top of the fence to the bird feeder. And the "
                        + "Dachshund says he is digging under the fence again, whatever anyone says.",
                SitterNotePattern::run);
    }
}
