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
 *
 * <p>This is the pivot of the talk and it is deliberately built from parts the room has already
 * met: the three desks are demo 6's, unchanged. Demo 6 routes to <b>one</b> of them, chosen by a
 * classifier you wrote. Here the model calls as many as it thinks it needs and decides when it is
 * done — which is the only thing routing cannot do, and exactly what this input requires.
 *
 * <p>The one agent this demo adds is {@link TriageNurse}, and she is the reason the hand-off is
 * reliable rather than lucky. An earlier version had the <i>trainer</i> decline cases that smelled
 * of pain: it reads beautifully and on a live model it called one agent and stopped, because a
 * refusal is a conditional exception sitting under a positive instruction ("give the owner one
 * thing to change this week") and a small model takes the positive instruction every time. The
 * nurse never treats and never trains; her whole job is to assess and name who is needed, so she
 * always succeeds at what she was asked and the supervisor's next decision rests on a fact it was
 * given rather than a judgement the model had to volunteer.
 */
public final class SupervisorPattern {

    private SupervisorPattern() {
    }

    /** Everyone this supervisor may call: the nurse it adds, then the routing demo's three. */
    private static final List<String> DESKS =
            List.of("TriageNurse", "EverydayCare", "DogTrainer", "EmergencyVet");

    /** The wiring. Everything below it is the dashboard telling itself how to draw this. */
    static String run(ChatModel model, String input, StreamingListener listener) {
        // The one new agent, and the first one called. Everything else here the room has
        // already watched run in the routing demo.
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
                .chatModel(model)                 // planner LLM lives on the supervisor
                // This text IS the configuration, and it has to describe the scenario the
                // demo actually runs — the planner will follow it and stop early otherwise.
                .supervisorContext("""
                        Always call the nurse first: she takes the call, works out what is \
                        going on, and ends by naming who it needs. She never treats and \
                        never trains, so her answer is NEVER the answer to give back — it \
                        tells you who to call next. When she says NEEDS: vet, call the vet \
                        with the original worry; NEEDS: trainer, call the trainer; NEEDS: \
                        everyday care, call everyday care. Only when she says NEEDS: nobody \
                        is her own answer enough. You are finished once the specialist she \
                        named has answered.""")
                // Explicit, because the planner reading the previous answer IS the mechanism.
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
     *
     * <p>The count is the whole point of the demo. With only the last answer on screen — which is
     * what {@code SupervisorResponseStrategy.LAST} gives you — three calls and one call look
     * identical, and the pattern reads as a router with extra steps.
     */
    private static String answerWithItsRoute(AgenticScope scope) {
        var calls = scope.agentInvocations().stream()
                .filter(i -> DESKS.contains(i.agentName()))
                .toList();
        if (calls.isEmpty()) {
            return "The supervisor called nobody.";
        }

        // Only the LAST answer is the answer. Everything before it was the supervisor working
        // out who to ask — and printing those as peer blocks is what made this read as a
        // fan-out: three answers of equal weight is exactly what a parallel workflow produces.
        var settled = calls.get(calls.size() - 1);
        String answer = strip(settled.output());
        if (calls.size() == 1) {
            return "**" + settled.agentName() + " answered it.**\n\n" + answer;
        }

        String path = calls.stream().map(AgentInvocation::agentName)
                .collect(java.util.stream.Collectors.joining(" → "));
        StringBuilder out = new StringBuilder("**" + path + "**\n\n" + answer + "\n\n---\n");
        // The earlier calls appear once, small, as the REASON the next one happened — never as
        // an answer in their own right, because they were not one.
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
        // Drawn as a star this was a wheel with four equal spokes, which is a picture of a
        // fan-out — the exact thing the demo spends five minutes denying. Laid out left to
        // right it is a picture of a decision instead: the worry arrives at the supervisor,
        // never at an agent, and the only arrow that comes BACK is the nurse's.
        Topology.Graph topo = graph("stages",
                List.of(node("in", "worry", "input", 0),
                        node("supervisor", "Supervisor", "supervisor", 1)
                                .withSub("asks, reads, asks again"),
                        node("nurse", "TriageNurse", "agent", 2).withSub("1 · always first"),
                        node("care", "EverydayCare", "agent", 2).withSub("2 · if she says so"),
                        node("trainer", "DogTrainer", "agent", 2).withSub("2 · if she says so"),
                        node("vet", "EmergencyVet", "agent", 2).withSub("2 · if she says so")),
                // Both directions on the nurse: the supervisor invokes her and READS the answer,
                // which is the edge the whole demo turns on, and the only two-way pair on the
                // page. The other three are one-way because only one of them is ever called,
                // and only after her.
                // Only the return arrow is labelled. Both halves of a two-way pair bow through
                // the same gap, so "invoke" and the answer landed on top of each other — and of
                // the two it is the answer that carries the mechanism. Same convention as the
                // blackboard, where the write is labelled and the read is not.
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
                        + "never done that before in his life.",
                SupervisorPattern::run);
    }
}
