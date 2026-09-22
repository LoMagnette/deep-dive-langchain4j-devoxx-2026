package dev.devoxx.dashboard.demos._11_p2p;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static dev.devoxx.dashboard.support.Parsing.agreed;
import static java.util.Objects.requireNonNullElse;

import java.util.List;
import java.util.Map;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._11_p2p.Keys.Counter;
import dev.devoxx.dashboard.demos._11_p2p.Keys.Proposal;
import dev.devoxx.dashboard.demos._11_p2p.Keys.Question;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.patterns.p2p.P2PPlanner;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for the <b>peer-to-peer</b> demo — two peers who cannot overrule each other.
 */
public final class P2pPattern {

    private P2pPattern() {
    }

    /** The wiring. Everything below it is the dashboard telling itself how to draw this. */
    static String run(ChatModel model, String input, StreamingListener listener) {
        var bed = AgenticServices.agentBuilder(TeamOnTheBed.class)
                .chatModel(model)
                .name("TeamOnTheBed")
                .outputKey(Proposal.class)
                .build();
        var floor = AgenticServices.agentBuilder(TeamOnTheFloor.class)
                .chatModel(model)
                .name("TeamOnTheFloor")
                .outputKey(Counter.class)
                .build();
        UntypedAgent app = AgenticServices.plannerBuilder()
                .subAgents(bed, floor)
                // The exit predicate is the only thing that ends this: neither side can
                // overrule the other, so without it they counter each other to the cap. Note
                // that it reads the CONTENT, and that EITHER key satisfies it — whichever peer
                // is the one to give way ends the argument.
                //
                // It used to be hasState(Agreement.class) against the floor peer's own output
                // key, which is true the moment that peer has run, on any model. The run always
                // stopped after exactly one exchange, and this was a two-step sequence with a
                // planner bolted on top. A predicate that cannot be false is not an exit
                // condition, and it is worth checking for that as carefully as for one that
                // never fires.
                .planner(() -> new P2PPlanner(10,
                        s -> agreed(s.readState(Proposal.class))
                                || agreed(s.readState(Counter.class))))
                .outputKey(Proposal.class)
                .listener(listener)
                .build();
        // Seeding an empty Counter is load-bearing: P2PPlanner activates an agent only once
        // every input it declares is present, so with neither key set neither peer can take a
        // turn and the run ends "stable after 0 invocations" — no agents, no error, no result.
        var r = app.invokeWithAgenticScope(Map.of(
                new Question().name(), input,
                new Counter().name(), "(nothing on the table yet)"));
        // Whichever half gave way is the answer, and which one that is is not fixed.
        var scope = r.agenticScope();
        if (scope == null) {
            return String.valueOf(r.result());
        }
        String proposal = requireNonNullElse(scope.readState(Proposal.class), "");
        String counter = requireNonNullElse(scope.readState(Counter.class), "");
        String signed = agreed(proposal) ? proposal : agreed(counter) ? counter : "";
        return signed.isBlank()
                ? "Ten rounds and no rule either of them would keep:\n\n" + proposal
                : "**" + (agreed(proposal) ? "The bed half" : "The floor half")
                        + " gave way**\n\n" + signed;
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // Two things this diagram has to say that the old one did not. The question reaches
        // BOTH peers, because a single arrow into the first one made it look like the peer in
        // charge — which is the one claim this pattern exists to deny. And the exit is a
        // predicate over what either of them wrote, so both feed it; drawn from one peer only,
        // that peer is the one who gets to end the argument.
        Topology.Graph topo = graph("stages",
                List.of(node("in", "question", "input", 0).withSub("nobody chairs this"),
                        node("bed", "TeamOnTheBed", "agent", 1)
                                .withSub("may sign, may counter"),
                        node("floor", "TeamOnTheFloor", "agent", 1)
                                .withSub("may sign, may counter"),
                        node("out", "exitCondition", "join", 2)
                                .withSub("'AGREED' from either")),
                List.of(edge("in", "bed"), edge("in", "floor"),
                        edge("bed", "floor", "proposal"),
                        edge("floor", "bed", "counter · ≤10 rounds"),
                        edge("bed", "out"),
                        edge("floor", "out", "checked every turn")));
        return new PatternDef("p2p", "Peer-to-Peer", "pattern-zoo",
                "And the argument you have been avoiding for a year. The bed. Neither of you "
                        + "outranks the other, which is why this is not a supervisor.",
                null,
                "Peers refine a shared state until an exit condition holds. Each one reads what "
                        + "the other wrote and answers it — no coordinator, no order laid down "
                        + "in advance, and either of them can be the one to give way. The case "
                        + "for it: every household has had this argument, and the reason it is "
                        + "not a supervisor is that neither half can overrule the other, so the "
                        + "only way out is a rule both will actually keep. Watch the roll-call: "
                        + "propose, counter, and then somebody signs.",
                // caveat: without a firm exit predicate peers can ping-pong indefinitely.
                "No hierarchy — needs a solid exit predicate or it never terminates, and "
                        + "\"they'll converge eventually\" is not one. Check the opposite "
                        + "failure just as hard: **a predicate that cannot be false is not an "
                        + "exit condition either.** This one asked whether a key *existed*, and "
                        + "the key was one of the peers' own output keys — so it was true the "
                        + "moment that peer had spoken, on any model, and a negotiation that "
                        + "looked fine on stage could never reach a second round. Read the "
                        + "content, not the presence. And `P2PPlanner` is **reactive**: an agent "
                        + "re-fires when an input changes, so two peers writing one shared key "
                        + "trigger each other and themselves, and race.",
                topo,
                "should Zao be allowed to sleep on the bed? He is asleep on the bed.",
                P2pPattern::run);
    }
}
