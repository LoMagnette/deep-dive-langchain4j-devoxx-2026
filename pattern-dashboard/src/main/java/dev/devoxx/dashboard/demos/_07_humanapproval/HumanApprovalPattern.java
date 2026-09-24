package dev.devoxx.dashboard.demos._07_humanapproval;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static dev.devoxx.dashboard.support.Parsing.category;
import static java.util.Objects.requireNonNullElse;

import java.util.List;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._06_conditional.DogTrainer;
import dev.devoxx.dashboard.demos._06_conditional.EmergencyVet;
import dev.devoxx.dashboard.demos._06_conditional.EverydayCare;
import dev.devoxx.dashboard.demos._06_conditional.Keys.Category;
import dev.devoxx.dashboard.demos._06_conditional.WorryRouter;
import dev.devoxx.dashboard.demos._07_humanapproval.Keys.Decision;
import dev.devoxx.dashboard.demos._07_humanapproval.Keys.Draft;
import dev.devoxx.dashboard.demos._07_humanapproval.Keys.Instruction;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for the <b>human in the loop</b> demo — the previous demo, with a person added.
 */
public final class HumanApprovalPattern {

    private HumanApprovalPattern() {
    }

    /** The wiring. Everything below it is the dashboard telling itself how to draw this. */
    static String run(ChatModel model, String input, StreamingListener listener) {
        // 1. Demo 6, unchanged: route the worry to whoever can answer it.
        var router = AgenticServices.agentBuilder(WorryRouter.class)
                .chatModel(model)
                .name("WorryRouter")
                .outputKey(Category.class)
                .build();
        var care = AgenticServices.agentBuilder(EverydayCare.class)
                .chatModel(model)
                .name("EverydayCare")
                .outputKey(Draft.class)
                .build();
        var trainer = AgenticServices.agentBuilder(DogTrainer.class)
                .chatModel(model)
                .name("DogTrainer")
                .outputKey(Draft.class)
                .build();
        var vet = AgenticServices.agentBuilder(EmergencyVet.class)
                .chatModel(model)
                .name("EmergencyVet")
                .outputKey(Draft.class)
                .build();
        TriageDesk triage = AgenticServices.conditionalBuilder(TriageDesk.class)
                .name("Conditional")
                .subAgents(s -> category(s.readState(Category.class)).equals("everyday"), care)
                .subAgents(s -> category(s.readState(Category.class)).equals("training"), trainer)
                .subAgents(s -> category(s.readState(Category.class)).equals("emergency"), vet)
                .build();


        var owner = AgenticServices.humanInTheLoopBuilder()
                .description("The owner, who decides what the sitter is actually told to do")
                .inputKey(String.class, new Draft().name())
                .outputKey(new Decision().name())
                .responseProvider(scope -> listener.askHuman("You", """
                        This is what the desk says, and your sitter is waiting on it. \
                        Approve it, change it, or refuse it — nothing is passed on until \
                        you say.

                        """ + requireNonNullElse(scope.readState(Draft.class), "")))
                .build();

        var last = AgenticServices.agentBuilder(FinalNote.class)
                .chatModel(model)
                .name("FinalNote")
                .outputKey(Instruction.class)
                .build();

        ApprovalPipeline app = AgenticServices.sequenceBuilder(ApprovalPipeline.class)
                .name("Sequential")
                .subAgents(router, triage, owner, last)
                .outputKey(Instruction.class)
                .listener(listener)
                .build();
        var r = app.instruct(input);

        // Show what was drafted and what the person said, not only the outcome: the whole
        // point of the pattern is the gap between those two.
        AgenticScope scope = r.agenticScope();
        if (scope == null) {
            return String.valueOf(r.result());
        }
        String draft = requireNonNullElse(scope.readState(Draft.class), "")
                .replaceAll("(?is)\\s*(ANSWERED|ESCALATE)\\s*$", "");
        String decisionText = requireNonNullElse(scope.readState(Decision.class), "");
        String instructionText = requireNonNullElse(scope.readState(Instruction.class), "");
        return "**The desk drafted**\n\n" + draft
                + "\n\n**You said**\n\n" + decisionText
                + "\n\n**So the sitter is told**\n\n" + instructionText;
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        Topology.Graph topo = graph("stages",
                // The person's node has role "human", not "agent", and that is the whole diagram:
                // drawn as another agent box it would say the model decided, which is the one
                // thing this pattern exists to deny.
                List.of(node("in", "worry", "input", 0),
                        node("router", "WorryRouter", "router", 1),
                        node("care", "EverydayCare", "agent", 2),
                        node("trainer", "DogTrainer", "agent", 2),
                        node("vet", "EmergencyVet", "agent", 2),
                        node("owner", "You", "human", 3),
                        node("final", "FinalNote", "agent", 4)),
                List.of(edge("in", "router"),
                        edge("router", "care", "everyday"),
                        edge("router", "trainer", "training"),
                        edge("router", "vet", "emergency"),
                        edge("care", "owner"), edge("trainer", "owner"),
                        edge("vet", "owner", "draft"),
                        edge("owner", "final", "decision")));

        return new PatternDef("humanApproval", "Human in the Loop", "workflow",
                "Same three desks, a different disaster. Except the person acting on the "
                        + "answer is your sister, and she will do exactly what it says.",
                "Demo 6 exactly — same router, same three desks — with one person added "
                        + "before anything reaches the sitter.",
                "The previous demo with a person added, and nothing else changed: same router, "
                        + "same three desks, one more step before anything reaches the sitter. "
                        + "`HumanInTheLoop` is a non-AI agent — it reads a key from the scope and "
                        + "writes one back, so the sequence around it cannot tell that the answer "
                        + "came from a browser.",
                "The brake on the dial, and it costs what brakes cost: the run blocks on a "
                        + "person, so it needs a timeout and a thread you can afford to park. Ask "
                        + "too often and it is a form nobody fills in; ask too rarely and the "
                        + "approval is a rubber stamp. Put it where the action is hard to undo.",
                topo,
                "he has swallowed a sock. A whole sock. We have counted them and there are "
                        + "eleven. My sister is the one standing there, not me",
                HumanApprovalPattern::run);
    }
}
