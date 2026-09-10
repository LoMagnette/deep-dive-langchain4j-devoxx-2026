package dev.devoxx.dashboard;

import static dev.devoxx.dashboard.Topology.edge;
import static dev.devoxx.dashboard.Topology.graph;
import static dev.devoxx.dashboard.Topology.node;
import static dev.devoxx.dashboard.Wiring.agent;

import java.util.List;

import dev.devoxx.dashboard.PatternDef.Runner;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;

/** The other end of the dial: the LLM decides which agent runs next, and when to stop. */
final class PureAgentPatterns {

    private PureAgentPatterns() {
    }

    /** In the order the rail and the gallery show them. */
    static List<PatternDef> all() {
        return List.of(supervisor());
    }

    // 7 — supervisor (pure agent: LLM plans which sub-agent to call)
    private static PatternDef supervisor() {
        Topology.Graph topo = graph("star",
                List.of(node("supervisor", "Supervisor", "supervisor"),
                        node("activity", "ActivityPlanner", "agent"),
                        node("meal", "MealPlanner", "agent")),
                // Both directions: the supervisor invokes, reads the result, then decides again.
                // One-way arrows would draw a static fan-out instead of a planning loop.
                List.of(edge("supervisor", "activity", "invoke"),
                        edge("activity", "supervisor", "result"),
                        edge("supervisor", "meal", "invoke"),
                        edge("meal", "supervisor", "result")));
        Runner runner = (model, input, listener) -> {
            var activity = agent(Agents.ActivityPlanner.class, model, "ActivityPlanner", null);
            var meal = agent(Agents.MealPlanner.class, model, "MealPlanner", null);
            SupervisorAgent sup = AgenticServices.supervisorBuilder()
                    .subAgents(activity, meal)
                    .chatModel(model)                 // planner LLM lives on the supervisor
                    .responseStrategy(SupervisorResponseStrategy.LAST)
                    .maxAgentsInvocations(3)
                    .listener(listener)
                    .build();
            var r = sup.invokeWithAgenticScope(input);
            return String.valueOf(r.result());
        };
        return new PatternDef("supervisor", "Supervisor", "pure-agent",
                "An LLM supervisor dynamically decides which specialist to invoke, and when.",
                "Non-deterministic and needs a capable planner LLM; bound invocations to stay safe.",
                topo, "plan a great Saturday for the dog Zao", runner);
    }
}
