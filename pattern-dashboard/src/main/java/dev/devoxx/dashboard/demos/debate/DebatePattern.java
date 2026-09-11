package dev.devoxx.dashboard.demos.debate;

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
import dev.langchain4j.agentic.patterns.debate.ConvergenceStrategy;
import dev.langchain4j.agentic.patterns.debate.DebatePlanner;

/**
 * Wiring for the <b>debate</b> demo — two strong cases, and a ruling the room can check.
 */
public final class DebatePattern {

    private DebatePattern() {
    }

    public static PatternDef define() {
        Topology.Graph topo = graph("mesh",
                List.of(node("in", "motion", "input"),
                        node("take", "TakeHimAdvocate", "agent"),
                        node("leave", "LeaveHimAdvocate", "agent"),
                        node("verdict", "HolidayVerdict", "judge")),
                List.of(edge("in", "take"), edge("in", "leave"),
                        edge("take", "leave", "rebut"), edge("leave", "take", "rebut"),
                        edge("take", "verdict"), edge("leave", "verdict")));
        Runner runner = (model, input, listener) -> {
            var take = AgenticServices.agentBuilder(TakeHimAdvocate.class)
                    .chatModel(model)
                    .name("TakeHimAdvocate")
                    .build();
            var leave = AgenticServices.agentBuilder(LeaveHimAdvocate.class)
                    .chatModel(model)
                    .name("LeaveHimAdvocate")
                    .build();
            var verdict = AgenticServices.agentBuilder(HolidayVerdict.class)
                    .chatModel(model)
                    .name("HolidayVerdict")
                    .outputKey("verdict")
                    .build();
            UntypedAgent app = AgenticServices.plannerBuilder()
                    .subAgents(take, leave, verdict) // last sub-agent is the judge
                    .planner(() -> new DebatePlanner(2, ConvergenceStrategy.unanimous()))
                    .outputKey("verdict")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("motion", input));
            return String.valueOf(r.result());
        };
        return new PatternDef("debate", "Debate", "pattern-zoo",
                "Agents argue opposing sides for N rounds; a judge rules. The value is not the "
                        + "drama: ask one agent and it picks a side and then rationalises it, "
                        + "whereas a debate forces the case against the winner to be said out "
                        + "loud first. Both sides here are genuinely strong, which is the only "
                        + "time it is worth the tokens.",
                // caveat: eloquence can beat correctness; more rounds cost more tokens.
                "The most persuasive agent may win over the most correct one — and it is "
                        + "token-hungry. Check the ruling against the facts yourself; that is why "
                        + "the motion states them.",
                topo,
                "two weeks in Tuscany in August: take Zao, or leave him with a sitter? It is a "
                        + "twelve-hour drive, the house has no shade, and he has never been left "
                        + "for more than two nights.",
                runner);
    }
}
