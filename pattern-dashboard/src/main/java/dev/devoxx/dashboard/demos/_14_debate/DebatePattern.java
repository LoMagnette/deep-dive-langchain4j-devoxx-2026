package dev.devoxx.dashboard.demos._14_debate;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static java.util.Objects.requireNonNullElse;

import java.util.List;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._14_debate.Keys.HowlTurn;
import dev.devoxx.dashboard.demos._14_debate.Keys.MittensTurn;
import dev.devoxx.dashboard.demos._14_debate.Keys.Transcript;
import dev.devoxx.dashboard.demos._14_debate.Keys.Verdict;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for <b>Mission 14</b> — a loop of two debaters and a minute-taker, then a judge.
 */
public final class DebatePattern {

    private DebatePattern() {
    }

    /** The council meets for three rounds, whatever happens. */
    public static final int ROUNDS = 3;

    static String run(ChatModel model, String input, StreamingListener listener) {
        var howl = AgenticServices.agentBuilder(HowlArgues.class)
                .chatModel(model).name("Howl").outputKey(HowlTurn.class).build();
        var mittens = AgenticServices.agentBuilder(MittensArgues.class)
                .chatModel(model).name("Mittens").outputKey(MittensTurn.class).build();
        var bolt = new BoltMinutes();
        var fifi = AgenticServices.agentBuilder(FifiJudges.class)
                .chatModel(model).name("Fifi").outputKey(Verdict.class).build();

        Rounds rounds = AgenticServices.loopBuilder(Rounds.class)
                .name("Loop")
                .subAgents(howl, mittens, bolt)
                .maxIterations(ROUNDS)
                .build();

        Council app = AgenticServices.sequenceBuilder(Council.class)
                .name("Sequential")
                .subAgents(rounds, fifi)
                .outputKey(Verdict.class)
                .listener(listener)
                .build();
        // The transcript is seeded because both debaters read it from their first turn on.
        var r = app.meet(input, "(the debate is about to begin)");
        return "**Fifi's verdict**\n\n" + r.result()
                + "\n\n---\n\n" + requireNonNullElse(r.agenticScope().readState(Transcript.class), "");
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // Columns: motion, three rounds, ruling. The two debaters share a column so their
        // answers bow between them; Bolt's minutes sit beside them because they happen inside
        // the same loop; Fifi rules only once the loop is done.
        Topology.Graph topo = graph("stages",
                List.of(node("in", "motion", "input", 0),
                        node("howl", "Howl", "agent", 1).withSub("for the dog park").as("howl"),
                        node("mittens", "Mittens", "agent", 1).withSub("for the cat café").as("mittens"),
                        node("bolt", "Bolt", "code", 2).withSub("keeps the minutes").as("bolt"),
                        node("fifi", "Fifi", "judge", 3).withSub("after round 3").as("fifi")),
                List.of(edge("in", "howl"), edge("in", "mittens"),
                        edge("howl", "mittens", "answers"),
                        edge("mittens", "howl", "3 rounds"),
                        edge("howl", "bolt"), edge("mittens", "bolt", "each turn"),
                        edge("bolt", "fifi", "transcript")));
        return new PatternDef("debate", "Debate", "minds",
                "The town council must decide: the empty lot on Elm Street becomes a dog park, "
                        + "or a cat café. Mittens has prepared.",
                null,
                "Two agents argue opposing sides for three rounds, each answering the other, and "
                        + "a judge rules on the transcript. Built from parts the room already "
                        + "knows: a **loop** of Howl, Mittens and Bolt (who appends each round to "
                        + "the transcript, word for word), then Fifi in a sequence after it. Ask "
                        + "one agent and it picks a side and rationalises it; a debate makes the "
                        + "case against the winner get said out loud first.",
                "The most persuasive agent may beat the most correct one, and it is token-hungry "
                        + "— three rounds is six calls before anyone rules. Let the audience vote "
                        + "before Fifi announces: the interesting moment is when they disagree.",
                topo,
                "Barkville town council: should the empty lot on Elm Street become a dog park or a "
                        + "cat café? Howl speaks for the dog park, Mittens for the cat café.",
                DebatePattern::run);
    }
}
