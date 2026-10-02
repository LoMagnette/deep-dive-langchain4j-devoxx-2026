package dev.devoxx.dashboard.demos._08_nonaiagent;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static dev.devoxx.dashboard.support.Parsing.firstNumber;

import java.util.List;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._08_nonaiagent.Keys.Ladder;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for <b>Mission 8</b> — Java first, a model second, and the sequence cannot tell.
 */
public final class NonAiAgentPattern {

    private NonAiAgentPattern() {
    }

    static String run(ChatModel model, String input, StreamingListener listener) {
        var bolt = new Bolt();

        var zoom = AgenticServices.agentBuilder(ZoomFetchesLadder.class)
                .chatModel(model)
                .tools(new ZoomGear())
                .name("Zoom")
                .outputKey(Ladder.class)
                .build();

        LadderRun app = AgenticServices.sequenceBuilder(LadderRun.class)
                .name("Sequential")
                .subAgents(bolt, zoom)
                .outputKey(Ladder.class)
                .listener(listener)
                .build();
        // Reading the height out of the sentence is ours, not a model's: plain Java, like Bolt.
        double height = firstNumber(input, 6.0);
        return "**Bolt:** the branch is " + height + " m up, so the ladder must be at least "
                + new Bolt().ladderLength(height) + " m.\n\n**Zoom:** " + app.fetch(height);
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // Bolt is NOT drawn as an agent, for the same reason Officer Jo is not: an identical box
        // beside Zoom's would say a model did the maths, which is the one thing this denies.
        Topology.Graph topo = graph("stages",
                List.of(node("in", "treeHeight", "input", 0),
                        node("bolt", "Bolt", "code", 1).withSub("no brain · never wrong").as("bolt"),
                        node("zoom", "Zoom", "agent", 2).withSub("fetches the ladder").as("zoom"),
                        node("t1", "fetch(item)", "code", 3).withSub("gear"),
                        node("t2", "deliver(item, place)", "code", 3).withSub("gear")),
                List.of(edge("in", "bolt"),
                        edge("bolt", "zoom", "ladderLength"),
                        edge("zoom", "t1", "it decides"), edge("zoom", "t2")));
        return new PatternDef("nonAiAgent", "Non-AI Agents (plain Java)", "team",
                "Meet Bolt: the silver robot dog. No brain, never wrong. Zoom has a brain, and "
                        + "would like a ladder.",
                "Mission 1's idea — an agent with gear — with a plain Java agent in front of it.",
                "An agent does not have to be a model. Bolt is a plain class with one `@Agent` "
                        + "method: its `@K` parameter is bound from the scope and its return "
                        + "value is written to `ladderLength`, so **the sequence cannot tell**. "
                        + "The test for a step is not \"could a model do this\" — it is \"is "
                        + "this judgement, or is it a calculation\". Six metres up means a 7.5 m "
                        + "ladder; watch Zoom pick that one and not the 5 m. Later, Bolt drops "
                        + "straight into Mission 2 between Sniff and Zoom — that is Mission 17.",
                "**A non-AI agent is invisible to the listener** in `1.20.0-beta30` — no brain, no "
                        + "events. `NonAiAgentInstance.setParent` sets the parent and never calls "
                        + "`registerInheritedParentListener`, so a plain-Java step inherits no "
                        + "listener, emits nothing, and is never timed: Bolt's box never lights. "
                        + "He is doing the work all the same — `LadderLength` appears in the "
                        + "Scope tab.\n\nThe smaller trap: `name` goes on the **annotation**. "
                        + "There is no builder to call `.name(\"X\")` on, and the default is the "
                        + "method name, so Bolt would be called \"ladderLength\" everywhere.",
                topo,
                "Paws up, Rangers! The kitten's branch on the Main Street oak is 6 metres up. "
                        + "Bolt, how long a ladder? Zoom, go and get it.",
                NonAiAgentPattern::run);
    }
}
