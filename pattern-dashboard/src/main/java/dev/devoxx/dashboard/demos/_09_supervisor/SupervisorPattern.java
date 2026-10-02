package dev.devoxx.dashboard.demos._09_supervisor;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static java.util.stream.Collectors.joining;

import java.util.List;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._06_conditional.DigOnCall;
import dev.devoxx.dashboard.demos._06_conditional.DocOnCall;
import dev.devoxx.dashboard.demos._06_conditional.SniffOnCall;
import dev.devoxx.dashboard.demos._06_conditional.ZoomOnCall;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.scope.AgentInvocation;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorContextStrategy;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for <b>Mission 9</b> — Mission 6's four Rangers, and nobody wrote down who goes first.
 */
public final class SupervisorPattern {

    private SupervisorPattern() {
    }

    /** The bench: Mission 6's four Rangers on call, unchanged. */
    private static final List<String> RANGERS = List.of("Sniff", "Zoom", "Dig", "Doc");

    static String run(ChatModel model, String input, StreamingListener listener) {
        var sniff = AgenticServices.agentBuilder(SniffOnCall.class)
                .chatModel(model).name("Sniff").build();
        var zoom = AgenticServices.agentBuilder(ZoomOnCall.class)
                .chatModel(model).name("Zoom").build();
        var dig = AgenticServices.agentBuilder(DigOnCall.class)
                .chatModel(model).name("Dig").build();
        var doc = AgenticServices.agentBuilder(DocOnCall.class)
                .chatModel(model).name("Doc").build();

        SupervisorAgent zao = AgenticServices.supervisorBuilder()
                .subAgents(sniff, zoom, dig, doc)
                .chatModel(model)
                .supervisorContext("""
                        You are Zao, leader of the Pawer Rangers. The fair has several separate \
                        problems. Send ONE Ranger at a time, to ONE problem, with that problem \
                        as the call: Sniff finds the lost, Zoom catches anything running away, \
                        Dig deals with holes and tight spots, Doc deals with anyone hurt. Read \
                        each report before deciding who goes next. When every problem has had \
                        a Ranger, finish, and say in one sentence whether the fair is under \
                        control.""")
                .contextGenerationStrategy(SupervisorContextStrategy.CHAT_MEMORY)
                .maxAgentsInvocations(6)
                .output(SupervisorPattern::fairStatus)
                .listener(listener)
                .build();
        return String.valueOf(zao.invokeWithAgenticScope(input).result());
    }

    // ---- how the result is presented; the wiring above is the demo ----

    /** The route Zao chose, then each Ranger's report — the order is the thing to compare. */
    private static String fairStatus(AgenticScope scope) {
        var calls = scope.agentInvocations().stream()
                .filter(i -> RANGERS.contains(i.agentName()))
                .toList();
        if (calls.isEmpty()) {
            return "Zao sent nobody.";
        }
        String route = calls.stream().map(AgentInvocation::agentName).collect(joining(" → "));
        String reports = calls.stream()
                .map(i -> "- **" + i.agentName() + "** — " + String.valueOf(i.output()).strip())
                .collect(joining("\n"));
        return "**" + route + "**\n\n" + reports
                + "\n\n*Run it again: Zao may send them in a different order, because the "
                + "model decides — not the code.*";
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // Columns, not a wheel: a star of equal spokes is a picture of a fan-out, and this is a
        // conversation — Zao sends, reads, sends again. The mission arrives at Zao, never at a
        // Ranger, and every Ranger edge is two-way because he reads every report.
        Topology.Graph topo = graph("stages",
                List.of(node("in", "mission", "input", 0).withSub("three problems"),
                        node("zao", "Zao", "supervisor", 1).withSub("sends, reads, decides").as("zao"),
                        node("sniff", "Sniff", "agent", 2).withSub("if Zao sends him").as("sniff"),
                        node("zoom", "Zoom", "agent", 2).withSub("if Zao sends him").as("zoom"),
                        node("dig", "Dig", "agent", 2).withSub("if Zao sends him").as("dig"),
                        node("doc", "Doc", "agent", 2).withSub("if Zao sends him").as("doc"),
                        node("out", "fairStatus", "join", 3).withSub("when Zao says so")),
                List.of(edge("in", "zao"),
                        edge("zao", "sniff"), edge("sniff", "zao", "report"),
                        edge("zao", "zoom"), edge("zoom", "zao"),
                        edge("zao", "dig"), edge("dig", "zao"),
                        edge("zao", "doc"), edge("doc", "zao"),
                        edge("zao", "out", "under control")));
        return new PatternDef("supervisor", "Supervisor", "planner",
                "The Town Fair is chaos: a lost child, a runaway sausage cart, and a hole in the "
                        + "bouncy castle. Zao has four Rangers.",
                "Mission 6's four Rangers on call, unchanged. Routing sent one; Zao sends as "
                        + "many as it takes.",
                "An LLM supervisor decides which Ranger to send, reads the report, and decides "
                        + "again — until the fair is under control. Nobody wrote down the order: "
                        + "**run it twice and it can change**, because the model decides, not "
                        + "the code. A router gets one call and stops; a fan-out would send "
                        + "everyone at once. This is the first mission where the system, not "
                        + "you, picks the next pup.",
                "Non-deterministic, and the route printed at the top is honest about it: a weaker "
                        + "model may stop early or send two Rangers to one problem. Bound the "
                        + "invocations (`maxAgentsInvocations`). And ask the hard question first: "
                        + "if you can write the order down, that is a sequence, and it is cheaper "
                        + "and debuggable. Reach for this when you genuinely cannot.",
                topo,
                "Paws up, Rangers! The Town Fair is chaos: a lost child by the carousel, a "
                        + "runaway sausage cart rolling towards the duck pond, and a hole in the "
                        + "bouncy castle.",
                SupervisorPattern::run);
    }
}
