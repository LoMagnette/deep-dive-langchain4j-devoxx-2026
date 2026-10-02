package dev.devoxx.dashboard.demos._13_voting;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static java.util.Objects.requireNonNullElse;

import java.util.List;
import java.util.Locale;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._13_voting.Keys.Vote1;
import dev.devoxx.dashboard.demos._13_voting.Keys.Vote2;
import dev.devoxx.dashboard.demos._13_voting.Keys.Vote3;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for <b>Mission 13</b> — three independent votes, and a rule for counting them.
 */
public final class VotingPattern {

    private VotingPattern() {
    }

    /** Shared with Mission 18, which puts the same lake through a whole inspection first. */
    public static final String LAKE =
            "The Mayor wants a skating party on Barkville Lake this afternoon. The ice is 12 cm "
                    + "thick in the middle. There is a dark patch near the reeds where the stream "
                    + "comes in, and the ducks are walking about on it quite happily.";

    static String run(ChatModel model, String input, StreamingListener listener) {
        // Three DIFFERENT ways of judging, or the count is decoration: a nose, a medic, a ruler.
        var sniff = AgenticServices.agentBuilder(SniffVotes.class)
                .chatModel(model).name("Sniff").outputKey(Vote1.class).build();
        var doc = AgenticServices.agentBuilder(DocVotes.class)
                .chatModel(model).name("Doc").outputKey(Vote2.class).build();
        var bolt = new BoltVotes();

        IceVote app = AgenticServices.parallelBuilder(IceVote.class)
                .name("Parallel")
                .subAgents(sniff, doc, bolt)
                .output(VotingPattern::count)
                .listener(listener)
                .build();
        return app.vote(input);
    }

    // ---- the aggregation step: part of the design, so it is shown in full ----

    /** Both rules, side by side: the strategy is the decision, and the room should see it. */
    public static String count(AgenticScope scope) {
        List<String> votes = List.of(
                requireNonNullElse(scope.readState(Vote1.class), ""),
                requireNonNullElse(scope.readState(Vote2.class), ""),
                requireNonNullElse(scope.readState(Vote3.class), ""));
        long safe = votes.stream().filter(VotingPattern::isSafe).count();
        String majority = safe >= 2 ? "SAFE" : "NOT SAFE";
        String veto = safe == votes.size() ? "SAFE" : "NOT SAFE";
        return "**Verdict: " + veto + "** — one \"not safe\" is a veto, because this is ice.\n\n"
                + "- Sniff: " + votes.get(0) + "\n"
                + "- Doc: " + votes.get(1) + "\n"
                + "- Bolt: " + votes.get(2) + "\n\n"
                + "*A plain majority would have said " + majority + " (" + safe + " of 3). That rule is "
                + "fine for naming the HQ mascot.*";
    }

    /** NOT SAFE contains SAFE, so the negative is checked first. */
    static boolean isSafe(String vote) {
        String v = vote.toUpperCase(Locale.ROOT);
        return !v.contains("NOT SAFE") && !v.contains("UNSAFE") && v.contains("SAFE");
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        Topology.Graph topo = graph("fanout",
                List.of(node("in", "the lake", "input"),
                        node("sniff", "Sniff", "agent").withSub("nose and eyes").as("sniff"),
                        node("doc", "Doc", "agent").withSub("the medic").as("doc"),
                        node("bolt", "Bolt", "code").withSub("the ruler · ≥ 10 cm").as("bolt"),
                        // Without the count this is just a fan-out; the count IS the pattern,
                        // and its rule is written on the box because the rule is the design.
                        node("vote", "veto()", "join").withSub("one NOT SAFE wins")),
                List.of(edge("in", "sniff"), edge("in", "doc"), edge("in", "bolt"),
                        edge("sniff", "vote", "vote1"), edge("doc", "vote", "vote2"),
                        edge("bolt", "vote", "vote3")));
        return new PatternDef("voting", "Voting / Ensemble", "minds",
                "The Mayor wants a skating party on the lake. Twelve centimetres of ice, a dark "
                        + "patch by the reeds, and some very confident ducks.",
                "Introduces the three voters the Lake Party reuses in Mission 18.",
                "Several agents answer independently, then one step aggregates. Sniff judges by "
                        + "nose, Doc by what could go wrong, Bolt by a ruler — three different "
                        + "criteria, which is what makes an ensemble worth its tokens. **The "
                        + "aggregation strategy is part of the design**: the result shows the "
                        + "majority (2 of 3 say SAFE) and the veto (Doc says no), and for ice "
                        + "the veto is the only rule.",
                "Correlated voters agree on the same mistake — diversity of criteria buys "
                        + "robustness, not the same prompt three times. And every voter must "
                        + "answer in a form that can be counted: SAFE or NOT SAFE, nothing a "
                        + "tally has to interpret.",
                topo, LAKE, VotingPattern::run);
    }
}
