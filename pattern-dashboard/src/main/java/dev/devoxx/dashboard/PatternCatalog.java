package dev.devoxx.dashboard;

import static dev.devoxx.dashboard.Topology.edge;
import static dev.devoxx.dashboard.Topology.graph;
import static dev.devoxx.dashboard.Topology.node;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

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
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Registry of the 13 LangChain4j agentic patterns, each with display metadata, a static
 * topology (for the SVG graph) and a live {@code run(...)}. Every wiring below doubles as
 * readable demo code for the talk.
 */
@ApplicationScoped
public class PatternCatalog {

    /** A pattern's live behaviour. */
    @FunctionalInterface
    public interface Runner {
        String run(ChatModel model, String input, StreamingListener listener) throws Exception;
    }

    /** JSON view of a pattern (no runnable code). */
    public record PatternInfo(String id, String name, String category, String useful,
                              String caveat, Topology.Graph topology, String defaultInput) {
    }

    /** Full pattern definition including its runner. */
    public record PatternDef(String id, String name, String category, String useful, String caveat,
                             Topology.Graph topology, String defaultInput, Runner runner) {

        public PatternInfo toInfo() {
            return new PatternInfo(id, name, category, useful, caveat, topology, defaultInput);
        }

        /** Never throws: on failure it emits an error event and returns an explanation. */
        public String run(ChatModel model, String input, StreamingListener listener) {
            try {
                return runner.run(model, input, listener);
            } catch (Exception e) {
                // Errors.explain, not getMessage(): the top-level AgentInvocationException only
                // says "Failed to invoke agent method", which is true of every failure.
                String why = Errors.explain(e);
                listener.emitError(id, "pattern '" + id + "' failed: " + why);
                return "This pattern could not complete: " + why;
            }
        }
    }

    private final List<PatternDef> patterns = build();

    public List<PatternInfo> infos() {
        return patterns.stream().map(PatternDef::toInfo).toList();
    }

    public Optional<PatternDef> byId(String id) {
        return patterns.stream().filter(p -> p.id().equals(id)).findFirst();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static <T> T agent(Class<T> cls, ChatModel model, String name, String outputKey) {
        var b = AgenticServices.agentBuilder(cls).chatModel(model).name(name);
        if (outputKey != null) {
            b = b.outputKey(outputKey);
        }
        return b.build();
    }

    private static String str(AgenticScope s, String key) {
        Object v = s.readState(key, "");
        return v == null ? "" : v.toString();
    }

    private static String result(ResultWithAgenticScope<?> r, String preferKey) {
        if (preferKey != null && r.agenticScope() != null && r.agenticScope().hasState(preferKey)) {
            return String.valueOf(r.agenticScope().readState(preferKey));
        }
        return String.valueOf(r.result());
    }

    private static final Pattern NUMBER = Pattern.compile("\\d+(?:[.,]\\d+)?");

    /**
     * Reads the loop's quality score defensively. Asked for "just a number", a real model
     * cheerfully answers "I'd rate this **8.5** out of 10" — so pull out the first number and
     * rescale anything above 1.0. An unparseable answer scores 0, which keeps the loop iterating
     * rather than exiting on garbage.
     */
    static double score(AgenticScope s) {
        var m = NUMBER.matcher(str(s, "score"));
        if (!m.find()) {
            return 0.0;
        }
        double v = Double.parseDouble(m.group().replace(',', '.'));
        while (v > 1.0) {
            v /= 10.0;
        }
        return v;
    }

    /** The categories the conditional router is allowed to dispatch to. */
    private static final List<String> CATEGORIES = List.of("technical", "legal", "medical");

    /**
     * Normalises the router's answer to exactly one known category. Asked to "return one word",
     * a real model answers "This request is best categorised as: **medical**." — an exact
     * {@code equalsIgnoreCase} then matches no branch at all and the run silently produces null.
     * We take the LAST category mentioned (models state the conclusion at the end) and fall back
     * to the first category so that some branch always fires.
     */
    static String category(AgenticScope s) {
        String raw = str(s, "category").toLowerCase(Locale.ROOT);
        String best = CATEGORIES.get(0);
        int bestAt = -1;
        for (String c : CATEGORIES) {
            int at = raw.lastIndexOf(c);
            if (at > bestAt) {
                bestAt = at;
                best = c;
            }
        }
        return best;
    }

    /**
     * Splits the user's typed input into items for the parallel mapper. A single-chunk input
     * (nothing to fan out over) falls back to three canned topics.
     */
    static List<String> items(String input) {
        List<String> parsed = Arrays.stream(String.valueOf(input).split("[,;\n]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        return parsed.size() > 1 ? parsed
                : List.of("Zao's favourite toys", "Zao's morning walk", "Zao's dinner");
    }

    // ------------------------------------------------------------------
    // The 13 patterns
    // ------------------------------------------------------------------

    private List<PatternDef> build() {
        return List.of(
                single(),
                sequential(),
                loop(),
                parallel(),
                parallelMapper(),
                conditional(),
                supervisor(),
                goap(),
                p2p(),
                blackboard(),
                voting(),
                debate(),
                bdi());
    }

    // 1 — single agent
    private PatternDef single() {
        Topology.Graph topo = graph("chain",
                List.of(node("in", "topic", "input"),
                        node("writer", "CreativeWriter", "agent"),
                        node("scope", "AgenticScope", "board")),
                List.of(edge("in", "writer"), edge("writer", "scope", "story")));
        Runner runner = (model, input, listener) -> {
            var writer = agent(Agents.CreativeWriter.class, model, "CreativeWriter", "story");
            UntypedAgent app = AgenticServices.sequenceBuilder()
                    .subAgents(writer).outputKey("story").listener(listener).build();
            var r = app.invokeWithAgenticScope(Map.of("topic", input));
            return result(r, "story");
        };
        return new PatternDef("single", "Single Agent", "workflow",
                "One LLM call wrapped as an agent — the simplest useful unit.",
                "No decomposition: a single agent struggles with multi-step or long tasks.",
                topo, "Zao the Belgian shepherd discovers snow for the first time", runner);
    }

    // 2 — sequential (writer -> editor)
    private PatternDef sequential() {
        Topology.Graph topo = graph("chain",
                List.of(node("in", "topic", "input"),
                        node("writer", "CreativeWriter", "agent"),
                        node("editor", "StoryEditor", "agent"),
                        node("scope", "AgenticScope", "board")),
                List.of(edge("in", "writer"), edge("writer", "editor", "story"),
                        edge("editor", "scope", "editedStory")));
        Runner runner = (model, input, listener) -> {
            var writer = agent(Agents.CreativeWriter.class, model, "CreativeWriter", "story");
            var editor = agent(Agents.StoryEditor.class, model, "StoryEditor", "editedStory");
            UntypedAgent app = AgenticServices.sequenceBuilder()
                    .subAgents(writer, editor).outputKey("editedStory").listener(listener).build();
            var r = app.invokeWithAgenticScope(Map.of("topic", input));
            return result(r, "editedStory");
        };
        return new PatternDef("sequential", "Sequential", "workflow",
                "Deterministic pipeline: each agent's output feeds the next.",
                "Rigid order; a failure or bad hand-off midway derails the whole chain.",
                topo, "Zao eats a shoe and feels guilty", runner);
    }

    // 3 — loop (editor + scorer until score >= 0.8)
    private PatternDef loop() {
        Topology.Graph topo = graph("loop",
                List.of(node("in", "story", "input"),
                        node("editor", "StoryEditor", "agent"),
                        node("scorer", "StoryScorer", "agent"),
                        node("scope", "AgenticScope", "board")),
                List.of(edge("in", "editor"), edge("editor", "scorer", "story"),
                        edge("scorer", "editor", "score < 0.8"),
                        edge("scorer", "scope", "score")));
        Runner runner = (model, input, listener) -> {
            var editor = agent(Agents.StoryEditor.class, model, "StoryEditor", "story");
            var scorer = agent(Agents.StoryScorer.class, model, "StoryScorer", "score");
            Predicate<AgenticScope> good = s -> score(s) >= 0.8;
            UntypedAgent app = AgenticServices.loopBuilder()
                    .subAgents(editor, scorer)
                    .maxIterations(5)
                    .exitCondition(good)
                    .testExitAtLoopEnd(true)
                    .outputKey("story")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("story", input));
            return result(r, "story");
        };
        return new PatternDef("loop", "Loop / Iterative Refinement", "workflow",
                "Refine until a quality bar is met (self-critique with an exit condition).",
                "Can spin forever or oscillate — always cap iterations and define a clear exit.",
                topo, "Zao becomes the mayor of a small Belgian village", runner);
    }

    // 4 — parallel (movie + meal, combined)
    private PatternDef parallel() {
        Topology.Graph topo = graph("fanout",
                List.of(node("in", "mood", "input"),
                        node("movie", "MovieExpert", "agent"),
                        node("meal", "MealExpert", "agent"),
                        node("scope", "AgenticScope", "board")),
                List.of(edge("in", "movie"), edge("in", "meal"),
                        edge("movie", "scope", "movie"), edge("meal", "scope", "meal")));
        Runner runner = (model, input, listener) -> {
            var movie = agent(Agents.MovieExpert.class, model, "MovieExpert", "movie");
            var meal = agent(Agents.MealExpert.class, model, "MealExpert", "meal");
            UntypedAgent app = AgenticServices.parallelBuilder()
                    .subAgents(movie, meal)
                    .output(s -> "Movie: " + str(s, "movie") + "\nMeal: " + str(s, "meal"))
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("mood", input));
            return String.valueOf(r.result());
        };
        return new PatternDef("parallel", "Parallel", "workflow",
                "Fan out independent work concurrently, then join the results.",
                "Only for truly independent sub-tasks; joining/merging logic is on you.",
                topo, "cozy and a little nostalgic", runner);
    }

    // 5 — parallel mapper (one agent over a list of items)
    private PatternDef parallelMapper() {
        Topology.Graph topo = graph("fanout",
                List.of(node("in", "topics[3]", "input"),
                        node("analyzer", "TopicAnalyzer (per item)", "agent"),
                        node("scope", "AgenticScope", "board")),
                List.of(edge("in", "analyzer"), edge("analyzer", "scope", "analyses")));
        Runner runner = (model, input, listener) -> {
            // The mapper collects each per-item invocation under the agent's outputKey.
            var analyzer = agent(Agents.TopicAnalyzer.class, model, "TopicAnalyzer", "analysis");
            UntypedAgent app = AgenticServices.parallelMapperBuilder()
                    .subAgents(analyzer)
                    .itemsProvider("topics")
                    .outputKey("analyses")
                    .listener(listener)
                    .build();
            // The items come from what the user typed (comma-separated), not a hard-coded list —
            // otherwise the input box on the page has no effect on this pattern.
            var r = app.invokeWithAgenticScope(Map.of("topics", items(input)));
            return result(r, "analyses");
        };
        return new PatternDef("parallelMapper", "Parallel Mapper", "workflow",
                "Map one agent over a collection in parallel (scatter/gather).",
                "Beware fan-out cost and rate limits when the list is large.",
                topo, "Zao's favourite toys, Zao's morning walk, Zao's dinner", runner);
    }

    // 6 — conditional routing
    private PatternDef conditional() {
        Topology.Graph topo = graph("branch",
                List.of(node("in", "request", "input"),
                        node("router", "CategoryRouter", "router"),
                        node("tech", "TechnicalExpert", "agent"),
                        node("legal", "LegalExpert", "agent"),
                        node("medical", "MedicalExpert", "agent"),
                        node("scope", "AgenticScope", "board")),
                List.of(edge("in", "router"),
                        edge("router", "tech", "technical"),
                        edge("router", "legal", "legal"),
                        edge("router", "medical", "medical"),
                        edge("tech", "scope", "answer"),
                        edge("legal", "scope", "answer"),
                        edge("medical", "scope", "answer")));
        Runner runner = (model, input, listener) -> {
            var router = agent(Agents.CategoryRouter.class, model, "CategoryRouter", "category");
            var tech = agent(Agents.TechnicalExpert.class, model, "TechnicalExpert", "answer");
            var legal = agent(Agents.LegalExpert.class, model, "LegalExpert", "answer");
            var medical = agent(Agents.MedicalExpert.class, model, "MedicalExpert", "answer");
            Predicate<AgenticScope> isTech = s -> category(s).equals("technical");
            Predicate<AgenticScope> isLegal = s -> category(s).equals("legal");
            Predicate<AgenticScope> isMed = s -> category(s).equals("medical");
            UntypedAgent routed = AgenticServices.conditionalBuilder()
                    .subAgents(isTech, tech)
                    .subAgents(isLegal, legal)
                    .subAgents(isMed, medical)
                    .build();
            UntypedAgent app = AgenticServices.sequenceBuilder()
                    .subAgents(router, routed)
                    .outputKey("answer")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("request", input));
            return result(r, "answer");
        };
        return new PatternDef("conditional", "Conditional Routing", "workflow",
                "A router classifies the input and dispatches to the right specialist.",
                "Only as good as the classifier; unseen categories fall through the cracks.",
                topo, "my dog Zao keeps limping after walks, what should I do?", runner);
    }

    // 7 — supervisor (pure agent: LLM plans which sub-agent to call)
    private PatternDef supervisor() {
        Topology.Graph topo = graph("star",
                List.of(node("supervisor", "Supervisor", "supervisor"),
                        node("activity", "ActivityPlanner", "agent"),
                        node("meal", "MealPlanner", "agent"),
                        node("scope", "AgenticScope", "board")),
                List.of(edge("supervisor", "activity"), edge("supervisor", "meal"),
                        edge("supervisor", "scope")));
        Runner runner = (model, input, listener) -> {
            var activity = agent(Agents.ActivityPlanner.class, model, "ActivityPlanner", null);
            var meal = agent(Agents.MealPlanner.class, model, "MealPlanner", null);
            SupervisorAgent sup = AgenticServices.supervisorBuilder()
                    .subAgents(activity, meal)
                    .chatModel(model)                 // planner LLM lives on the supervisor
                    .responseStrategy(SupervisorResponseStrategy.LAST)
                    .maxAgentsInvocations(3)
                    .listener(listener)
                    .build();
            var r = sup.invokeWithAgenticScope(input);
            return String.valueOf(r.result());
        };
        return new PatternDef("supervisor", "Supervisor", "pure-agent",
                "An LLM supervisor dynamically decides which specialist to invoke, and when.",
                "Non-deterministic and needs a capable planner LLM; bound invocations to stay safe.",
                topo, "plan a great Saturday for the dog Zao", runner);
    }

    // 8 — GOAP (goal-oriented planning; outputKeys chain automatically)
    private PatternDef goap() {
        Topology.Graph topo = graph("dag",
                List.of(node("in", "prompt", "input"),
                        node("extractor", "PersonExtractor", "agent"),
                        node("bio", "Biographer", "agent"),
                        node("scope", "AgenticScope", "board")),
                List.of(edge("in", "extractor"), edge("extractor", "bio", "person"),
                        edge("bio", "scope", "writeup")));
        Runner runner = (model, input, listener) -> {
            var extractor = agent(Agents.PersonExtractor.class, model, "PersonExtractor", "person");
            var bio = agent(Agents.Biographer.class, model, "Biographer", "writeup");
            UntypedAgent app = AgenticServices.plannerBuilder()
                    .subAgents(extractor, bio)
                    .planner(GoalOrientedPlanner::new)
                    .outputKey("writeup")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("prompt", input));
            return result(r, "writeup");
        };
        return new PatternDef("goap", "GOAP (Goal-Oriented Planning)", "pattern-zoo",
                "The planner orders agents automatically by matching each output to the next input.",
                // caveat: planning is only as good as the declared pre/post-conditions (I/O keys).
                "Needs well-declared I/O keys; a missing link means the goal is unreachable.",
                topo, "Write a short bio: the famous Belgian shepherd Zao", runner);
    }

    // 9 — P2P (peers refine shared state until consensus)
    private PatternDef p2p() {
        Topology.Graph topo = graph("mesh",
                List.of(node("in", "issue", "input"),
                        node("negotiator", "Negotiator", "agent"),
                        node("mediator", "Mediator", "agent"),
                        node("scope", "AgenticScope", "board")),
                List.of(edge("in", "negotiator"),
                        edge("negotiator", "mediator", "proposal"),
                        edge("mediator", "negotiator", "refine"),
                        edge("mediator", "scope", "consensus")));
        Runner runner = (model, input, listener) -> {
            var negotiator = agent(Agents.Negotiator.class, model, "Negotiator", "proposal");
            var mediator = agent(Agents.Mediator.class, model, "Mediator", "consensus");
            UntypedAgent app = AgenticServices.plannerBuilder()
                    .subAgents(negotiator, mediator)
                    .planner(() -> new P2PPlanner(10, s -> s.hasState("consensus")))
                    .outputKey("consensus")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("issue", input));
            return result(r, "consensus");
        };
        return new PatternDef("p2p", "Peer-to-Peer", "pattern-zoo",
                "Peers iteratively refine a shared blackboard until an exit condition holds.",
                // caveat: without a firm exit predicate peers can ping-pong indefinitely.
                "No fixed hierarchy — needs a solid exit predicate or it never terminates.",
                topo, "should Zao sleep indoors or in the garden?", runner);
    }

    // 10 — blackboard (experts contribute until goal state is reached)
    private PatternDef blackboard() {
        Topology.Graph topo = graph("star",
                List.of(node("scope", "Blackboard (Scope)", "board"),
                        node("researcher", "Researcher", "agent"),
                        node("analyst", "Analyst", "agent"),
                        node("solver", "Solver", "agent")),
                List.of(edge("researcher", "scope", "facts"),
                        edge("analyst", "scope", "analysis"),
                        edge("solver", "scope", "solution")));
        Runner runner = (model, input, listener) -> {
            var researcher = agent(Agents.Researcher.class, model, "Researcher", "facts");
            var analyst = agent(Agents.Analyst.class, model, "Analyst", "analysis");
            var solver = agent(Agents.Solver.class, model, "Solver", "solution");
            Predicate<AgenticScope> goal = s -> s.hasState("solution");
            UntypedAgent app = AgenticServices.plannerBuilder()
                    .subAgents(researcher, analyst, solver)
                    .planner(() -> new BlackboardPlanner(goal,
                            ConflictResolutionStrategy.declarationOrder()))
                    .outputKey("solution")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("problem", input));
            return result(r, "solution");
        };
        return new PatternDef("blackboard", "Blackboard", "pattern-zoo",
                "Experts read/write a shared blackboard, contributing until a goal state exists.",
                // caveat: concurrent writers need a conflict-resolution strategy.
                "Shared mutable state invites conflicts; pick a conflict-resolution strategy.",
                topo, "how to keep Zao happy and healthy through a Belgian winter", runner);
    }

    // 11 — voting (3 classifiers, majority wins)
    private PatternDef voting() {
        Topology.Graph topo = graph("fanout",
                List.of(node("in", "text", "input"),
                        node("a", "SentimentVoterA", "agent"),
                        node("b", "SentimentVoterB", "agent"),
                        node("c", "SentimentVoterC", "agent"),
                        node("scope", "AgenticScope", "board")),
                List.of(edge("in", "a"), edge("in", "b"), edge("in", "c"),
                        edge("a", "scope"), edge("b", "scope"), edge("c", "scope", "majority")));
        Runner runner = (model, input, listener) -> {
            var a = agent(Agents.SentimentVoterA.class, model, "SentimentVoterA", null);
            var b = agent(Agents.SentimentVoterB.class, model, "SentimentVoterB", null);
            var c = agent(Agents.SentimentVoterC.class, model, "SentimentVoterC", null);
            UntypedAgent app = AgenticServices.plannerBuilder()
                    .subAgents(a, b, c)
                    .planner(() -> new VotingPlanner(VotingStrategy.majority()))
                    .outputKey("classification")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("text", input));
            return result(r, "classification");
        };
        return new PatternDef("voting", "Voting / Ensemble", "pattern-zoo",
                "Several agents answer independently; a strategy aggregates (majority/avg/highest).",
                // caveat: correlated models vote alike, so an ensemble can be confidently wrong.
                "Correlated voters agree on the same mistake — diversity is what buys robustness.",
                topo, "Zao learned to open the fridge and ate the whole roast", runner);
    }

    // 12 — debate (2 debaters + judge; judge LAST)
    private PatternDef debate() {
        Topology.Graph topo = graph("mesh",
                List.of(node("in", "motion", "input"),
                        node("a", "DebaterA", "agent"),
                        node("b", "DebaterB", "agent"),
                        node("judge", "DebateJudge", "judge"),
                        node("scope", "AgenticScope", "board")),
                List.of(edge("in", "a"), edge("in", "b"),
                        edge("a", "b", "rebut"), edge("b", "a", "rebut"),
                        edge("a", "judge"), edge("b", "judge"),
                        edge("judge", "scope", "verdict")));
        Runner runner = (model, input, listener) -> {
            var a = agent(Agents.DebaterA.class, model, "DebaterA", null);
            var b = agent(Agents.DebaterB.class, model, "DebaterB", null);
            var judge = agent(Agents.DebateJudge.class, model, "DebateJudge", "verdict");
            UntypedAgent app = AgenticServices.plannerBuilder()
                    .subAgents(a, b, judge) // last sub-agent is the judge
                    .planner(() -> new DebatePlanner(2, ConvergenceStrategy.unanimous()))
                    .outputKey("verdict")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("motion", input));
            return result(r, "verdict");
        };
        return new PatternDef("debate", "Debate", "pattern-zoo",
                "Agents argue opposing sides for N rounds; a judge renders the verdict.",
                // caveat: eloquence can beat correctness; more rounds cost more tokens.
                "The most persuasive agent may win over the most correct one — and it's token-hungry.",
                topo, "Zao should be allowed on the sofa", runner);
    }

    // 13 — BDI (beliefs-desires-intentions; prioritised desires)
    private PatternDef bdi() {
        Topology.Graph topo = graph("dag",
                List.of(node("in", "goal", "input"),
                        node("gatherer", "InfoGatherer (desire p10)", "agent"),
                        node("reporter", "Reporter (desire p5)", "agent"),
                        node("scope", "AgenticScope", "board")),
                List.of(edge("in", "gatherer"),
                        edge("gatherer", "reporter", "info"),
                        edge("reporter", "scope", "report")));
        Runner runner = (model, input, listener) -> {
            var gatherer = agent(Agents.InfoGatherer.class, model, "InfoGatherer", "info");
            var reporter = agent(Agents.Reporter.class, model, "Reporter", "report");
            List<Desire> desires = List.of(
                    Desire.of("gather-info", 10, s -> true, s -> s.hasState("info"),
                            Agents.InfoGatherer.class),
                    Desire.of("write-report", 5, s -> s.hasState("info"), s -> s.hasState("report"),
                            Agents.Reporter.class));
            UntypedAgent app = AgenticServices.plannerBuilder()
                    .subAgents(gatherer, reporter)
                    .planner(() -> new BDIPlanner(desires))
                    .outputKey("report")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("goal", input));
            return result(r, "report");
        };
        return new PatternDef("bdi", "BDI (Belief-Desire-Intention)", "pattern-zoo",
                "Agent pursues prioritised desires, acting on the highest achievable, unmet one.",
                // caveat: designing achievable/satisfied predicates is subtle and easy to get wrong.
                "Powerful but fiddly: the achievable/satisfied predicates are hard to get right.",
                topo, "produce a care report for the dog Zao", runner);
    }
}
