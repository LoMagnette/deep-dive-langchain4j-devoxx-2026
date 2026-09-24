package dev.devoxx.dashboard.demos._09_supervisor;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;

import java.util.List;
import java.util.Locale;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._06_conditional.DogTrainer;
import dev.devoxx.dashboard.demos._06_conditional.EmergencyVet;
import dev.devoxx.dashboard.demos._06_conditional.EverydayCare;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.scope.AgentInvocation;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorContextStrategy;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for the <b>supervisor</b> demo — the same three people the router chose between, except
 * that now nobody wrote down who to ask.
 */
public final class SupervisorPattern {

    private SupervisorPattern() {
    }

    /** Everyone this supervisor may call: the nurse it adds, then the routing demo's three. */
    private static final List<String> DESKS =
            List.of("TriageNurse", "EverydayCare", "DogTrainer", "EmergencyVet");

    /** The wiring. Everything below it is the dashboard telling itself how to draw this. */
    static String run(ChatModel model, String input, StreamingListener listener) {
        var nurse = AgenticServices.agentBuilder(TriageNurse.class)
                .chatModel(model)
                .name("TriageNurse")
                .build();

        var care = AgenticServices.agentBuilder(EverydayCare.class)
                .chatModel(model)
                .name("EverydayCare")
                .build();

        var trainer = AgenticServices.agentBuilder(DogTrainer.class)
                .chatModel(model)
                .name("DogTrainer")
                .build();

        var vet = AgenticServices.agentBuilder(EmergencyVet.class)
                .chatModel(model)
                .name("EmergencyVet")
                .build();

        SupervisorAgent sup = AgenticServices.supervisorBuilder()
                .subAgents(nurse, care, trainer, vet)
                .chatModel(model)
                .supervisorContext("""
                        Always call the nurse first: she takes the call, works out what is \
                        going on, and ends by naming who it needs. She never treats and \
                        never trains, so her answer is NEVER the answer to give back — it \
                        tells you who to call next. When she says NEEDS: vet, call the vet \
                        with the original worry; NEEDS: trainer, call the trainer; NEEDS: \
                        everyday care, call everyday care. Only when she says NEEDS: nobody \
                        is her own answer enough. You are finished once the specialist she \
                        named has answered.""")
                .contextGenerationStrategy(SupervisorContextStrategy.CHAT_MEMORY)
                .maxAgentsInvocations(4)
                .output(SupervisorPattern::answerWithItsRoute)
                .listener(listener)
                .build();
        var r = sup.invokeWithAgenticScope(input);
        return String.valueOf(r.result());
    }

    // ---- how the result is presented; the wiring above is the demo ----

    /**
     * Who was actually called, and what each of them said.
     */
    private static String answerWithItsRoute(AgenticScope scope) {
        var calls = scope.agentInvocations().stream()
                .filter(i -> DESKS.contains(i.agentName()))
                .toList();
        if (calls.isEmpty()) {
            return "The supervisor called nobody.";
        }

        var settled = calls.get(calls.size() - 1);
        String answer = strip(settled.output());
        if (calls.size() == 1) {
            return "**" + settled.agentName() + " answered it.**\n\n" + answer;
        }

        String path = calls.stream().map(AgentInvocation::agentName)
                .collect(java.util.stream.Collectors.joining(" → "));
        StringBuilder out = new StringBuilder("**" + path + "**\n\n" + answer + "\n\n---\n");


        for (int i = 0; i < calls.size() - 1; i++) {
            out.append("\n*").append(calls.get(i).agentName()).append(" did not answer it — \"")
                    .append(firstSentence(strip(calls.get(i).output())))
                    .append("\" — and named ").append(calls.get(i + 1).agentName())
                    .append(", so that is who the supervisor called.*\n");
        }
        return out.toString();
    }

    /**
     * The answer without the protocol on the end of it. Both markers have to go: the desks sign
     * off with ANSWERED or ESCALATE, and the nurse ends by naming who is needed — words the
     * planner acts on and a reader should never have to see.
     */
    private static String strip(Object output) {
        return String.valueOf(output)
                .replaceAll("(?is)\\s*NEEDS:\\s*\\w[\\w ]*$", "")
                .replaceAll("(?is)\\s*(ANSWERED|ESCALATE)\\s*$", "")
                .strip();
    }

    /** Enough of a declined answer to see why it was declined, and no more. */
    private static String firstSentence(String text) {
        int stop = text.indexOf(". ");
        return stop < 0 || stop > 160 ? text.substring(0, Math.min(160, text.length())).strip()
                : text.substring(0, stop).strip();
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // Columns, not a star: a wheel of equal spokes is a picture of the fan-out this demo
        // exists to deny. The worry arrives at the supervisor, never at an agent.
        Topology.Graph topo = graph("stages",
                List.of(node("in", "worry", "input", 0),
                        node("supervisor", "Supervisor", "supervisor", 1)
                                .withSub("asks, reads, asks again"),
                        node("nurse", "TriageNurse", "agent", 2).withSub("1 · always first"),
                        node("care", "EverydayCare", "agent", 2).withSub("2 · if she says so"),
                        node("trainer", "DogTrainer", "agent", 2).withSub("2 · if she says so"),
                        node("vet", "EmergencyVet", "agent", 2).withSub("2 · if she says so")),
                // Two-way on the nurse only — the supervisor reads her answer, and that is the
                // edge the demo turns on. Only the return half is labelled: both halves bow
                // through the same gap, and the answer is the one carrying the mechanism.
                List.of(edge("in", "supervisor"),
                        edge("supervisor", "nurse"),
                        edge("nurse", "supervisor", "names who it needs"),
                        edge("supervisor", "care"),
                        edge("supervisor", "trainer", "then one of these"),
                        edge("supervisor", "vet")));

        return new PatternDef("supervisor", "Supervisor", "pure-agent",
                "Then something that is not like him at all. You cannot tell if it is "
                        + "behaviour or something worse, and neither can one phone call.",
                "Demo 6's three desks again, unchanged, plus one new agent: the nurse who "
                        + "takes the call. Routing picks one desk; this picks several and "
                        + "decides when to stop.",
                "An LLM supervisor decides which specialist to invoke, and when to stop. Watch "
                        + "the order: the **TriageNurse** takes the call, works out what is going "
                        + "on and ends by naming who is needed — and **that answer is what makes "
                        + "it call the vet.** A router gets one call and stops. A fan-out would "
                        + "have asked all three desks at once and learned nothing from any of "
                        + "them. Neither can produce a second call that exists only because of "
                        + "what the first one said. Change the input and the route changes with "
                        + "it: pulling and barking reach the trainer, grass-eating settles with "
                        + "the nurse and stops there. Note what the result shows: **one answer**, "
                        + "with the route to it underneath. The nurse did not give an opinion "
                        + "worth keeping — she assessed, and assessing is work, not output.",
                "Non-deterministic, and the roll-call is honest about it: a weaker planner will "
                        + "sometimes take the nurse's assessment as the answer and stop. Bound "
                        + "the invocations. Note also what it took to make the hand-off reliable "
                        + "— an agent whose job **is** to hand on, rather than one that declines; "
                        + "a model asked to refuse under a positive instruction will follow the "
                        + "positive one. And ask the hard question first: if you can write down "
                        + "\"nurse, then whoever she names\", that is a sequence with a "
                        + "condition, and it is cheaper and debuggable. Reach for this when you "
                        + "genuinely cannot enumerate who is needed.",
                topo,
                // Reads as a training problem, and is not one — which nobody can know until the
                // trainer has looked at it. That is the point: the second call is not in anyone's
                // plan at the start, it is caused by the first agent's answer.
                "he is four and he has started snapping when the children go near his bed. He has "
                        + "never done that in his life. Nothing here has changed except him.",
                SupervisorPattern::run);
    }
}
