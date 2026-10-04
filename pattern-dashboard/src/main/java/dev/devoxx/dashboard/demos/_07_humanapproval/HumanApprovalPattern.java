package dev.devoxx.dashboard.demos._07_humanapproval;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static java.util.Objects.requireNonNullElse;

import java.nio.file.Path;
import java.util.List;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._02_sequential.Keys.RescueStatus;
import dev.devoxx.dashboard.demos._07_humanapproval.Keys.Approved;
import dev.devoxx.dashboard.demos._07_humanapproval.Keys.DigPlan;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.observability.AgentMonitor;
import dev.langchain4j.agentic.observability.HtmlReportGenerator;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for <b>Mission 7</b> — Dig plans, Officer Jo decides, Dig acts.
 */
public final class HumanApprovalPattern {

    private HumanApprovalPattern() {
    }

    static String run(ChatModel model, String input, StreamingListener listener) {
        var monitor = new AgentMonitor();
        var r = CurrentRun.with(listener, monitor, () ->
                AgenticServices.createAgenticSystem(RoseRescue.class, model).rescue(input));

        HtmlReportGenerator.generateReport(monitor, Path.of("human-in-the-loop.html"));

        // Show what was planned and what Jo said, not only the outcome: the whole point of the
        // pattern is the gap between those two.
        AgenticScope scope = r.agenticScope();
        if (scope == null) {
            return String.valueOf(r.result());
        }
        return "**Dig's plan**\n\n" + requireNonNullElse(scope.readState(DigPlan.class), "")
                + "\n\n**Officer Jo said**\n\n" + requireNonNullElse(scope.readState(Approved.class), "")
                + "\n\n**So Dig…**\n\n" + requireNonNullElse(scope.readState(RescueStatus.class), "");
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        Topology.Graph topo = graph("stages",
                // Officer Jo has role "human", not "agent", and that is the whole diagram: drawn
                // as another agent box she would say the model decided, which is the one thing
                // this pattern exists to deny.
                List.of(node("in", "mission", "input", 0),
                        node("plan", "Dig", "agent", 1).withSub("plans the tunnel").as("dig"),
                        node("jo", "OfficerJo", "human", 2).withSub("yes · no · yes-but").as("jo"),
                        node("act", "DigActs", "agent", 3).withSub("digs, or finds a way").as("dig"),
                        node("out", "rescueStatus", "join", 4)),
                List.of(edge("in", "plan"),
                        edge("plan", "jo", "digPlan"),
                        edge("jo", "act", "approved"),
                        edge("act", "out")));
        return new PatternDef("humanApproval", "Human in the Loop", "workflow",
                "A hedgehog is trapped under the Mayor's prize roses. Dig is delighted. Officer "
                        + "Jo would like a word first.",
                null,
                "A sequence with a person in it: Dig plans, `HumanInTheLoop` asks Officer Jo, "
                        + "Dig acts on her answer. `HumanInTheLoop` is a non-AI agent — it reads "
                        + "`digPlan` from the scope and writes `approved` back — so **the sequence "
                        + "cannot tell the answer came from a browser**. On stage, say no once: "
                        + "Dig does not dig, and suggests another way.",
                "The brake on the dial, and it costs what brakes cost: the run blocks on a "
                        + "person, so it needs a timeout and a thread you can afford to park. Ask "
                        + "too often and it is a form nobody reads; ask too rarely and the "
                        + "approval is a rubber stamp. Put it where the action is hard to undo — "
                        + "a rose bed is.",
                topo,
                "Paws up, Rangers! A hedgehog is trapped under the Mayor's prize roses, the ones "
                        + "that won Best in Show. The Mayor is at the flower show until four.",
                HumanApprovalPattern::run)
                .gist("The run stops and waits for a person to decide.");
    }
}
