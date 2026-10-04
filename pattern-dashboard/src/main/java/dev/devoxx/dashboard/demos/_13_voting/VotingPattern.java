package dev.devoxx.dashboard.demos._13_voting;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static java.util.Objects.requireNonNullElse;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._13_voting.Keys.Verdict;
import dev.devoxx.dashboard.demos._13_voting.Keys.Vote1;
import dev.devoxx.dashboard.demos._13_voting.Keys.Vote2;
import dev.devoxx.dashboard.demos._13_voting.Keys.Vote3;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.patterns.voting.VotingPlanner;
import dev.langchain4j.agentic.patterns.voting.VotingStrategy;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for <b>Mission 13</b> — three independent votes, and a strategy for counting them.
 */
public final class VotingPattern {

    private VotingPattern() {
    }

    /** Shared with Mission 18, which puts the same lake through a whole inspection first. */
    public static final String LAKE =
            "The Mayor wants a skating party on Barkville Lake this afternoon. The ice is 12 cm "
                    + "thick in the middle. There is a dark patch near the reeds where the stream "
                    + "comes in, and the ducks are walking about on it quite happily.";

    /**
     * The rule for ice: one NOT SAFE is a veto. {@code VotingStrategy} is a one-method interface,
     * so a safety rule is a lambda — the strategy is part of the design, and this is the design.
     */
    public static final VotingStrategy VETO =
            votes -> votes.stream().allMatch(VotingPattern::isSafe) ? "SAFE" : "NOT SAFE";

    static String run(ChatModel model, String input, StreamingListener listener) {
        // Three DIFFERENT ways of judging, or the count is decoration: a nose, a medic, a ruler.
        var sniff = AgenticServices.agentBuilder(SniffVotes.class)
                .chatModel(model).name("Sniff").outputKey(Vote1.class).build();
        var doc = AgenticServices.agentBuilder(DocVotes.class)
                .chatModel(model).name("Doc").outputKey(Vote2.class).build();
        var bolt = new BoltVotes();

        IceVote app = AgenticServices.plannerBuilder(IceVote.class)
                .subAgents(sniff, doc, bolt)
                // VotingPlanner calls every voter at once, collects what each returned, and
                // hands the collection to the strategy — whose answer is the verdict.
                .planner(() -> new VotingPlanner(VETO))
                .outputKey(Verdict.class)
                .listener(listener)
                .build();
        var r = app.invoke(input);
        return explain(r.agenticScope(), String.valueOf(r.result()));
    }

    // ---- how the result is presented: both strategies, side by side ----

    /**
     * The verdict the planner returned, each vote, and what the library's own
     * {@code VotingStrategy.majority()} would have said about the same three votes — so the
     * room sees the strategy decide, not the voters.
     */
    public static String explain(AgenticScope scope, String verdict) {
        List<String> votes = List.of(
                requireNonNullElse(scope.readState(Vote1.class), ""),
                requireNonNullElse(scope.readState(Vote2.class), ""),
                requireNonNullElse(scope.readState(Vote3.class), ""));
        // majority() counts EQUAL votes, and a vote is "SAFE — the ducks are on it", so each is
        // read down to its verdict word first — exactly what a real ensemble has to do.
        Object majority = VotingStrategy.majority().aggregate(words(votes));
        return "**Verdict: " + verdict + "** — the veto strategy: one NOT SAFE wins, because this "
                + "is ice.\n\n"
                + "- Sniff: " + votes.get(0) + "\n"
                + "- Doc: " + votes.get(1) + "\n"
                + "- Bolt: " + votes.get(2) + "\n\n"
                + "*`VotingStrategy.majority()` on the same three votes would have said " + majority
                + ". That rule is fine for naming the HQ mascot.*";
    }

    private static List<Object> words(Collection<String> votes) {
        return votes.stream().map(v -> (Object) (isSafe(v) ? "SAFE" : "NOT SAFE")).toList();
    }

    /** NOT SAFE contains SAFE, so the negative is checked first. */
    static boolean isSafe(Object vote) {
        String v = String.valueOf(vote).toUpperCase(Locale.ROOT);
        return !v.contains("NOT SAFE") && !v.contains("UNSAFE") && v.contains("SAFE");
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // The planner fans out to all three at once — no voter sees another — and the strategy is
        // drawn as its own box, labelled with its rule, because the rule IS the design.
        Topology.Graph topo = graph("stages",
                List.of(node("in", "the lake", "input", 0),
                        node("plan", "VotingPlanner", "planner", 1).withSub("all three at once"),
                        node("sniff", "Sniff", "agent", 2).withSub("nose and eyes").as("sniff"),
                        node("doc", "Doc", "agent", 2).withSub("the medic").as("doc"),
                        node("bolt", "Bolt", "code", 2).withSub("the ruler · ≥ 10 cm").as("bolt"),
                        node("vote", "VETO", "join", 3).withSub("one NOT SAFE wins")),
                List.of(edge("in", "plan"),
                        edge("plan", "sniff"), edge("plan", "doc", "votes independently"),
                        edge("plan", "bolt"),
                        edge("sniff", "vote", "vote1"), edge("doc", "vote", "vote2"),
                        edge("bolt", "vote", "vote3")));
        return new PatternDef("voting", "Voting / Ensemble", "minds",
                "The Mayor wants a skating party on the lake. Twelve centimetres of ice, a dark "
                        + "patch by the reeds, and some very confident ducks.",
                "Introduces the three voters the Lake Party reuses in Mission 18.",
                "Several agents answer independently; a strategy aggregates. LangChain4j's "
                        + "`VotingPlanner` calls every voter at once, collects what each returned, "
                        + "and hands the lot to a `VotingStrategy` — `majority()`, `average()`, "
                        + "`highest()`, or your own, because it is a one-method interface. Sniff "
                        + "judges by nose, Doc by what could go wrong, Bolt by a ruler. **The "
                        + "strategy is part of the design**: here it is a veto — one NOT SAFE "
                        + "wins — and the result shows what `majority()` would have said instead "
                        + "(2 of 3 say SAFE).",
                "Correlated voters agree on the same mistake — diversity of criteria buys "
                        + "robustness, not the same prompt three times. And `majority()` counts "
                        + "EQUAL votes: \"SAFE — the ducks are on it\" and \"SAFE, 12 cm\" are two "
                        + "different votes to it, so every vote has to be read down to a word a "
                        + "tally can compare before any strategy means anything.",
                topo, LAKE, VotingPattern::run);
    }
}
