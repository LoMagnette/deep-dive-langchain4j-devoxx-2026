package dev.devoxx.dashboard;

import static dev.devoxx.dashboard.Topology.edge;
import static dev.devoxx.dashboard.Topology.graph;
import static dev.devoxx.dashboard.Topology.node;
import static dev.devoxx.dashboard.Wiring.agent;
import static dev.devoxx.dashboard.Wiring.result;
import static dev.devoxx.dashboard.Wiring.str;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import dev.devoxx.dashboard.PatternDef.Runner;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.patterns.bdi.BDIPlanner;
import dev.langchain4j.agentic.patterns.bdi.Desire;
import dev.langchain4j.agentic.patterns.blackboard.BlackboardPlanner;
import dev.langchain4j.agentic.patterns.blackboard.ConflictResolutionStrategy;
import dev.langchain4j.agentic.patterns.debate.ConvergenceStrategy;
import dev.langchain4j.agentic.patterns.debate.DebatePlanner;
import dev.langchain4j.agentic.patterns.goap.GoalOrientedPlanner;
import dev.langchain4j.agentic.patterns.p2p.P2PPlanner;
import dev.langchain4j.agentic.patterns.voting.VotingPlanner;
import dev.langchain4j.agentic.patterns.voting.VotingStrategy;
import dev.langchain4j.agentic.scope.AgenticScope;

/** The pattern zoo — planners from {@code langchain4j-agentic-patterns}, each with its own
 * idea of how agents should take turns. Experimental, and the most fun to watch. */
final class ZooPatterns {

    private ZooPatterns() {
    }

    /** In the order the rail and the gallery show them. */
    static List<PatternDef> all() {
        return List.of(goap(), p2p(), blackboard(), voting(), debate(), bdi());
    }

    // 8 — GOAP: the planner works out the order from the declared inputs and outputs
    private static PatternDef goap() {
        Topology.Graph topo = graph("dag",
                List.of(node("in", "request", "input"),
                        node("audit", "VaccinationAuditor", "agent"),
                        node("allocate", "RunAllocator", "agent"),
                        node("price", "QuotePricer", "agent")),
                List.of(edge("in", "audit"),
                        edge("audit", "allocate", "vaccination"),
                        edge("allocate", "price", "run")));
        Runner runner = (model, input, listener) -> {
            var audit = agent(Agents.VaccinationAuditor.class, model, "VaccinationAuditor",
                    "vaccination");
            var allocate = agent(Agents.RunAllocator.class, model, "RunAllocator", "run");
            var price = agent(Agents.QuotePricer.class, model, "QuotePricer", "quote");
            UntypedAgent app = AgenticServices.plannerBuilder()
                    // Registered BACKWARDS on purpose, and it still runs audit → allocate →
                    // price. That is the whole pattern: the order comes from the I/O keys
                    // (QuotePricer needs 'run', RunAllocator needs 'vaccination'), not from the
                    // order you happened to type. Say this out loud on stage — it is the one
                    // moment where GOAP is visibly not a sequence with extra steps.
                    .subAgents(price, allocate, audit)
                    .planner(GoalOrientedPlanner::new)
                    .outputKey("quote")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("request", input));
            return result(r, "quote");
        };
        return new PatternDef("goap", "GOAP (Goal-Oriented Planning)", "pattern-zoo",
                "The planner orders agents automatically by matching each output to the next "
                        + "input. A real precondition chain: you cannot price a stay before a run "
                        + "is allocated, and you cannot allocate one before the vaccination "
                        + "status is known — so there is genuinely an order to discover.",
                // caveat: planning is only as good as the declared pre/post-conditions (I/O keys).
                "Needs well-declared I/O keys; a missing link means the goal is unreachable — and "
                        + "the failure is silence, not an error.",
                topo,
                // The booster is deliberately well clear of the 21-day rule, so the chain runs to
                // a real number instead of stopping at "cannot be quoted".
                "Nero, 40kg German shepherd, seven nights from 12 October, needs half an "
                        + "antibiotic tablet twice a day, rabies booster done 2 September",
                runner);
    }

    // 9 — P2P: two peers with opposed mandates and no authority over each other
    private static PatternDef p2p() {
        Topology.Graph topo = graph("mesh",
                List.of(node("in", "issue", "input"),
                        node("foreman", "KennelForeman", "agent"),
                        node("welfare", "WelfareOfficer", "agent")),
                List.of(edge("in", "foreman"),
                        edge("foreman", "welfare", "proposal"),
                        edge("welfare", "foreman", "counter")));
        Runner runner = (model, input, listener) -> {
            var foreman = agent(Agents.KennelForeman.class, model, "KennelForeman", "plan");
            var welfare = agent(Agents.WelfareOfficer.class, model, "WelfareOfficer", "consensus");
            UntypedAgent app = AgenticServices.plannerBuilder()
                    .subAgents(foreman, welfare)
                    // The exit predicate is the only thing that ends this: neither peer can
                    // overrule the other, so without it they counter each other forever.
                    .planner(() -> new P2PPlanner(10, s -> s.hasState("consensus")))
                    .outputKey("consensus")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("issue", input));
            return result(r, "consensus");
        };
        return new PatternDef("p2p", "Peer-to-Peer", "pattern-zoo",
                "Peers iteratively refine a shared blackboard until an exit condition holds. The "
                        + "case for it: these two have genuinely opposed mandates — full runs "
                        + "versus welfare rules — and neither outranks the other, so there is no "
                        + "supervisor to hand it to.",
                // caveat: without a firm exit predicate peers can ping-pong indefinitely.
                "No fixed hierarchy — needs a solid exit predicate or it never terminates, and "
                        + "'they'll converge' is not one.",
                topo,
                "the bank holiday is overbooked: 14 dogs are booked into 11 runs, and three of "
                        + "them cannot be housed next to another male",
                runner);
    }

    // 10 — blackboard: three kinds of knowledge, contributed in any order
    private static PatternDef blackboard() {
        Topology.Graph topo = graph("star",
                // The only pattern that still draws the shared state: here it is not plumbing,
                // it is the pattern. Every other topology dropped its AgenticScope sink — it was
                // the same box in all 13 diagrams, and the scope now has its own tab.
                List.of(node("board", "Case board", "board"),
                        node("medical", "MedicalNotes", "agent"),
                        node("behaviour", "BehaviourNotes", "agent"),
                        node("feed", "FeedNotes", "agent"),
                        node("lead", "VetLead", "agent")),
                // Experts read the board as well as write to it — that mutual dependency is why
                // the pattern needs a conflict-resolution strategy at all.
                List.of(edge("medical", "board", "medical"), edge("board", "medical"),
                        edge("behaviour", "board", "behaviour"), edge("board", "behaviour"),
                        edge("feed", "board", "feed"), edge("board", "feed"),
                        edge("lead", "board", "differential"), edge("board", "lead")));
        Runner runner = (model, input, listener) -> {
            // The three note-takers read ONLY 'problem', so any of them can go first and the
            // board accumulates three different KINDS of knowledge. Chain them instead — each
            // reading the last one's output — and you have written a sequence wearing a
            // blackboard's coat, which is what this demo used to be.
            var medical = agent(Agents.MedicalNotes.class, model, "MedicalNotes", "medical");
            var behaviour = agent(Agents.BehaviourNotes.class, model, "BehaviourNotes", "behaviour");
            var feed = agent(Agents.FeedNotes.class, model, "FeedNotes", "feed");
            var lead = agent(Agents.VetLead.class, model, "VetLead", "differential");
            Predicate<AgenticScope> goal = s -> s.hasState("differential");
            UntypedAgent app = AgenticServices.plannerBuilder()
                    .subAgents(medical, behaviour, feed, lead)
                    .planner(() -> new BlackboardPlanner(goal,
                            ConflictResolutionStrategy.declarationOrder()))
                    .outputKey("differential")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("problem", input));
            return result(r, "differential");
        };
        return new PatternDef("blackboard", "Blackboard", "pattern-zoo",
                "Experts read and write a shared board, contributing until a goal state exists. "
                        + "Right when the answer needs several kinds of knowledge and you do not "
                        + "know which one cracks it: a dog off his food is a medical question, a "
                        + "behaviour question and a feeding question until the board says which.",
                // caveat: concurrent writers need a conflict-resolution strategy.
                "Shared mutable state invites conflicts; pick a conflict-resolution strategy. And "
                        + "be honest about whether your experts really are order-independent.",
                topo,
                "Nero has eaten nothing for two days. He is bright and his temperature is "
                        + "normal, he came off his usual food on Monday, and the dog in the next "
                        + "run barks most of the night",
                runner);
    }

    // 11 — voting: three assessors, different criteria, genuine disagreement
    private static PatternDef voting() {
        Topology.Graph topo = graph("fanout",
                List.of(node("in", "dossier", "input"),
                        node("temperament", "TemperamentAssessor", "agent"),
                        node("foster", "FosterAssessor", "agent"),
                        node("medical", "MedicalAssessor", "agent"),
                        // Without the tally this is just a fan-out; the tally IS the pattern.
                        node("vote", "majority()", "join")),
                List.of(edge("in", "temperament"), edge("in", "foster"), edge("in", "medical"),
                        edge("temperament", "vote", "PLACE / HOLD"),
                        edge("foster", "vote"), edge("medical", "vote")));
        Runner runner = (model, input, listener) -> {
            // Three DIFFERENT rubrics over the same dossier — the temperament test, the foster
            // diary, the medical file. Three copies of one prompt (what this demo used to be)
            // always agree, so the tally was decoration. Here they can split 2-1, which is the
            // only situation in which a majority means anything.
            // Each voter also writes its own key. The strategy does not need them — it tallies
            // what the agents returned — but the result pane does: "PLACE" on its own hides the
            // one thing worth seeing, which is whether the three of them actually split.
            var temperament = agent(Agents.TemperamentAssessor.class, model,
                    "TemperamentAssessor", "temperamentVote");
            var foster = agent(Agents.FosterAssessor.class, model, "FosterAssessor", "fosterVote");
            var medical = agent(Agents.MedicalAssessor.class, model, "MedicalAssessor",
                    "medicalVote");
            UntypedAgent app = AgenticServices.plannerBuilder()
                    .subAgents(temperament, foster, medical)
                    .planner(() -> new VotingPlanner(VotingStrategy.majority()))
                    .outputKey("decision")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("dossier", input));
            var scope = r.agenticScope();
            if (scope == null) {
                return result(r, "decision");
            }
            return "**Majority: " + str(scope, "decision") + "**\n\n"
                    + "- Temperament test: " + str(scope, "temperamentVote") + "\n"
                    + "- Foster diary: " + str(scope, "fosterVote") + "\n"
                    + "- Medical file: " + str(scope, "medicalVote");
        };
        return new PatternDef("voting", "Voting / Ensemble", "pattern-zoo",
                "Several agents answer independently; a strategy aggregates (majority, average, "
                        + "highest). Worth the tokens when one judgement is not trustworthy "
                        + "enough to act on — and this dossier contradicts itself, so the three "
                        + "assessors can legitimately disagree.",
                // caveat: correlated models vote alike, so an ensemble can be confidently wrong.
                "Correlated voters agree on the same mistake — diversity of evidence or rubric is "
                        + "what buys robustness, not running the same prompt three times. Note "
                        + "the price: each voter answers in ONE word, because a strategy can only "
                        + "tally answers that can be equal.",
                topo,
                // The contradiction is the point: a clean test, a bowl-guarding note, and pain
                // on the medical file. A single prompt would average this away silently.
                "Rescue 7 'Bruno', five-year-old mastiff cross, for a home with a three-year-old. "
                        + "Temperament test: passed all handling, no reactivity to children. "
                        + "Foster diary: growled twice when approached at his bowl, otherwise "
                        + "gentle with the foster's teenager. Medical file: chronic ear "
                        + "infection, on painkillers for it.",
                runner);
    }

    // 12 — debate: two homes, one dog, checkable constraints
    private static PatternDef debate() {
        Topology.Graph topo = graph("mesh",
                List.of(node("in", "motion", "input"),
                        node("one", "HomeOneAdvocate", "agent"),
                        node("two", "HomeTwoAdvocate", "agent"),
                        node("panel", "PlacementPanel", "judge")),
                List.of(edge("in", "one"), edge("in", "two"),
                        edge("one", "two", "rebut"), edge("two", "one", "rebut"),
                        edge("one", "panel"), edge("two", "panel")));
        Runner runner = (model, input, listener) -> {
            var one = agent(Agents.HomeOneAdvocate.class, model, "HomeOneAdvocate", null);
            var two = agent(Agents.HomeTwoAdvocate.class, model, "HomeTwoAdvocate", null);
            var panel = agent(Agents.PlacementPanel.class, model, "PlacementPanel", "verdict");
            UntypedAgent app = AgenticServices.plannerBuilder()
                    .subAgents(one, two, panel) // last sub-agent is the judge
                    .planner(() -> new DebatePlanner(2, ConvergenceStrategy.unanimous()))
                    .outputKey("verdict")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("motion", input));
            return result(r, "verdict");
        };
        return new PatternDef("debate", "Debate", "pattern-zoo",
                "Agents argue opposing sides for N rounds; a judge renders the verdict. The value "
                        + "is not the drama: a single prompt picks one home and then rationalises "
                        + "it, whereas a debate forces the case against the winner to be stated "
                        + "out loud before the panel rules.",
                // caveat: eloquence can beat correctness; more rounds cost more tokens.
                "The most persuasive agent may win over the most correct one — and it is "
                        + "token-hungry. Check the verdict against the constraints yourself; that "
                        + "is why this motion states them.",
                topo,
                // Both homes fail a different requirement, so the verdict is arguable but
                // checkable — the room has the same facts the panel does.
                "Bruno needs placing and both homes want him. Home One: large garden, but two "
                        + "cats and nobody in the house from nine to six. Home Two: first-floor "
                        + "flat with no garden, but someone home all day and two mastiffs raised "
                        + "before. Bruno guards his bowl and has never met a cat.",
                runner);
    }

    // 13 — BDI: priorities and preconditions decide the order, not the declaration order
    private static PatternDef bdi() {
        Topology.Graph topo = graph("dag",
                List.of(node("in", "shift", "input"),
                        node("safety", "SafetyRound (p30)", "agent"),
                        node("meds", "MedsRound (p20)", "agent"),
                        node("report", "ShiftReport (p5)", "agent")),
                // The report is gated on BOTH rounds, which is what makes this a DAG of desires
                // rather than a chain: 'needs' labels are preconditions, not hand-offs.
                List.of(edge("in", "safety"),
                        edge("safety", "meds", "needs round"),
                        edge("safety", "report", "needs round"),
                        edge("meds", "report", "needs meds")));
        Runner runner = (model, input, listener) -> {
            var safety = agent(Agents.SafetyRound.class, model, "SafetyRound", "round");
            var meds = agent(Agents.MedsRound.class, model, "MedsRound", "meds");
            var report = agent(Agents.ShiftReport.class, model, "ShiftReport", "report");
            // Priorities, not order. The safety round outranks everything; medication is only
            // achievable once every dog has been looked at; the report only once both are done.
            // Shuffle these three declarations and the behaviour does not change — which is the
            // point of BDI, and impossible to show with two agents in the only order they could
            // ever have run.
            List<Desire> desires = List.of(
                    Desire.of("safety-round", 30, s -> true, s -> s.hasState("round"),
                            Agents.SafetyRound.class),
                    Desire.of("meds-round", 20, s -> s.hasState("round"), s -> s.hasState("meds"),
                            Agents.MedsRound.class),
                    Desire.of("shift-report", 5,
                            s -> s.hasState("round") && s.hasState("meds"),
                            s -> s.hasState("report"), Agents.ShiftReport.class));
            UntypedAgent app = AgenticServices.plannerBuilder()
                    .subAgents(safety, meds, report)
                    .planner(() -> new BDIPlanner(desires))
                    .outputKey("report")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("shift", input));
            return result(r, "report");
        };
        return new PatternDef("bdi", "BDI (Belief-Desire-Intention)", "pattern-zoo",
                "The agent pursues prioritised desires, always acting on the highest-priority one "
                        + "that is achievable and not yet met. The morning shift: the safety "
                        + "round outranks the medication round, which outranks the paperwork — "
                        + "and that is declared as priority, not wired as an order.",
                // caveat: designing achievable/satisfied predicates is subtle and easy to get wrong.
                "Powerful but fiddly: the achievable and satisfied predicates are hard to get "
                        + "right, and a desire that is never satisfiable stalls the whole plan.",
                topo,
                "morning shift, nine dogs in: Nero in run 2 left his supper and was panting at "
                        + "03:00, Luna in run 5 has no stool overnight, the other seven are on "
                        + "their usual routine and four of them have medication due at 08:00",
                runner);
    }
}
