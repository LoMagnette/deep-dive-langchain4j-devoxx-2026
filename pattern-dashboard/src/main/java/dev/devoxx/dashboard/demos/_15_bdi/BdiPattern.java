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
import dev.devoxx.dashboard.demos._15_bdi.Keys.Crossing;
import dev.devoxx.dashboard.demos._15_bdi.Keys.Lookout;
import dev.devoxx.dashboard.demos._15_bdi.Keys.Napped;
import dev.devoxx.dashboard.demos._15_bdi.Keys.Rescued;
import dev.devoxx.dashboard.demos._15_bdi.Keys.Treed;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.patterns.bdi.BDIPlanner;
import dev.langchain4j.agentic.patterns.bdi.Desire;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for <b>Mission 15</b> — three ranked desires, each with a two-step plan, and one belief
 * that changes halfway through.
 */
public final class BdiPattern {

    private BdiPattern() {
    }

    static String run(ChatModel model, String input, StreamingListener listener) {
        var upTheBank = AgenticServices.agentBuilder(ZoomUpTheBank.class)
                .chatModel(model).name("ZoomUpTheBank").outputKey(Lookout.class).build();
        var treesIt = AgenticServices.agentBuilder(ZoomTreesTheSquirrel.class)
                .chatModel(model).name("ZoomTreesIt").outputKey(Treed.class).build();
        var toTheFord = AgenticServices.agentBuilder(ZoomToTheFord.class)
                .chatModel(model).name("ZoomToTheFord").outputKey(Crossing.class).build();
        var bringsKid = AgenticServices.agentBuilder(ZoomBringsTheKidBack.class)
                .chatModel(model).name("ZoomBringsKidBack").outputKey(Rescued.class).build();
        var nap = AgenticServices.agentBuilder(ZoomNaps.class)
                .chatModel(model).name("ZoomNaps").outputKey(Napped.class).build();

        List<Desire> desires = List.of(
                Desire.of("rescue the kid", 100,
                        s -> sees(s, "stranded"),          // only once he has SEEN the kid
                        s -> s.hasState(Rescued.class),
                        ZoomToTheFord.class, ZoomBringsTheKidBack.class),
                Desire.of("chase that squirrel", 50,
                        s -> told(s, "squirrel"),
                        s -> s.hasState(Treed.class),
                        ZoomUpTheBank.class, ZoomTreesTheSquirrel.class),
                Desire.of("nap", 10,
                        s -> true,
                        s -> s.hasState(Napped.class),
                        ZoomNaps.class));

        ZoomsHead app = AgenticServices.plannerBuilder(ZoomsHead.class)
                .subAgents(upTheBank, treesIt, toTheFord, bringsKid, nap)
                .planner(() -> new BDIPlanner(desires))
                .outputKey(Napped.class)
                .listener(listener)
                .build();
        var r = app.invoke(input);
        return intentions(r.agenticScope());
    }

    /**
     * Beliefs are read in plain Java: what the radio said, and what Zoom reported seeing. Deciding
     * what Zoom KNOWS is not a judgement, and keeping it out of the model is what makes the
     * intention change exactly when the belief does.
     */
    static boolean told(AgenticScope scope, String phrase) {
        return mentions(scope.readState(Beliefs.class), phrase);
    }

    static boolean sees(AgenticScope scope, String phrase) {
        return mentions(scope.readState(Lookout.class), phrase);
    }

    private static boolean mentions(String text, String phrase) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(phrase);
    }

    // ---- how the result is presented ----

    /** Which desire each step served, and which step of its plan it was. */
    private static final Map<String, String> STEP = Map.of(
            "ZoomUpTheBank", "squirrel 1/2", "ZoomTreesIt", "squirrel 2/2",
            "ZoomToTheFord", "rescue 1/2", "ZoomBringsKidBack", "rescue 2/2",
            "ZoomNaps", "nap 1/1");

    private static String intentions(AgenticScope scope) {
        var acted = scope.agentInvocations().stream()
                .filter(i -> STEP.containsKey(i.agentName())).toList();
        List<String> names = acted.stream().map(i -> i.agentName()).toList();
        String what = acted.stream()
                .map(i -> "- *" + STEP.get(i.agentName()) + "* — " + String.valueOf(i.output()).strip())
                .collect(joining("\n"));
        boolean preempted = names.indexOf("ZoomToTheFord") > names.indexOf("ZoomUpTheBank")
                && names.indexOf("ZoomToTheFord") < names.indexOf("ZoomTreesIt");
        return "**" + acted.stream().map(i -> STEP.get(i.agentName())).collect(joining(" → "))
                + "**\n\n" + what + "\n\n"
                + (preempted
                ? "*From the top of the bank Zoom saw a kid stranded — a new belief. That made "
                + "the rescue achievable, and it outranks the squirrel, so the chase was "
                + "preempted mid-plan. Once the kid was safe, the chase resumed at step 2, not "
                + "from the start.*"
                : "*Zoom never saw anyone stranded, so the rescue never became achievable: the "
                + "squirrel plan ran straight through.*");
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // One ROW per desire, its plan left to right, and the planner in front of all three —
        // the picture of "three plans, one commitment at a time". The edge that matters is the
        // one coming BACK from step 1 of the squirrel plan: that is the belief revision, and it is
        // what sends the planner to the rescue row halfway through.
        Topology.Graph topo = graph("stages",
                List.of(node("in", "beliefs", "input", 0).withSub("from the radio"),
                        node("bdi", "BDIPlanner", "planner", 1).withSub("highest achievable"),
                        node("ford", "ZoomToTheFord", "agent", 2).withSub("rescue · step 1").as("zoom"),
                        node("bank", "ZoomUpTheBank", "agent", 2).withSub("squirrel · step 1").as("zoom"),
                        node("nap", "ZoomNaps", "agent", 2).withSub("nap · step 1").as("zoom"),
                        node("kid", "ZoomBringsKidBack", "agent", 3).withSub("rescue · step 2").as("zoom"),
                        node("tree", "ZoomTreesIt", "agent", 3).withSub("squirrel · step 2").as("zoom")),
                List.of(edge("in", "bdi"),
                        edge("bdi", "ford", "100 · once kid seen"),
                        edge("bdi", "bank"),
                        edge("bank", "bdi", "50 · sees the kid!"),
                        edge("bdi", "nap", "10 · always"),
                        edge("ford", "kid"),
                        edge("bank", "tree", "resumed later")));
        return new PatternDef("bdi", "BDI (Belief-Desire-Intention)", "minds",
                "Zoom is chasing a squirrel up the riverbank. From the top he can see the bridge "
                        + "is out, and a kid on the far side. Squirrel. Kid. Squirrel.",
                null,
                "Zoom's head in three parts. **Beliefs**: what the radio said, and what he sees. "
                        + "**Desires**, ranked: rescue a kid (100), chase the squirrel (50), nap "
                        + "(10), each with a two-step plan. **Intention**: the plan of the highest "
                        + "desire that is achievable right now. Watch it happen: the squirrel "
                        + "plan starts, step 1 reveals a stranded kid, the rescue **preempts** "
                        + "the chase mid-plan, and once the kid is safe the chase **resumes at "
                        + "step 2**. Then, at last, the nap.",
                "Powerful but fiddly: the achievable and satisfied predicates are hard to get "
                        + "right, and a plan that finishes without satisfying its desire is an "
                        + "error, not a retry. Beliefs are read in plain Java here on purpose — if "
                        + "a model decided what Zoom believes, the intention would change for "
                        + "reasons nobody can point at. Try the radio saying \"Officer Jo already "
                        + "has the kid, safe\": the rescue never becomes achievable.",
                topo,
                "Radio: a squirrel has just run off towards the riverbank. Also, the old bridge "
                        + "over the river came down in the storm last night.",
                BdiPattern::run)
                .gist("Ranked desires with plans; a new belief can preempt one.");
    }
}
