package dev.devoxx.dashboard.demos.supervisor;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;

import java.util.List;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.PatternDef.Runner;
import dev.devoxx.dashboard.catalog.Topology;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;

/**
 * Wiring for the <b>supervisor</b> demo — pure agent: LLM plans which sub-agent to call.
 */
public final class SupervisorPattern {

    private SupervisorPattern() {
    }

    public static PatternDef define() {
        Topology.Graph topo = graph("star",
                List.of(node("supervisor", "Supervisor", "supervisor"),
                        node("routine", "RoutinePlanner", "agent"),
                        node("training", "TrainingPlanner", "agent")),
                // Both directions: the supervisor invokes, reads the result, then decides again.
                // One-way arrows would draw a static fan-out instead of a planning loop.
                List.of(edge("supervisor", "routine", "invoke"),
                        edge("routine", "supervisor", "result"),
                        edge("supervisor", "training", "invoke"),
                        edge("training", "supervisor", "result")));
        Runner runner = (model, input, listener) -> {
            var routine = AgenticServices.agentBuilder(RoutinePlanner.class)
                    .chatModel(model)
                    .name("RoutinePlanner")
                    .build();
            var training = AgenticServices.agentBuilder(TrainingPlanner.class)
                    .chatModel(model)
                    .name("TrainingPlanner")
                    .build();
            SupervisorAgent sup = AgenticServices.supervisorBuilder()
                    .subAgents(routine, training)
                    .chatModel(model)                 // planner LLM lives on the supervisor
                    .responseStrategy(SupervisorResponseStrategy.LAST)
                    .maxAgentsInvocations(3)
                    .listener(listener)
                    .build();
            var r = sup.invokeWithAgenticScope(input);
            return String.valueOf(r.result());
        };
        return new PatternDef("supervisor", "Supervisor", "pure-agent",
                "An LLM supervisor dynamically decides which specialist to invoke, and when to "
                        + "stop. The right shape when the request does not say what it needs: a "
                        + "baby arriving might mean changing the routine, teaching him new "
                        + "things, or both, and no amount of thinking up front tells you which.",
                "Non-deterministic and needs a capable planner LLM; bound invocations to stay "
                        + "safe. Ask yourself first whether you could have written the steps "
                        + "down — if you could, a sequence is cheaper and debuggable.",
                topo,
                "our first baby is due in three months and Zao has never been near one — he's "
                        + "four, he sleeps on our bed and he barges through doorways ahead of us",
                runner);
    }
}
