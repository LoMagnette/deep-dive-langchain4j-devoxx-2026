package dev.devoxx.dashboard.demos._06_conditional;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static dev.devoxx.dashboard.support.Parsing.category;

import java.util.List;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._06_conditional.Keys.Category;
import dev.devoxx.dashboard.demos._06_conditional.Keys.Response;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for <b>Mission 6</b> — one classifier, four Rangers, exactly one of them sent.
 */
public final class ConditionalPattern {

    private ConditionalPattern() {
    }

    static String run(ChatModel model, String input, StreamingListener listener) {
        var zao = AgenticServices.agentBuilder(ZaoClassifies.class)
                .chatModel(model)
                .name("Zao")
                .outputKey(Category.class)
                .build();
        var sniff = AgenticServices.agentBuilder(SniffOnCall.class)
                .chatModel(model).name("Sniff").outputKey(Response.class).build();
        var dig = AgenticServices.agentBuilder(DigOnCall.class)
                .chatModel(model).name("Dig").outputKey(Response.class).build();
        var doc = AgenticServices.agentBuilder(DocOnCall.class)
                .chatModel(model).name("Doc").outputKey(Response.class).build();
        var zoom = AgenticServices.agentBuilder(ZoomOnCall.class)
                .chatModel(model).name("Zoom").outputKey(Response.class).build();

        OneRanger route = AgenticServices.conditionalBuilder(OneRanger.class)
                .name("Conditional")
                .subAgents(s -> "lost".equals(category(s.readState(Category.class))), sniff)
                .subAgents(s -> "underground".equals(category(s.readState(Category.class))), dig)
                .subAgents(s -> "hurt".equals(category(s.readState(Category.class))), doc)
                .subAgents(s -> "urgent".equals(category(s.readState(Category.class))), zoom)
                .build();

        EmergencyPhone app = AgenticServices.sequenceBuilder(EmergencyPhone.class)
                .name("Sequential")
                .subAgents(zao, route)
                .outputKey(Response.class)
                .listener(listener)
                .build();
        return app.answer(input);
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        Topology.Graph topo = graph("branch",
                List.of(node("in", "call", "input"),
                        node("zao", "Zao", "router").withSub("classifies the call").as("zao"),
                        node("sniff", "Sniff", "agent").withSub("something lost").as("sniff"),
                        node("dig", "Dig", "agent").withSub("stuck underground").as("dig"),
                        node("doc", "Doc", "agent").withSub("someone hurt").as("doc"),
                        node("zoom", "Zoom", "agent").withSub("far away, urgent").as("zoom")),
                List.of(edge("in", "zao"),
                        edge("zao", "sniff", "lost"),
                        edge("zao", "dig", "underground"),
                        edge("zao", "doc", "hurt"),
                        edge("zao", "zoom", "urgent")));
        return new PatternDef("conditional", "Conditional Routing", "workflow",
                "The emergency phone rings at Pup HQ. Four Rangers on the bench, one of them "
                        + "goes — and the room gets to guess which.",
                "Introduces the four Rangers on call that missions 9, 16 and 19 all reuse.",
                "A classifier agent writes `category`, and a conditional workflow sends exactly "
                        + "one Ranger. Worth it when mis-routing is expensive — and everyone in "
                        + "the room can grade it: send a tortoise down a well (Dig), \"my "
                        + "glasses are gone\" (Sniff), \"Grandpa slipped and his ankle is "
                        + "swelling\" (Doc), \"the ice-cream van is rolling downhill with nobody "
                        + "in it\" (Zoom). Let the audience call it before the run does.",
                "Only as good as the classifier, and unseen calls fall through the cracks — so "
                        + "choose which way they fall. The fallback here is Doc: when Zao cannot "
                        + "tell, the tolerable mistake is sending the medic.",
                topo,
                "Pup HQ, emergency phone: Mrs Pebble's tortoise has fallen down the old well "
                        + "behind the bakery, and she can hear him shouting.",
                ConditionalPattern::run);
    }
}
