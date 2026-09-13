package dev.devoxx.dashboard.demos.supervisor;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;

import java.util.List;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.PatternDef.Runner;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos.conditional.DogTrainer;
import dev.devoxx.dashboard.demos.conditional.EmergencyVet;
import dev.devoxx.dashboard.demos.conditional.EverydayCare;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;

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

    public static PatternDef define() {
        Topology.Graph topo = graph("star",
                List.of(node("supervisor", "Supervisor", "supervisor"),
                        node("care", "EverydayCare", "agent"),
                        node("trainer", "DogTrainer", "agent"),
                        node("vet", "EmergencyVet", "agent")),
                // Both directions: the supervisor invokes, reads the result, then decides again.
                // One-way arrows would draw a static fan-out instead of a planning loop.
                List.of(edge("supervisor", "care", "invoke"), edge("care", "supervisor", "result"),
                        edge("supervisor", "trainer", "invoke"),
                        edge("trainer", "supervisor", "result"),
                        edge("supervisor", "vet", "invoke"), edge("vet", "supervisor", "result")));

        Runner runner = (model, input, listener) -> {
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
                    .subAgents(care, trainer, vet)
                    .chatModel(model)                 // planner LLM lives on the supervisor
                    .responseStrategy(SupervisorResponseStrategy.LAST)
                    .maxAgentsInvocations(4)
                    .listener(listener)
                    .build();
            var r = sup.invokeWithAgenticScope(input);
            return String.valueOf(r.result()).replaceAll("(?is)\\s*(ANSWERED|ESCALATE)\\s*$", "");
        };

        return new PatternDef("supervisor", "Supervisor", "pure-agent",
                // The beat this demo plays in the running narration.
                "Then everything at once, and no idea which of them to ring first.",
                // What this demo inherits from the ones before it.
                "Demo 6's three desks again, and not one new agent. Routing picks one of "
                        + "them; this picks several and decides when to stop.",
                "An LLM supervisor decides which specialist to invoke, and when to stop. These are "
                        + "the same three agents the router chose between two demos ago — not one "
                        + "new line of agent code — so the only thing that changed is who decides. "
                        + "Routing picks one. This picks several, in an order nobody wrote down, "
                        + "and stops when it judges the job done.",
                "Non-deterministic and needs a capable planner LLM; bound invocations to stay "
                        + "safe. Ask yourself first whether you could have written the steps "
                        + "down — if you could, a sequence is cheaper and debuggable.",
                topo,
                "he has been off his food for two days, he is scratching one ear raw, and he has "
                        + "started barking at the postman — I do not know who to ring first",
                runner);
    }
}
