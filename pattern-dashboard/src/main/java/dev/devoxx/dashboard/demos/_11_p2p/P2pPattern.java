package dev.devoxx.dashboard.demos._11_p2p;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static dev.devoxx.dashboard.support.Parsing.agreed;
import static java.util.Objects.requireNonNullElse;

import java.util.List;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._11_p2p.Keys.Counter;
import dev.devoxx.dashboard.demos._11_p2p.Keys.Proposal;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
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
        var greyhound = AgenticServices.agentBuilder(WholeSofa.class)
                .chatModel(model)
                .name("WholeSofa")
                .outputKey(Proposal.class)
                .build();
        var labrador = AgenticServices.agentBuilder(CornerSeat.class)
                .chatModel(model)
                .name("CornerSeat")
                .outputKey(Counter.class)
                .build();
        Negotiation app = AgenticServices.plannerBuilder(Negotiation.class)
                .subAgents(greyhound, labrador)
                // The exit predicate is the only thing that ends this: neither side can
                // overrule the other, so without it they counter each other to the cap. Note
                // that it reads the CONTENT, and that EITHER key satisfies it — whichever peer
                // is the one to give way ends the argument.
                //
                // It used to be hasState(Agreement.class) against the second peer's own output
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
        var r = app.invoke(input, "(nothing on the table yet)");
        // Whichever dog gave way is the answer, and which one that is is not fixed.
        var scope = r.agenticScope();
        if (scope == null) {
            return String.valueOf(r.result());
        }
        String proposal = requireNonNullElse(scope.readState(Proposal.class), "");
        String counter = requireNonNullElse(scope.readState(Counter.class), "");
        String signed = agreed(proposal) ? proposal : agreed(counter) ? counter : "";
        return signed.isBlank()
                ? "Ten rounds and no deal either of them would keep:\n\n" + proposal
                : "**" + (agreed(proposal) ? "The Greyhound" : "The Labrador")
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
                        // The same second line on both, apart from the dog: neither box may
                        // read as the senior one.
                        node("greyhound", "WholeSofa", "agent", 1)
                                .withSub("Greyhound · equal say"),
                        node("labrador", "CornerSeat", "agent", 1)
                                .withSub("Labrador · equal say"),
                        node("out", "exitCondition", "join", 2)
                                .withSub("'AGREED' from either")),
                List.of(edge("in", "greyhound"), edge("in", "labrador"),
                        edge("greyhound", "labrador", "proposal"),
                        edge("labrador", "greyhound", "counter · ≤10 rounds"),
                        edge("greyhound", "out"),
                        edge("labrador", "out", "checked every turn")));
        return new PatternDef("p2p", "Peer-to-Peer", "pattern-zoo",
                "One sofa, two dogs. Zao is pack leader and has declined to rule on it, which "
                        + "is why this is not a supervisor.",
                null,
                "Peers refine a shared state until an exit condition holds. Each one reads what "
                        + "the other wrote and answers it — no coordinator, no order laid down "
                        + "in advance, and either of them can be the one to give way. The case "
                        + "for it: the reason this is not a supervisor is that nobody here can "
                        + "overrule anybody — the one dog who could has refused — so the only "
                        + "way out is a deal both will actually keep. Watch the roll-call: "
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
                "who gets the sofa? There is one sofa. The Greyhound is lying on all of it.",
                P2pPattern::run);
    }
}
