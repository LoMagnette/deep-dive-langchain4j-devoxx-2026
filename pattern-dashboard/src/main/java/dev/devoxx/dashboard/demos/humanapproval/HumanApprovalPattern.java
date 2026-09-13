package dev.devoxx.dashboard.demos.humanapproval;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;

import java.util.List;
import java.util.Map;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.PatternDef.Runner;
import dev.devoxx.dashboard.catalog.Topology;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;

/** Wiring for the <b>human in the loop</b> demo — a step in the middle that is a person. */
public final class HumanApprovalPattern {

    private HumanApprovalPattern() {
    }

    public static PatternDef define() {
        Topology.Graph topo = graph("chain",
                // The middle node's role is "human", not "agent", and that is the whole diagram:
                // drawn as another agent box it would say the model decided, which is the one
                // thing this pattern exists to deny.
                List.of(node("in", "tonight", "input"),
                        node("drafter", "DoseDrafter", "agent"),
                        node("owner", "You", "human"),
                        node("final", "FinalNote", "agent")),
                List.of(edge("in", "drafter"),
                        edge("drafter", "owner", "draft"),
                        edge("owner", "final", "decision")));

        Runner runner = (model, input, listener) -> {
            var drafter = AgenticServices.agentBuilder(DoseDrafter.class)
                    .chatModel(model)
                    .name("DoseDrafter")
                    .outputKey("draft")
                    .build();

            // A HumanInTheLoop is a non-AI agent: its "implementation" is a person. From the
            // sequence's point of view it is just another sub-agent that happens to be slow —
            // it reads a key from the scope and writes one back, like everything else.
            var owner = AgenticServices.humanInTheLoopBuilder()
                    .description("The owner, who decides what actually goes in the dog")
                    .inputKey(String.class, "draft")
                    .outputKey("decision")
                    .responseProvider(scope -> listener.askHuman("You", """
                            The vet is closed and this is what the assistant suggests. \
                            Approve it, change it, or refuse it — nothing is given until you say.

                            """ + scope.readState("draft", "")))
                    .build();

            var last = AgenticServices.agentBuilder(FinalNote.class)
                    .chatModel(model)
                    .name("FinalNote")
                    .outputKey("instruction")
                    .build();

            UntypedAgent app = AgenticServices.sequenceBuilder()
                    .subAgents(drafter, owner, last)
                    .outputKey("instruction")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("situation", input));

            // Show what was drafted and what the person said, not just the outcome: the whole
            // point of the pattern is the gap between those two, and a result that showed only
            // the final line would hide the only interesting thing that happened.
            var scope = r.agenticScope();
            if (scope == null) {
                return String.valueOf(r.result());
            }
            return "**The assistant drafted**\n\n" + scope.readState("draft", "")
                    + "\n\n**You said**\n\n" + scope.readState("decision", "")
                    + "\n\n**So the instruction is**\n\n" + scope.readState("instruction", "");
        };

        return new PatternDef("humanApproval", "Human in the Loop", "workflow",
                // The beat this demo plays in the running narration.
                "Another night, the vet already closed, and the human medicine cupboard "
                        + "open in front of you.",
                "A step in the middle that is a person, not an agent. The model is good at "
                        + "drafting what to give and how much; deciding whether it actually goes "
                        + "in the dog is not its call. `HumanInTheLoop` is a non-AI agent — it "
                        + "reads a key from the scope and writes one back, so the sequence around "
                        + "it does not know or care that the answer came from a browser.",
                "The brake on the dial, and it costs what brakes cost: the run blocks on a "
                        + "person, so it needs a timeout and a thread you can afford to park. Ask "
                        + "too often and it is a form nobody fills in; ask too rarely and the "
                        + "approval is a rubber stamp. Put it where the action is hard to undo.",
                topo,
                // Everyone knows you do not dose a dog out of the human medicine cupboard, so
                // everyone can judge both the draft and their own answer to it.
                "Zao is limping and clearly sore, the vet is closed until morning, and there is "
                        + "half a packet of our own ibuprofen and some leftover dog painkillers "
                        + "from his last check-up in the cupboard",
                runner);
    }
}
