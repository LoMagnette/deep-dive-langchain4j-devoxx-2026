package dev.devoxx.dashboard.demos._14_debate;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;

import java.util.List;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.patterns.debate.ConvergenceStrategy;
import dev.langchain4j.agentic.patterns.debate.DebatePlanner;
import dev.langchain4j.agentic.scope.AgentInvocation;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for <b>Mission 14</b> — two debaters and a judge, run by LangChain4j's DebatePlanner.
 */
public final class DebatePattern {

    private DebatePattern() {
    }

    /** The council meets for three rounds, unless the two sides say exactly the same thing. */
    public static final int ROUNDS = 3;

    static String run(ChatModel model, String input, StreamingListener listener) {
        var r = CurrentRun.with(listener, () ->
                AgenticServices.createAgenticSystem(Debate.class, model).invoke(input));
        return "**Fifi's verdict**\n\n" + r.result() + "\n\n---\n\n" + transcript(r.agenticScope());
    }

    // ---- how the result is presented; the wiring above is the demo ----

    /**
     * The whole debate, round by round. DebatePlanner only keeps the LAST round on the board
     * (debateContext), so the full transcript is rebuilt from the scope's record of who said what.
     */
    private static String transcript(AgenticScope scope) {
        List<AgentInvocation> turns = scope.agentInvocations().stream()
                .filter(i -> List.of("Howl", "Marmalade").contains(i.agentName())).toList();
        StringBuilder out = new StringBuilder();
        int round = 0;
        for (int i = 0; i < turns.size(); i++) {
            if (i % 2 == 0) {
                out.append(out.isEmpty() ? "" : "\n\n").append("Round ").append(++round);
            }
            out.append("\n").append(turns.get(i).agentName()).append(": ")
                    .append(String.valueOf(turns.get(i).output()).strip());
        }
        return out.toString();
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // The debaters never talk to each other directly: each round, the planner collects what
        // both said and hands it back as debateContext. So the planner sits between them and
        // the arrows go through it — and the judge is reached only from the planner, after the
        // last round, because that is the only way the judge is ever called.
        Topology.Graph topo = graph("stages",
                List.of(node("in", "motion", "input", 0),
                        node("plan", "DebatePlanner", "planner", 1).withSub("3 rounds · unanimous()"),
                        node("howl", "Howl", "agent", 2).withSub("for the dog park").as("howl"),
                        node("marmalade", "Marmalade", "agent", 2).withSub("for the cat café").as("marmalade"),
                        node("fifi", "Fifi", "judge", 3).withSub("the LAST sub-agent").as("fifi")),
                List.of(edge("in", "plan"),
                        edge("plan", "howl"), edge("howl", "plan", "each round"),
                        edge("plan", "marmalade"), edge("marmalade", "plan"),
                        edge("plan", "fifi", "closing statements")));
        return new PatternDef("debate", "Debate", "minds",
                "The town council must decide: the empty lot on Elm Street becomes a dog park, "
                        + "or a cat café. Marmalade has prepared.",
                null,
                "Two agents argue opposing sides for N rounds; a judge rules. LangChain4j's "
                        + "`DebatePlanner` runs it: **every sub-agent but the last is a debater, "
                        + "and the last is the judge**. Each round it calls both debaters, then "
                        + "writes what they said into `debateContext`, which is what they answer "
                        + "next round — so each side answers the other's LAST round. It stops when "
                        + "the `ConvergenceStrategy` says the sides agree, or after `maxRounds`, and "
                        + "only then calls the judge. Ask one agent and it picks a side and "
                        + "rationalises it; a debate makes the case against the winner get said "
                        + "out loud first.",
                "The planner keeps only the last round on the board: `debateContext` is "
                        + "overwritten every round, so Fifi rules on the CLOSING statements, not the "
                        + "whole debate — the full transcript below the verdict is rebuilt for the "
                        + "room from the scope's invocations. `unanimous()` means word-for-word "
                        + "identical, which prose never is, so this always runs all three rounds "
                        + "(`unanimousLastWord()` converges when both END on the same word). And it "
                        + "is token-hungry: three rounds is six calls before anyone rules.",
                topo,
                "Barkville town council: should the empty lot on Elm Street become a dog park or a "
                        + "cat café? Howl speaks for the dog park, Marmalade for the cat café.",
                DebatePattern::run)
                .gist("Agents argue in rounds; a judge rules at the end.");
    }
}
