package dev.devoxx.dashboard.demos._18_lakeparty;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static java.util.Objects.requireNonNullElse;

import java.util.List;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._13_voting.RivetVotes;
import dev.devoxx.dashboard.demos._13_voting.DocVotes;
import dev.devoxx.dashboard.demos._13_voting.Keys.Vote1;
import dev.devoxx.dashboard.demos._13_voting.Keys.Vote2;
import dev.devoxx.dashboard.demos._13_voting.SniffVotes;
import dev.devoxx.dashboard.demos._13_voting.VotingPattern;
import dev.devoxx.dashboard.demos._18_lakeparty.Keys.Announcement;
import dev.devoxx.dashboard.demos._18_lakeparty.Keys.Finding;
import dev.devoxx.dashboard.demos._18_lakeparty.Keys.Findings;
import dev.devoxx.dashboard.demos._18_lakeparty.Keys.IceVerdict;
import dev.devoxx.dashboard.demos._18_lakeparty.Keys.Spots;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.patterns.voting.VotingPlanner;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for <b>Mission 18</b> — a mapper, an adapter, a vote and an announcement.
 */
public final class LakePartyPattern {

    private LakePartyPattern() {
    }

    /** Where Sniff is sent. Derived here, not by an agent: the mapper needs a real list first. */
    static final List<String> SPOTS = List.of("the middle of the lake",
            "by the reeds, where the stream comes in", "the end of the jetty", "round the island");

    static String run(ChatModel model, String input, StreamingListener listener) {
        // 1. Parallel mapper — one Sniff, once per spot.
        var spot = AgenticServices.agentBuilder(SniffChecksSpot.class)
                .chatModel(model).name("Sniff").outputKey(Finding.class).build();
        IceSurvey survey = AgenticServices.parallelMapperBuilder(IceSurvey.class)
                .name("ParallelMapper")
                .subAgents(spot)
                .itemsProvider(new Spots().name())
                .outputKey(Findings.class)
                .build();

        // 2. Glue — the findings written back into the mission the voters read.
        var report = new IceReport();

        // 3. Mission 13's vote, unchanged: the same VotingPlanner, the same VETO strategy, nested
        //    as one step. Its verdict is pinned this time, so the announcement can read it.
        var sniff = AgenticServices.agentBuilder(SniffVotes.class)
                .chatModel(model).name("SniffVote").outputKey(Vote1.class).build();
        var doc = AgenticServices.agentBuilder(DocVotes.class)
                .chatModel(model).name("Doc").outputKey(Vote2.class).build();
        IceBallot ballot = AgenticServices.plannerBuilder(IceBallot.class)
                .subAgents(sniff, doc, new RivetVotes())
                .planner(() -> new VotingPlanner(VotingPattern.VETO))
                .outputKey(IceVerdict.class)
                .build();

        // 4. Howl tells the town.
        var howl = AgenticServices.agentBuilder(HowlAnnounces.class)
                .chatModel(model).name("Howl").outputKey(Announcement.class).build();

        LakeParty app = AgenticServices.sequenceBuilder(LakeParty.class)
                .name("Sequential")
                .subAgents(survey, report, ballot, howl)
                .outputKey(Announcement.class)
                .listener(listener)
                .build();
        var r = app.decide(input, SPOTS);
        return "**Howl, to all of Barkville:** " + r.result() + "\n\n---\n\n"
                + VotingPattern.explain(r.agenticScope(),
                        requireNonNullElse(r.agenticScope().readState(IceVerdict.class), ""));
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        Topology.Graph topo = graph("stages",
                List.of(node("in", "the lake", "input", 0),
                        node("spot", "Sniff", "agent", 1).withSub("once per spot").asStack().as("sniff"),
                        node("report", "IceReport", "code", 2).withSub("glue · spots → mission"),
                        node("vsniff", "SniffVote", "agent", 3).withSub("Mission 13").as("sniff"),
                        node("doc", "Doc", "agent", 3).withSub("Mission 13").as("doc"),
                        node("rivet", "Rivet", "code", 3).withSub("Mission 13").as("rivet"),
                        node("veto", "VETO", "join", 4).withSub("VotingPlanner's strategy"),
                        node("howl", "Howl", "agent", 5).withSub("tells the town").as("howl")),
                List.of(edge("in", "spot", "4 spots"),
                        edge("spot", "report", "findings"),
                        edge("report", "vsniff"), edge("report", "doc", "the full report"),
                        edge("report", "rivet"),
                        edge("vsniff", "veto"), edge("doc", "veto"), edge("rivet", "veto"),
                        edge("veto", "howl", "iceVerdict")));
        return new PatternDef("lakeParty", "The Lake Party (composite)", "composite",
                "The Mayor will not take a vote for an answer. Sniff checks the ice spot by spot, "
                        + "and then the Rangers vote again.",
                "Mission 13's three voters and their veto, now judging a proper inspection rather "
                        + "than a glance from the bank.",
                "Two patterns carried by plumbing: a **parallel mapper** sends Sniff to four spots "
                        + "at once, a plain-Java step writes the findings up, **the vote** — the "
                        + "same three voters, the same veto — decides, and Howl announces it. The "
                        + "result shows the whole count, so the last third of the diagram is "
                        + "visibly doing the deciding.",
                "Most of this system is glue. The voters read `Mission`, because that is the key "
                        + "they declared — so the inspection is written back INTO the mission, "
                        + "original words first, or Rivet's ruler loses the measured thickness. "
                        + "Reusing an agent means accepting the key it already declared.",
                topo, VotingPattern.LAKE, LakePartyPattern::run)
                .gist("A mapper, a veto vote and an announcement, wired as one system.");
    }
}
