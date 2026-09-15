package dev.devoxx.dashboard.demos.supervisor;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;

import java.util.List;
import java.util.Locale;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos.conditional.DogTrainer;
import dev.devoxx.dashboard.demos.conditional.EmergencyVet;
import dev.devoxx.dashboard.demos.conditional.EverydayCare;
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
 * met: not one new agent. Demo 6 routes to <b>one</b> desk, chosen by a classifier you wrote.
 * Here the model calls as many as it thinks it needs and decides when it is done — which is the
 * only thing routing cannot do, and exactly what this input requires.
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
                "Then something that is not like him at all, and you cannot tell whether it is "
                        + "behaviour or something worse.",
                "Demo 6's three desks again, and not one new agent. Routing picks one of "
                        + "them; this picks several and decides when to stop.",
                "An LLM supervisor decides which specialist to invoke, and when to stop — the "
                        + "same three agents the router chose between two demos ago, not one new "
                        + "line of agent code. Watch the order: it asks the trainer, the trainer "
                        + "says this is not a training problem, and **that answer is what makes "
                        + "it call the vet.** A router gets one call and stops. A fan-out would "
                        + "have asked all three at once and learned nothing from any of them. "
                        + "Neither can produce a second call that exists only because of what "
                        + "the first one said. Note what the result shows: **one answer**, with "
                        + "the route to it underneath. The trainer did not give an opinion worth "
                        + "keeping — it declined, and declining is work, not output.",
                "Non-deterministic, and the roll-call is honest about it: a weaker planner will "
                        + "sometimes take the trainer's first sentence and stop. Bound the "
                        + "invocations. And ask the hard question first — if you can write down "
                        + "\"trainer, then vet if they say so\", that is a sequence with a "
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
