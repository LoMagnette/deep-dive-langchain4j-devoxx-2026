package dev.devoxx.dashboard.demos._12_blackboard;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._12_blackboard.Keys.CameraClue;
import dev.devoxx.dashboard.demos._12_blackboard.Keys.CrumbClue;
import dev.devoxx.dashboard.demos._12_blackboard.Keys.Culprit;
import dev.devoxx.dashboard.demos._12_blackboard.Keys.ScentClue;
import dev.devoxx.dashboard.demos._12_blackboard.Keys.TunnelClue;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.patterns.blackboard.BlackboardPlanner;
import dev.langchain4j.agentic.patterns.blackboard.ConflictResolutionStrategy;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for <b>Mission 12</b> — five Rangers, each able to act only once the board holds what
 * they need.
 */
public final class BlackboardPattern {

    private BlackboardPattern() {
    }

    static String run(ChatModel model, String input, StreamingListener listener) {
        // Each Ranger's @K parameters are its PRECONDITION: the planner will not pick an agent
        // until every pin it reads is on the board. Sniff and Bolt need only the crime; Dig needs
        // Sniff's scent; Doc needs Bolt's cameras; Zao needs Dig's prints and Doc's crumb.
        var sniff = AgenticServices.agentBuilder(SniffTrails.class)
                .chatModel(model)
                .name("Sniff")
                .outputKey(ScentClue.class)
                .build();
        var bolt = new BoltCameras();
        var dig = AgenticServices.agentBuilder(DigTunnels.class)
                .chatModel(model)
                .name("Dig")
                .outputKey(TunnelClue.class)
                .build();
        var doc = AgenticServices.agentBuilder(DocTestsTheCrumb.class)
                .chatModel(model)
                .name("Doc")
                .outputKey(CrumbClue.class)
                .build();
        var zao = AgenticServices.agentBuilder(ZaoNamesTheCulprit.class)
                .chatModel(model)
                .name("Zao")
                .outputKey(Culprit.class)
                .build();
        Predicate<AgenticScope> solved = s -> s.hasState(Culprit.class);
        Investigation app = AgenticServices.plannerBuilder(Investigation.class)
                // Registered BACKWARDS on purpose, Zao first. If this list were the order, Zao
                // would rule on an empty board. It is not: it is only the tie-break among the
                // Rangers who are able to act at that moment — that is what the conflict
                // resolution strategy is for.
                .subAgents(zao, doc, dig, bolt, sniff)
                .planner(() -> new BlackboardPlanner(solved,
                        ConflictResolutionStrategy.declarationOrder()))
                .outputKey(Culprit.class)
                .listener(listener)
                .build();
        var r = app.invoke(input);
        return ruling(r.agenticScope(), r.result());
    }

    // ---- how the result is presented ----

    /** The ruling, then the order the clues actually went up in — which nobody typed. */
    private static String ruling(AgenticScope scope, String culprit) {
        String order = scope.agentInvocations().stream()
                .map(i -> i.agentName())
                .filter(n -> List.of("Sniff", "Bolt", "Dig", "Doc", "Zao").contains(n))
                .collect(joining(" → "));
        return Objects.requireNonNullElse(culprit, "(no ruling)").strip()
                + "\n\n*Clues went up in this order: " + order
                + " — registered as Zao, Doc, Dig, Bolt, Sniff. The board decided.*";
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // The board sits in the MIDDLE column, with the Rangers either side of it, because every
        // arrow goes through it: nobody hands anything to anybody. Each box says what it needs on
        // the board before it can act, and each arrow back says what it pins. The columns are the
        // earliest a Ranger CAN act, not an order anyone wrote.
        Topology.Graph topo = graph("stages",
                List.of(node("in", "the crime", "input", 0),
                        node("sniff", "Sniff", "agent", 1).withSub("needs: the crime").as("sniff"),
                        node("bolt", "Bolt", "code", 1).withSub("needs: the crime").as("bolt"),
                        node("board", "Pup Board", "board", 2).withSub("every clue pinned here"),
                        node("dig", "Dig", "agent", 3).withSub("needs: the scent").as("dig"),
                        node("doc", "Doc", "agent", 3).withSub("needs: the cameras").as("doc"),
                        node("zao", "Zao", "agent", 4).withSub("needs: prints + crumb").as("zao"),
                        node("out", "culprit", "join", 5).withSub("the goal state")),
                List.of(edge("in", "board"),
                        edge("board", "sniff"), edge("sniff", "board", "the scent"),
                        edge("board", "bolt"), edge("bolt", "board", "cameras"),
                        edge("board", "dig"), edge("dig", "board", "paw prints"),
                        edge("board", "doc"), edge("doc", "board", "the crumb"),
                        edge("board", "zao", "prints + crumb"),
                        edge("zao", "out", "names the culprit")));
        return new PatternDef("blackboard", "Blackboard", "planner",
                "The town's sausages are gone from the butcher's. There is a sausage crumb in "
                        + "Zao's beard. Zao would like it known that this proves nothing.",
                null,
                "Specialists watch a shared board and contribute **whenever what they need is on "
                        + "it** — the AgenticScope is the Pup Board, and an agent's inputs are "
                        + "its precondition. Sniff and Bolt can start at once; Dig can only "
                        + "crawl a drain once Sniff has pinned which one; Doc can only test the "
                        + "crumb once Bolt's cameras are up; Zao rules once the prints and the "
                        + "crumb are both there. The order the clues go up in is not written "
                        + "anywhere — the Rangers are even registered backwards — it emerges "
                        + "from what is on the board, one contribution at a time.",
                "Shared state invites conflicts: when several Rangers can act at once, a "
                        + "`ConflictResolutionStrategy` picks which goes first (here, "
                        + "declaration order — and the declaration is backwards). And the goal "
                        + "predicate must be the CONTENT you want: stop on \"any clue exists\" "
                        + "and the case closes after one pin.",
                topo,
                "Paws up, Rangers! The Great Sausage Heist: every sausage in the butcher's "
                        + "window is gone, the door was locked, and a sausage crumb has been found "
                        + "in Zao's beard.",
                BlackboardPattern::run);
    }
}
