package dev.devoxx.dashboard.demos._03_loop;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static dev.devoxx.dashboard.support.Parsing.reviewScore;

import java.util.List;
import java.util.Locale;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._03_loop.Keys.Draft;
import dev.devoxx.dashboard.demos._03_loop.Keys.Feedback;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.scope.AgentInvocation;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for <b>Mission 3</b> — a writer, a critic, and a bag of five treats.
 */
public final class LoopPattern {

    private LoopPattern() {
    }

    /** One treat per rewrite. When the bag is empty the loop stops, good poster or not. */
    public static final int TREATS = 5;

    /** Four rules nobody has to be persuaded of, so the room can score the poster too. */
    public static final String POSTER_RULES =
            "1. it gives the date from the brief; 2. it gives the place from the brief; "
                    + "3. it says entry is free; 4. it is under 40 words";

    static String run(ChatModel model, String input, StreamingListener listener) {
        var howl = AgenticServices.agentBuilder(HowlWrites.class)
                .chatModel(model)
                .name("Howl")
                .outputKey("Draft")
                .build();
        var fifi = AgenticServices.agentBuilder(FifiScores.class)
                .chatModel(model)
                .name("Fifi")
                .outputKey("Feedback")
                .build();

        // TODO live: loopBuilder
        return "TODO live: loopBuilder";
    }

    // ---- how the result is presented; the wiring above is the demo ----

    /** Each draft with its score, as the spec asks: the loop is only visible if every pass is. */
    static String everyPass(AgenticScope scope, String last) {
        if (scope == null) {
            return last;
        }
        List<AgentInvocation> calls = scope.agentInvocations();
        StringBuilder out = new StringBuilder();
        int pass = 0;
        String draft = null;
        for (AgentInvocation call : calls) {
            if ("Howl".equals(call.agentName())) {
                draft = String.valueOf(call.output());
            } else if ("Fifi".equals(call.agentName()) && draft != null) {
                pass++;
                String review = String.valueOf(call.output());
                out.append("**Pass ").append(pass).append(" · score ")
                        .append(String.format(Locale.US, "%.2f", reviewScore(review)))
                        .append("**\n\n").append(draft).append("\n\n*Fifi: ")
                        .append(review.replaceAll("\\s+", " ").strip()).append("*\n\n");
            }
        }
        out.append("---\n\n*").append(pass).append(" of ").append(TREATS)
                .append(" treats used.*");
        return out.toString();
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        Topology.Graph topo = graph("loop",
                // Both ways out of the critic: the arc back AND the exit, which is what ends a
                // loop. The exit has two conditions and both are on the page.
                List.of(node("in", "the brief", "input"),
                        node("howl", "Howl", "agent").withSub("writes · rewrites").as("howl"),
                        node("fifi", "Fifi", "agent").withSub("scores 4 rules").as("fifi"),
                        node("out", "the poster", "join").withSub("or after 5 treats")),
                List.of(edge("in", "howl"), edge("howl", "fifi", "draft"),
                        edge("fifi", "howl", "score < 0.8 · feedback"),
                        edge("fifi", "out", "score ≥ 0.8")));
        return new PatternDef("loop", "Loop / Iterative Refinement", "workflow",
                "The Town Fair is Saturday. Howl has written a poster. It is very loud, and it "
                        + "does not say when the fair is.",
                null,
                "Refine until a quality bar is met. Fifi scores against four rules the room can "
                        + "check for itself — the date, the place, free entry, under 40 words — "
                        + "so the score is a fraction of rules that hold and every pass shows "
                        + "which one it fixed. Howl reads her feedback on the next pass; the "
                        + "result prints every draft with its score.",
                "Can spin for ever or oscillate — so cap it. `maxIterations(5)` is Howl's bag "
                        + "of treats: when it is empty the loop stops, good poster or not. And a "
                        + "critic scoring \"quality\" out of 1.0 gives a number nobody in the "
                        + "room can check; score against named rules instead.",
                topo,
                "Poster for the Barkville Town Fair: Saturday 12 October, on Barkville Green, "
                        + "10:00 to 16:00. Free entry. Sausage stall, and the dog show at 14:00.",
                LoopPattern::run)
                .gist("Repeat a step until a critic's score clears the bar.");
    }
}
