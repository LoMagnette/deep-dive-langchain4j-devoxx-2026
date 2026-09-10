package dev.devoxx.dashboard;

import static dev.devoxx.dashboard.Parsing.category;
import static dev.devoxx.dashboard.Parsing.score;
import static dev.devoxx.dashboard.Topology.edge;
import static dev.devoxx.dashboard.Topology.graph;
import static dev.devoxx.dashboard.Topology.node;
import static dev.devoxx.dashboard.Wiring.agent;
import static dev.devoxx.dashboard.Wiring.result;
import static dev.devoxx.dashboard.Wiring.str;

import java.util.List;
import java.util.Map;

import dev.devoxx.dashboard.PatternDef.Runner;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.patterns.debate.ConvergenceStrategy;
import dev.langchain4j.agentic.patterns.debate.DebatePlanner;
import dev.langchain4j.agentic.patterns.voting.VotingPlanner;
import dev.langchain4j.agentic.patterns.voting.VotingStrategy;

/**
 * Systems rather than patterns: the payoff for the talk's arc. Each one nests the builders
 * above — every composite is itself an {@code UntypedAgent}, so a sequence can hold a loop
 * that holds a conditional.
 */
final class CompositePatterns {

    private CompositePatterns() {
    }

    /** In the order the rail and the gallery show them. */
    static List<PatternDef> all() {
        return List.of(nightHandover(), placementCouncil());
    }

    // 14 — the capstone: four patterns composed into the sheet the night hand actually carries
    private static PatternDef nightHandover() {
        Topology.Graph topo = graph("stages",
                List.of(node("in", "call", "input", 0),
                        node("router", "NightLineRouter", "router", 1),
                        node("emergency", "EmergencyVet", "agent", 2),
                        node("behaviour", "BehaviourDesk", "agent", 2),
                        node("booking", "BookingDesk", "agent", 2),
                        node("rota", "RotaPlanner", "agent", 2),
                        node("feed", "FeedPlanner", "agent", 2),
                        node("writer", "HandoverWriter", "join", 3),
                        node("editor", "HandoverEditor", "agent", 4),
                        node("checker", "HandoverChecker", "agent", 4)),
                List.of(edge("in", "router"),
                        edge("router", "emergency", "emergency"),
                        edge("router", "behaviour", "behaviour"),
                        edge("router", "booking", "booking"),
                        edge("in", "rota", "in parallel"),
                        edge("in", "feed"),
                        edge("emergency", "writer"), edge("behaviour", "writer"),
                        edge("booking", "writer", "answer"),
                        edge("rota", "writer"), edge("feed", "writer"),
                        edge("writer", "editor", "sheet"),
                        edge("editor", "checker"),
                        edge("checker", "editor", "score < 0.8")));

        Runner runner = (model, input, listener) -> {
            // 1. Conditional routing — one LLM judgement decides which desk answers the call.
            var router = agent(Agents.NightLineRouter.class, model, "NightLineRouter", "category");
            var emergency = agent(Agents.EmergencyVet.class, model, "EmergencyVet", "answer");
            var behaviour = agent(Agents.BehaviourDesk.class, model, "BehaviourDesk", "answer");
            var booking = agent(Agents.BookingDesk.class, model, "BookingDesk", "answer");
            UntypedAgent triage = AgenticServices.conditionalBuilder()
                    .subAgents(s -> category(s).equals("emergency"), emergency)
                    .subAgents(s -> category(s).equals("behaviour"), behaviour)
                    .subAgents(s -> category(s).equals("booking"), booking)
                    .build();

            // 2. Parallel — exercise and feeding are independent, so fan them out.
            var rota = agent(Agents.RotaPlanner.class, model, "RotaPlanner", "rota");
            var feed = agent(Agents.FeedPlanner.class, model, "FeedPlanner", "feed");
            UntypedAgent plan = AgenticServices.parallelBuilder()
                    .subAgents(rota, feed)
                    .build();

            // 3. Loop — refine the sheet until it satisfies three named rules, never forever.
            var editor = agent(Agents.HandoverEditor.class, model, "HandoverEditor", "sheet");
            var checker = agent(Agents.HandoverChecker.class, model, "HandoverChecker", "score");
            UntypedAgent refine = AgenticServices.loopBuilder()
                    .subAgents(editor, checker)
                    .maxIterations(3)
                    .exitCondition(s -> score(s) >= 0.8)
                    .testExitAtLoopEnd(true)
                    .build();

            // 4. Sequence — the spine that holds the three composites plus the merge step.
            var writer = agent(Agents.HandoverWriter.class, model, "HandoverWriter", "sheet");
            UntypedAgent app = AgenticServices.sequenceBuilder()
                    .subAgents(router, triage, plan, writer, refine)
                    .outputKey("sheet")
                    .listener(listener)
                    .build();
            // The same text under two keys, and not by accident: the night line's own agents read
            // 'call', but RotaPlanner and FeedPlanner were written for the supervisor demo, where
            // the planner protocol names every argument 'request'. Reusing an agent means
            // accepting the key IT already declared — this one line is the seam the caveat is
            // about, and it is the sort of thing that breaks a composite with a
            // "Missing argument" error pointing at a step that looks unrelated.
            var r = app.invokeWithAgenticScope(Map.of("call", input, "request", input));
            return result(r, "sheet");
        };

        return new PatternDef("nightHandover", "Night Handover (composite)", "composite",
                "A real system, not a pattern: an out-of-hours call is triaged to a desk, a "
                        + "parallel step plans exercise and feeding, a sequence merges all three "
                        + "into the sheet the night hand carries, and a loop refines it until it "
                        + "names the dog, every dose and who to telephone. Deterministic "
                        + "scaffolding with LLM judgement at exactly three points.",
                // caveat: the interesting failures in composites are at the seams, not inside them.
                "Composites fail at the seams: every step depends on a key an earlier one wrote, "
                        + "so one agent answering off-format breaks a step that looks unrelated.",
                topo,
                "Nero's belly has gone swollen and tight, he keeps retching, and his antibiotic "
                        + "is due at 20:00",
                runner);
    }

    // 15 — the second capstone: two zoo patterns carried by simple plumbing
    private static PatternDef placementCouncil() {
        Topology.Graph topo = graph("stages",
                List.of(node("in", "question", "input", 0),
                        node("scout", "CaseScout (per angle)", "agent", 1),
                        node("briefer", "CouncilBriefer", "join", 2),
                        node("one", "HomeOneAdvocate", "agent", 3),
                        node("two", "HomeTwoAdvocate", "agent", 3),
                        node("panel", "PlacementPanel", "judge", 4),
                        node("note", "CouncilNote", "join", 5),
                        node("temperament", "TemperamentAssessor", "agent", 6),
                        node("foster", "FosterAssessor", "agent", 6),
                        node("medical", "MedicalAssessor", "agent", 6),
                        node("tally", "majority()", "join", 7)),
                List.of(edge("in", "scout", "3 angles"),
                        edge("scout", "briefer", "findings"),
                        edge("briefer", "one", "motion"), edge("briefer", "two"),
                        edge("one", "two", "rebut"), edge("two", "one", "rebut"),
                        edge("one", "panel"), edge("two", "panel"),
                        edge("panel", "note", "verdict"),
                        edge("note", "temperament"), edge("note", "foster"),
                        edge("note", "medical"),
                        edge("temperament", "tally"), edge("foster", "tally"),
                        edge("medical", "tally")));

        Runner runner = (model, input, listener) -> {
            // 1. Parallel mapper (simple) — the same scout reads three angles of the dossier
            //    at once.
            var scout = agent(Agents.CaseScout.class, model, "CaseScout", "finding");
            UntypedAgent survey = AgenticServices.parallelMapperBuilder()
                    .subAgents(scout)
                    .itemsProvider("angles")
                    .outputKey("findings")
                    .build();

            // 2. One plain agent (simple) turns the evidence into something debatable.
            var briefer = agent(Agents.CouncilBriefer.class, model, "CouncilBriefer", "motion");

            // 3. Debate (advanced) — the two case workers argue, the panel rules.
            var one = agent(Agents.HomeOneAdvocate.class, model, "HomeOneAdvocate", null);
            var two = agent(Agents.HomeTwoAdvocate.class, model, "HomeTwoAdvocate", null);
            var panel = agent(Agents.PlacementPanel.class, model, "PlacementPanel", "verdict");
            UntypedAgent debate = AgenticServices.plannerBuilder()
                    .subAgents(one, two, panel)          // judge LAST
                    .planner(() -> new DebatePlanner(2, ConvergenceStrategy.unanimous()))
                    .outputKey("verdict")
                    .build();

            // 4. Glue (simple): the assessors ratify a 'dossier', the debate wrote a 'verdict'.
            //    This one line is the whole lesson of this composite — see the caveat.
            var note = agent(Agents.CouncilNote.class, model, "CouncilNote", "dossier");

            // 5. Voting (advanced) — three rubrics, majority ratifies the ruling.
            var temperament = agent(Agents.TemperamentAssessor.class, model,
                    "TemperamentAssessor", null);
            var foster = agent(Agents.FosterAssessor.class, model, "FosterAssessor", null);
            var medical = agent(Agents.MedicalAssessor.class, model, "MedicalAssessor", null);
            UntypedAgent ratify = AgenticServices.plannerBuilder()
                    .subAgents(temperament, foster, medical)
                    .planner(() -> new VotingPlanner(VotingStrategy.majority()))
                    .outputKey("ratified")
                    .build();

            UntypedAgent app = AgenticServices.sequenceBuilder()
                    .subAgents(survey, briefer, debate, note, ratify)
                    .outputKey("verdict")
                    .listener(listener)
                    .build();
            // The angles are derived here rather than by an agent: the mapper needs a real
            // collection in scope before anything has run.
            var r = app.invokeWithAgenticScope(Map.of(
                    "question", input,
                    "angles", List.of("what the dog needs — " + input,
                            "what each home can actually offer — " + input,
                            "what could go wrong in the first month — " + input)));
            // The last stage is the vote, so the result has to show it: returning only the
            // debate's verdict would leave the ratification invisible and the final third of the
            // diagram looking decorative.
            var scope = r.agenticScope();
            if (scope == null) {
                return result(r, "verdict");
            }
            return "**Council ruling** — " + str(scope, "dossier")
                    + "\n\n**Ratified by the three assessors:** " + str(scope, "ratified");
        };

        return new PatternDef("placementCouncil", "Placement Council (composite)", "composite",
                "Settles a contested placement: a mapper reads three angles of the dossier at "
                        + "once, one agent turns them into a motion, a debate argues it to a "
                        + "ruling, and a vote of three assessors ratifies it. Two zoo patterns "
                        + "carried by simple plumbing.",
                // caveat: the zoo patterns are the easy part; the adapters between them are not.
                "Most of this system is glue. Each zoo pattern expects its input under its own "
                        + "key — the debate writes 'verdict', the assessors read 'dossier' — so "
                        + "composing them is mostly writing the small steps in between.",
                topo,
                "Bruno needs placing and both homes want him. Home One: large garden, but two "
                        + "cats and nobody home from nine to six. Home Two: first-floor flat with "
                        + "no garden, but someone home all day and two mastiffs raised before.",
                runner);
    }
}
