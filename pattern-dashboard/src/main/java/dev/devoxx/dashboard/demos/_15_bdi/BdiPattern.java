package dev.devoxx.dashboard.demos._15_bdi;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._15_bdi.Keys.Beliefs;
import dev.devoxx.dashboard.demos._15_bdi.Keys.Chased;
import dev.devoxx.dashboard.demos._15_bdi.Keys.Napped;
import dev.devoxx.dashboard.demos._15_bdi.Keys.Rescued;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.patterns.bdi.BDIPlanner;
import dev.langchain4j.agentic.patterns.bdi.Desire;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for <b>Mission 15</b> — three desires, ranked, and one dog who keeps his word.
 */
public final class BdiPattern {

    private BdiPattern() {
    }

    static String run(ChatModel model, String input, StreamingListener listener) {
        var rescue = AgenticServices.agentBuilder(ZoomRescuesTheKid.class)
                .chatModel(model).name("ZoomRescue").outputKey(Rescued.class).build();
        var squirrel = AgenticServices.agentBuilder(ZoomChasesTheSquirrel.class)
                .chatModel(model).name("ZoomSquirrel").outputKey(Chased.class).build();
        var nap = AgenticServices.agentBuilder(ZoomNaps.class)
                .chatModel(model).name("ZoomNap").outputKey(Napped.class).build();

        // Priorities, not declaration order: shuffle these three and Zoom behaves the same. The
        // squirrel ranks above the nap and below the kid — the whole of a Greyhound's character
        // in three numbers — and the kid's desire is only ACHIEVABLE while Zoom believes the
        // kid is still stranded. Change the belief and the intention is dropped.
        List<Desire> desires = List.of(
                Desire.of("rescue the kid", 100,
                        s -> believes(s, "stranded") && !believes(s, "already safe"),
                        s -> s.hasState(Rescued.class), ZoomRescuesTheKid.class),
                Desire.of("chase that squirrel", 50,
                        s -> believes(s, "squirrel"),
                        s -> s.hasState(Chased.class), ZoomChasesTheSquirrel.class),
                Desire.of("nap", 10,
                        s -> true,
                        s -> s.hasState(Napped.class), ZoomNaps.class));

        ZoomsHead app = AgenticServices.plannerBuilder(ZoomsHead.class)
                .subAgents(rescue, squirrel, nap)
                .planner(() -> new BDIPlanner(desires))
                .outputKey(Napped.class)
                .listener(listener)
                .build();
        var r = app.invoke(input);
        return intentions(r.agenticScope());
    }

    /**
     * A belief is a phrase on the radio, read in plain Java. Deciding what Zoom KNOWS is not a
     * judgement — it is what the radio said — and keeping it out of the model is what makes the
     * intention change predictably when the belief does.
     */
    static boolean believes(AgenticScope scope, String phrase) {
        String b = scope.readState(Beliefs.class);
        return b != null && b.toLowerCase(Locale.ROOT).contains(phrase);
    }

    // ---- how the result is presented ----

    private static final Map<String, String> DESIRE = Map.of(
            "ZoomRescue", "rescue the kid", "ZoomSquirrel", "chase that squirrel", "ZoomNap", "nap");

    private static String intentions(AgenticScope scope) {
        var acted = scope.agentInvocations().stream()
                .filter(i -> DESIRE.containsKey(i.agentName())).toList();
        String order = acted.stream().map(i -> DESIRE.get(i.agentName())).collect(joining(" → "));
        String what = acted.stream()
                .map(i -> "- **" + DESIRE.get(i.agentName()) + "** — " + String.valueOf(i.output()).strip())
                .collect(joining("\n"));
        boolean dropped = acted.stream().noneMatch(i -> "ZoomRescue".equals(i.agentName()));
        return "**Intentions, in the order Zoom committed to them: " + order + "**\n\n" + what
                + (dropped ? "\n\n*The rescue was dropped: Zoom no longer believes anyone is "
                        + "stranded, so that desire is not achievable — and the squirrel wins.*"
                        : "\n\n*The squirrel appeared before the rescue was done, and Zoom kept "
                        + "his intention: a stranded kid outranks a squirrel.*");
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        Topology.Graph topo = graph("dag",
                List.of(node("in", "beliefs", "input").withSub("from the radio"),
                        node("rescue", "ZoomRescue", "agent").withSub("desire · priority 100").as("zoom"),
                        node("squirrel", "ZoomSquirrel", "agent").withSub("desire · priority 50").as("zoom"),
                        node("nap", "ZoomNap", "agent").withSub("desire · priority 10").as("zoom")),
                // Gated on beliefs, not wired as an order: the edges are what each desire needs to
                // be achievable, and the one that skips a node is what makes this a DAG of
                // preconditions rather than a chain.
                List.of(edge("in", "rescue", "kid stranded"),
                        edge("rescue", "squirrel", "rescue done"),
                        edge("in", "squirrel", "squirrel seen"),
                        edge("squirrel", "nap", "nothing left"),
                        edge("rescue", "nap")));
        return new PatternDef("bdi", "BDI (Belief-Desire-Intention)", "minds",
                "The bridge is out and a kid is stranded on the far bank. Zoom is on his way. "
                        + "Then: a squirrel.",
                null,
                "Zoom's head in three parts. **Beliefs**: the bridge is out, a kid is stranded, "
                        + "a squirrel just appeared. **Desires**, ranked: rescue the kid (100), "
                        + "chase the squirrel (50), nap (10). **Intention**: the highest desire "
                        + "that is achievable and not yet met — so the squirrel changes nothing "
                        + "while the kid is stranded. Then send a second belief update, \"the kid "
                        + "is already safe\", and watch the rescue intention dropped.",
                "Powerful but fiddly: the achievable and satisfied predicates are hard to get "
                        + "right, and a desire that can never be satisfied stalls the whole plan. "
                        + "Here the beliefs are read in plain Java on purpose — if a model decided "
                        + "what Zoom believes, the intention would change for reasons nobody can "
                        + "point at.",
                topo,
                "Radio: the bridge over the river is out. A kid is stranded on the other side. "
                        + "Also — SQUIRREL — a squirrel has just appeared on the riverbank.",
                BdiPattern::run);
    }
}
