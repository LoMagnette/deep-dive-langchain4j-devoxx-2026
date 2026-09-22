package dev.devoxx.dashboard.demos._14_debate;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;

import java.util.List;
import java.util.Map;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._14_debate.Keys.Motion;
import dev.devoxx.dashboard.demos._14_debate.Keys.Verdict;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.patterns.debate.ConvergenceStrategy;
import dev.langchain4j.agentic.patterns.debate.DebatePlanner;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for the <b>debate</b> demo — two strong cases, and a ruling the room can check.
 */
public final class DebatePattern {

    private DebatePattern() {
    }

    /** The wiring. Everything below it is the dashboard telling itself how to draw this. */
    static String run(ChatModel model, String input, StreamingListener listener) {
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
                .outputKey(Verdict.class)
                .build();
        UntypedAgent app = AgenticServices.plannerBuilder()
                .subAgents(take, leave, verdict) // last sub-agent is the judge
                .planner(() -> new DebatePlanner(2, ConvergenceStrategy.unanimous()))
                .outputKey(Verdict.class)
                .listener(listener)
                .build();
        var r = app.invokeWithAgenticScope(Map.of(new Motion().name(), input));
        return String.valueOf(r.result());
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // Columns, not a circle: a debate has a direction — motion, argument, ruling. The two
        // advocates share the middle column, so their rebuttals bow between them.
        Topology.Graph topo = graph("stages",
                List.of(node("in", "motion", "input", 0),
                        node("take", "TakeHimAdvocate", "agent", 1),
                        node("leave", "LeaveHimAdvocate", "agent", 1),
                        node("verdict", "HolidayVerdict", "judge", 2)
                                .withSub("only if they never agree")),
                List.of(edge("in", "take"), edge("in", "leave"),
                        edge("take", "leave", "rebut"), edge("leave", "take", "up to 2 rounds"),
                        edge("take", "verdict"), edge("leave", "verdict")));
        return new PatternDef("debate", "Debate", "pattern-zoo",
                "And before any of it, two weeks in Tuscany in August. Does he come? Both of "
                        + "you are certain, and not about the same thing.",
                null,
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
                DebatePattern::run);
    }
}
