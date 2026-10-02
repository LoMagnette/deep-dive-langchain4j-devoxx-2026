package dev.devoxx.dashboard.demos._16_customplanner;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;

import java.util.List;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._06_conditional.DigOnCall;
import dev.devoxx.dashboard.demos._06_conditional.DocOnCall;
import dev.devoxx.dashboard.demos._06_conditional.SniffOnCall;
import dev.devoxx.dashboard.demos._06_conditional.ZoomOnCall;
import dev.devoxx.dashboard.demos._16_customplanner.Keys.Schedule;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for <b>Mission 16</b> — Mission 6's four Rangers, run by a planner you wrote.
 */
public final class CustomPlannerPattern {

    private CustomPlannerPattern() {
    }

    static String run(ChatModel model, String input, StreamingListener listener) {
        var sniff = AgenticServices.agentBuilder(SniffOnCall.class)
                .chatModel(model).name("Sniff").build();
        var zoom = AgenticServices.agentBuilder(ZoomOnCall.class)
                .chatModel(model).name("Zoom").build();
        var dig = AgenticServices.agentBuilder(DigOnCall.class)
                .chatModel(model).name("Dig").build();
        var doc = AgenticServices.agentBuilder(DocOnCall.class)
                .chatModel(model).name("Doc").build();

        DaysWork app = AgenticServices.plannerBuilder(DaysWork.class)
                .subAgents(sniff, zoom, dig, doc)
                // Same builder as every planner before it. The only difference is that this
                // planner is a page of this repo instead of a page of the library.
                .planner(NapSchedule::new)
                .outputKey(Schedule.class)
                .listener(listener)
                .build();
        var r = app.invoke(input);
        return "**Zao's schedule for the day**\n\n" + String.valueOf(r.result()).lines()
                .map(l -> l.startsWith("   ") ? "  - *" + l.strip() + "*" : "- " + l)
                .reduce((a, b) -> a + "\n" + b).orElse("");
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // The planner is drawn as framework-shaped but labelled as ours: the dotted box is code
        // you wrote. Its two silent outcomes — feed and nap — are on the box, because they call
        // nobody and so never light anything.
        Topology.Graph topo = graph("stages",
                List.of(node("in", "roster", "input", 0).withSub("missions · energy"),
                        node("plan", "NapSchedule", "planner", 1).withSub("your Java · feed or nap").as("zao"),
                        node("sniff", "Sniff", "agent", 2).withSub("if energy > 70").as("sniff"),
                        node("zoom", "Zoom", "agent", 2).withSub("if energy > 70").as("zoom"),
                        node("dig", "Dig", "agent", 2).withSub("if energy > 70").as("dig"),
                        node("doc", "Doc", "agent", 2).withSub("if energy > 70").as("doc"),
                        node("out", "schedule", "join", 3).withSub("queue empty, or bedtime")),
                List.of(edge("in", "plan"),
                        edge("plan", "sniff"), edge("sniff", "plan", "report"),
                        edge("plan", "zoom"), edge("zoom", "plan"),
                        edge("plan", "dig"), edge("dig", "plan"),
                        edge("plan", "doc"), edge("doc", "plan"),
                        edge("plan", "out", "done")));
        return new PatternDef("customPlanner", "Custom Planner (write your own)", "minds",
                "Four missions, four tired Rangers, and Zao's one rule: nobody does two missions "
                        + "in a row. Zoom is hungry.",
                "Mission 6's four Rangers on call, one more time — now run by your own code.",
                "Every planner above implements one small interface; here is one written by hand. "
                        + "`nextAction` reads the board and decides in plain Java: **hungry → "
                        + "feed; energy above 70 → go; otherwise → nap**, and nobody twice in a "
                        + "row. Feeding and napping change the board and call nobody, so the loop "
                        + "runs on until a Ranger is sent or it is bedtime. Watch `Energy`, "
                        + "`LastOnMission` and `MissionQueue` change in the Scope tab.",
                "You own the loop now. Nothing stops a planner from never terminating or calling "
                        + "the same agent for ever — the empty queue and `BEDTIME` are the guard "
                        + "rails, and they are yours to write. Reach for this only when a built-in "
                        + "builder genuinely cannot say what you mean.",
                topo,
                "Missions: find the Mayor's reading glasses; fetch the post from the station; dig "
                        + "out the blocked drain on Elm Street; check the new puppy at number 4. "
                        + "Energy: Sniff 90, Zoom 75, Dig 40, Doc 80. Zoom is hungry.",
                CustomPlannerPattern::run);
    }
}
