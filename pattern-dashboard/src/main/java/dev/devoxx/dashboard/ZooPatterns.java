package dev.devoxx.dashboard;

import static dev.devoxx.dashboard.Topology.edge;
import static dev.devoxx.dashboard.Topology.graph;
import static dev.devoxx.dashboard.Topology.node;
import static dev.devoxx.dashboard.Wiring.agent;
import static dev.devoxx.dashboard.Wiring.result;
import static dev.devoxx.dashboard.Wiring.str;

import java.util.List;
import java.util.Locale;
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
import dev.langchain4j.agentic.scope.AgentInvocation;
import dev.langchain4j.agentic.scope.AgenticScope;

/** The pattern zoo — planners from {@code langchain4j-agentic-patterns}, each with its own
 * idea of how agents should take turns. Experimental, and the most fun to watch. */
final class ZooPatterns {

    private ZooPatterns() {
    }

    /** In the order the rail and the gallery show them. Custom last: you meet the planners the
     * library ships with, and then the interface all six of them implement. */
    static List<PatternDef> all() {
        return List.of(goap(), p2p(), blackboard(), voting(), debate(), bdi(), customPlanner());
    }

    /** The escalation ladder, cheapest first — the same order the planner is handed. */
    private static final List<String> LADDER =
            List.of("PuppyBook", "TrainerOnCall", "VetOnCall");

    /** The household the three assessors and the council both judge. */
    static final String HOUSEHOLD =
            "two-bedroom flat, no garden, both of us out from eight until six, we can afford a "
                    + "second one comfortably. Zao is four and he stiffens up and growls when "
                    + "another dog comes at him in the park.";

    // 8 — GOAP: the planner works out the order from the declared inputs and outputs
    private static PatternDef goap() {
        Topology.Graph topo = graph("dag",
                List.of(node("in", "goal", "input"),
                        node("indoor", "IndoorRecall", "agent"),
                        node("garden", "GardenRecall", "agent"),
                        node("park", "ParkRecall", "agent")),
                List.of(edge("in", "indoor"),
                        edge("indoor", "garden", "indoor"),
                        edge("garden", "park", "garden")));
        Runner runner = (model, input, listener) -> {
            var indoor = agent(Agents.IndoorRecall.class, model, "IndoorRecall", "indoor");
            var garden = agent(Agents.GardenRecall.class, model, "GardenRecall", "garden");
            var park = agent(Agents.ParkRecall.class, model, "ParkRecall", "park");
            UntypedAgent app = AgenticServices.plannerBuilder()
                    // Registered BACKWARDS on purpose, and it still runs indoor → garden → park.
                    // That is the whole pattern: the order comes from the I/O keys (ParkRecall
                    // needs 'garden', GardenRecall needs 'indoor'), not from the order you
                    // happened to type. Say this out loud on stage — it is the one moment where
                    // GOAP is visibly not a sequence with extra ceremony.
                    .subAgents(park, garden, indoor)
                    .planner(GoalOrientedPlanner::new)
                    .outputKey("park")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("goal", input));
            return "**Indoors** — " + str(r.agenticScope(), "indoor")
                    + "\n\n**Garden** — " + str(r.agenticScope(), "garden")
                    + "\n\n**Park** — " + str(r.agenticScope(), "park");
        };
        return new PatternDef("goap", "GOAP (Goal-Oriented Planning)", "pattern-zoo",
                "The planner orders agents automatically by matching each output to the next "
                        + "input. Nobody has to be told this order: recall works indoors before "
                        + "it works in the garden, and in the garden before it works at the park. "
                        + "So there is genuinely an order to discover, and you can see it was "
                        + "discovered rather than typed.",
                // caveat: planning is only as good as the declared pre/post-conditions (I/O keys).
                "Needs well-declared I/O keys; a missing link means the goal is unreachable — and "
                        + "the failure is silence, not an error.",
                topo,
                "teach Zao to come back when he's called, even at the park with other dogs about",
                runner);
    }

    // 9 — P2P: two peers who cannot overrule each other
    private static PatternDef p2p() {
        Topology.Graph topo = graph("mesh",
                List.of(node("in", "question", "input"),
                        node("bed", "TeamOnTheBed", "agent"),
                        node("floor", "TeamOnTheFloor", "agent")),
                List.of(edge("in", "bed"),
                        edge("bed", "floor", "proposal"),
                        edge("floor", "bed", "counter")));
        Runner runner = (model, input, listener) -> {
            var bed = agent(Agents.TeamOnTheBed.class, model, "TeamOnTheBed", "proposal");
            var floor = agent(Agents.TeamOnTheFloor.class, model, "TeamOnTheFloor", "agreement");
            UntypedAgent app = AgenticServices.plannerBuilder()
                    .subAgents(bed, floor)
                    // The exit predicate is the only thing that ends this: neither side can
                    // overrule the other, so without it they counter each other forever.
                    .planner(() -> new P2PPlanner(10, s -> s.hasState("agreement")))
                    .outputKey("agreement")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("question", input));
            return result(r, "agreement");
        };
        return new PatternDef("p2p", "Peer-to-Peer", "pattern-zoo",
                "Peers refine a shared state until an exit condition holds. The case for it: "
                        + "every household has had this argument, and the reason it is not a "
                        + "supervisor is that neither half can overrule the other — so the only "
                        + "way out is a rule both will actually keep.",
                // caveat: without a firm exit predicate peers can ping-pong indefinitely.
                "No hierarchy — needs a solid exit predicate or it never terminates, and "
                        + "\"they'll converge eventually\" is not one.",
                topo,
                "should Zao be allowed to sleep on the bed?",
                runner);
    }

    // 10 — blackboard: three kinds of knowledge, contributed in any order
    private static PatternDef blackboard() {
        Topology.Graph topo = graph("star",
                // The only pattern that still draws the shared state: here it is not plumbing,
                // it is the pattern. Every other topology dropped its AgenticScope sink — it was
                // the same box in all 13 diagrams, and the scope now has its own tab.
                List.of(node("board", "The board", "board"),
                        node("walks", "WalkNotes", "agent"),
                        node("routine", "RoutineNotes", "agent"),
                        node("home", "HomeNotes", "agent"),
                        node("lead", "TrainerLead", "agent")),
                // Contributors read the board as well as write to it — that mutual dependency is
                // why the pattern needs a conflict-resolution strategy at all.
                List.of(edge("walks", "board", "exercise"), edge("board", "walks"),
                        edge("routine", "board", "changes"), edge("board", "routine"),
                        edge("home", "board", "the house"), edge("board", "home"),
                        edge("lead", "board", "ranked causes"), edge("board", "lead")));
        Runner runner = (model, input, listener) -> {
            // The three note-takers read ONLY 'problem', so any of them can go first and the
            // board accumulates three different KINDS of knowledge. Chain them instead — each
            // reading the last one's output — and you have written a sequence wearing a
            // blackboard's coat, which is what this demo used to be.
            var walks = agent(Agents.WalkNotes.class, model, "WalkNotes", "walks");
            var routine = agent(Agents.RoutineNotes.class, model, "RoutineNotes", "routine");
            var home = agent(Agents.HomeNotes.class, model, "HomeNotes", "home");
            var lead = agent(Agents.TrainerLead.class, model, "TrainerLead", "causes");
            Predicate<AgenticScope> goal = s -> s.hasState("causes");
            UntypedAgent app = AgenticServices.plannerBuilder()
                    .subAgents(walks, routine, home, lead)
                    .planner(() -> new BlackboardPlanner(goal,
                            ConflictResolutionStrategy.declarationOrder()))
                    .outputKey("causes")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("problem", input));
            return result(r, "causes");
        };
        return new PatternDef("blackboard", "Blackboard", "pattern-zoo",
                "Contributors read and write a shared board until a goal state exists. This is "
                        + "debugging, which is what a blackboard is for: barking while you are "
                        + "out is an exercise question, a what-changed question and a "
                        + "what-can-he-see question until the board says which one it is.",
                // caveat: concurrent writers need a conflict-resolution strategy.
                "Shared mutable state invites conflicts; pick a conflict-resolution strategy. And "
                        + "be honest about whether your contributors really are order-independent.",
                topo,
                "he's started barking all day while we're at work and the neighbour has "
                        + "complained twice. He never used to. Nothing has changed except my new "
                        + "shift and we moved his bed under the front window.",
                runner);
    }

    // 11 — voting: three criteria that can genuinely disagree
    private static PatternDef voting() {
        Topology.Graph topo = graph("fanout",
                List.of(node("in", "household", "input"),
                        node("space", "SpaceAndTime", "agent"),
                        node("money", "MoneyAndVet", "agent"),
                        node("zao", "AskZaoHimself", "agent"),
                        // Without the tally this is just a fan-out; the tally IS the pattern.
                        node("vote", "majority()", "join")),
                List.of(edge("in", "space"), edge("in", "money"), edge("in", "zao"),
                        edge("space", "vote", "YES / LATER"),
                        edge("money", "vote"), edge("zao", "vote")));
        Runner runner = (model, input, listener) -> {
            // Three DIFFERENT criteria over the same household — space and hours, money, and
            // what the dog you already have would say. Three copies of one prompt (what this
            // demo used to be) always agree, so the tally was decoration. Here money says yes
            // while the other two say later, which is the only situation where a majority means
            // anything.
            //
            // Each voter also writes its own key. The strategy does not need them — it tallies
            // what the agents returned — but the result pane does: one word on its own hides the
            // only interesting thing, which is whether they split.
            var space = agent(Agents.SpaceAndTime.class, model, "SpaceAndTime", "spaceVote");
            var money = agent(Agents.MoneyAndVet.class, model, "MoneyAndVet", "moneyVote");
            var zao = agent(Agents.AskZaoHimself.class, model, "AskZaoHimself", "zaoVote");
            UntypedAgent app = AgenticServices.plannerBuilder()
                    .subAgents(space, money, zao)
                    .planner(() -> new VotingPlanner(VotingStrategy.majority()))
                    .outputKey("decision")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("household", input));
            var scope = r.agenticScope();
            if (scope == null) {
                return result(r, "decision");
            }
            return "**Majority: " + str(scope, "decision") + "**\n\n"
                    + "- Space and hours alone: " + str(scope, "spaceVote") + "\n"
                    + "- Money: " + str(scope, "moneyVote") + "\n"
                    + "- Zao himself: " + str(scope, "zaoVote");
        };
        return new PatternDef("voting", "Voting / Ensemble", "pattern-zoo",
                "Several agents answer independently; a strategy aggregates (majority, average, "
                        + "highest). Worth the tokens when one judgement is not trustworthy "
                        + "enough to act on — and this household is a genuine split, because the "
                        + "money is fine and everything else is not.",
                // caveat: correlated models vote alike, so an ensemble can be confidently wrong.
                "Correlated voters agree on the same mistake — diversity of criteria is what "
                        + "buys robustness, not running the same prompt three times. Note the "
                        + "price: each voter answers in ONE word, because a strategy can only "
                        + "tally answers that can be equal.",
                topo, HOUSEHOLD, runner);
    }

    // 12 — debate: two strong cases, and a ruling the room can check
    private static PatternDef debate() {
        Topology.Graph topo = graph("mesh",
                List.of(node("in", "motion", "input"),
                        node("take", "TakeHimAdvocate", "agent"),
                        node("leave", "LeaveHimAdvocate", "agent"),
                        node("verdict", "HolidayVerdict", "judge")),
                List.of(edge("in", "take"), edge("in", "leave"),
                        edge("take", "leave", "rebut"), edge("leave", "take", "rebut"),
                        edge("take", "verdict"), edge("leave", "verdict")));
        Runner runner = (model, input, listener) -> {
            var take = agent(Agents.TakeHimAdvocate.class, model, "TakeHimAdvocate", null);
            var leave = agent(Agents.LeaveHimAdvocate.class, model, "LeaveHimAdvocate", null);
            var verdict = agent(Agents.HolidayVerdict.class, model, "HolidayVerdict", "verdict");
            UntypedAgent app = AgenticServices.plannerBuilder()
                    .subAgents(take, leave, verdict) // last sub-agent is the judge
                    .planner(() -> new DebatePlanner(2, ConvergenceStrategy.unanimous()))
                    .outputKey("verdict")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("motion", input));
            return result(r, "verdict");
        };
        return new PatternDef("debate", "Debate", "pattern-zoo",
                "Agents argue opposing sides for N rounds; a judge rules. The value is not the "
                        + "drama: ask one agent and it picks a side and then rationalises it, "
                        + "whereas a debate forces the case against the winner to be said out "
                        + "loud first. Both sides here are genuinely strong, which is the only "
                        + "time it is worth the tokens.",
                // caveat: eloquence can beat correctness; more rounds cost more tokens.
                "The most persuasive agent may win over the most correct one — and it is "
                        + "token-hungry. Check the ruling against the facts yourself; that is why "
                        + "the motion states them.",
                topo,
                "two weeks in Tuscany in August: take Zao, or leave him with a sitter? It is a "
                        + "twelve-hour drive, the house has no shade, and he has never been left "
                        + "for more than two nights.",
                runner);
    }

    // 13 — BDI: priorities and preconditions decide the order, not the declaration order
    private static PatternDef bdi() {
        Topology.Graph topo = graph("dag",
                List.of(node("in", "first hour", "input"),
                        node("out", "ToiletTrip (p30)", "agent"),
                        node("fed", "FirstMeal (p20)", "agent"),
                        node("train", "FirstTraining (p5)", "agent")),
                // The training session is gated on BOTH of the others, which is what makes this a
                // DAG of desires rather than a chain: 'needs' labels are preconditions, not
                // hand-offs.
                List.of(edge("in", "out"),
                        edge("out", "fed", "needs been out"),
                        edge("out", "train", "needs been out"),
                        edge("fed", "train", "needs fed")));
        Runner runner = (model, input, listener) -> {
            var out = agent(Agents.ToiletTrip.class, model, "ToiletTrip", "out");
            var fed = agent(Agents.FirstMeal.class, model, "FirstMeal", "fed");
            var train = agent(Agents.FirstTraining.class, model, "FirstTraining", "session");
            // Priorities, not order. Nobody needs telling that a puppy goes out before he is fed
            // and long before he is taught anything — so the room can see the planner making the
            // right call instead of taking it on trust. Shuffle these three declarations and the
            // behaviour does not change, which is the point of BDI and impossible to show with
            // two agents in the only order they could ever have run.
            List<Desire> desires = List.of(
                    Desire.of("out-first", 30, s -> true, s -> s.hasState("out"),
                            Agents.ToiletTrip.class),
                    Desire.of("then-feed", 20, s -> s.hasState("out"), s -> s.hasState("fed"),
                            Agents.FirstMeal.class),
                    Desire.of("then-teach", 5,
                            s -> s.hasState("out") && s.hasState("fed"),
                            s -> s.hasState("session"), Agents.FirstTraining.class));
            UntypedAgent app = AgenticServices.plannerBuilder()
                    .subAgents(out, fed, train)
                    .planner(() -> new BDIPlanner(desires))
                    .outputKey("session")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("hour", input));
            var scope = r.agenticScope();
            if (scope == null) {
                return result(r, "session");
            }
            return "**Out first** — " + str(scope, "out")
                    + "\n\n**Then fed** — " + str(scope, "fed")
                    + "\n\n**Then taught** — " + str(scope, "session");
        };
        return new PatternDef("bdi", "BDI (Belief-Desire-Intention)", "pattern-zoo",
                "The agent pursues prioritised desires, always acting on the highest-priority one "
                        + "that is achievable and not yet met. The puppy's first hour: out ranks "
                        + "food, food ranks training — and that is declared as a priority, not "
                        + "wired as an order.",
                // caveat: designing achievable/satisfied predicates is subtle and easy to get wrong.
                "Powerful but fiddly: the achievable and satisfied predicates are hard to get "
                        + "right, and a desire that can never be satisfied stalls the whole plan.",
                topo,
                "the puppy has just come home — eight weeks old, first hour in the house, "
                        + "he's been in the car for forty minutes",
                runner);
    }

    // 14 — a planner written by hand: the escalation ladder. See EscalationPlanner.
    private static PatternDef customPlanner() {
        Topology.Graph topo = graph("stages",
                // The three tiers share one column, stacked, so escalation reads downwards and
                // each rung's own way out reads across. A plain chain would draw a pipeline that
                // always runs all three, which is the opposite of what this planner does.
                List.of(node("in", "question", "input", 0),
                        node("book", "PuppyBook", "agent", 1),
                        node("trainer", "TrainerOnCall", "agent", 1),
                        node("vet", "VetOnCall", "agent", 1),
                        node("out", "first ANSWERED wins", "join", 2)),
                List.of(edge("in", "book"),
                        edge("book", "trainer", "ESCALATE"),
                        edge("trainer", "vet", "ESCALATE"),
                        edge("book", "out", "ANSWERED"),
                        edge("trainer", "out"),
                        edge("vet", "out")));
        Runner runner = (model, input, listener) -> {
            // Declaration order IS the cost order — that is the whole configuration of this
            // planner, and it is worth pointing at on stage: no prompt says "cheapest first".
            var book = agent(Agents.PuppyBook.class, model, "PuppyBook", "answer");
            var trainer = agent(Agents.TrainerOnCall.class, model, "TrainerOnCall", "answer");
            var vet = agent(Agents.VetOnCall.class, model, "VetOnCall", "answer");
            UntypedAgent app = AgenticServices.plannerBuilder()
                    .subAgents(book, trainer, vet)
                    // Same builder as every pattern above it. The only difference is that this
                    // planner is forty lines in this repo instead of forty lines in the library.
                    .planner(EscalationPlanner::new)
                    .outputKey("answer")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("question", input));
            // Report WHICH rung settled it and how many were asked. Returning just the answer
            // would hide the only thing this pattern does differently from a sequence — the
            // scope's invocation history is what makes that reportable without threading state
            // out of the planner.
            String answer = result(r, "answer");
            var scope = r.agenticScope();
            if (scope == null) {
                return answer;
            }
            List<String> asked = scope.agentInvocations().stream()
                    .map(AgentInvocation::agentName).filter(LADDER::contains).distinct().toList();
            boolean settled = answer.toUpperCase(Locale.ROOT).lastIndexOf("ANSWERED")
                    > answer.toUpperCase(Locale.ROOT).lastIndexOf("ESCALATE");
            String rung = asked.isEmpty() ? "nobody" : asked.get(asked.size() - 1);
            // The marker is protocol, not prose: the planner read it, the Scope tab still shows
            // it on the raw value, and the reader does not need it in the answer.
            String shown = answer.replaceAll("(?is)\\s*(ANSWERED|ESCALATE)\\s*$", "");
            return "**" + (settled ? "Answered by " + rung
                        : "Nobody could answer — best effort from " + rung)
                    + "** · asked " + asked.size() + " of " + LADDER.size() + " rungs\n\n" + shown;
        };
        return new PatternDef("customPlanner", "Custom Planner (write your own)", "pattern-zoo",
                "Every planner above is an implementation of one small interface — here is one "
                        + "written by hand. The policy is a cost ladder: ask the book, then the "
                        + "trainer, then the vet, and stop at the first rung that can actually "
                        + "answer. Change the question and watch it stop at a different rung: "
                        + "that decision depends on what came back, which is the one thing none "
                        + "of the built-in builders can express.",
                // caveat: you own the control loop, including the ways it can fail to end.
                "You own the loop now. Nothing stops a planner from never terminating, calling "
                        + "the same agent forever, or spending the whole budget on the top rung — "
                        + "`terminated()` and a hard tier count are the guard rails, and they are "
                        + "yours to write. Reach for this only when a built-in builder genuinely "
                        + "cannot say what you mean.",
                topo,
                // Escalates all the way, so the default run walks the whole ladder. Try
                // "which food should I buy for a four-year-old shepherd?" and it stops at the
                // book; try "he pulls like a train on the lead" and it stops at the trainer.
                "he's suddenly limping on his back left leg and won't put weight on it",
                runner);
    }
}
