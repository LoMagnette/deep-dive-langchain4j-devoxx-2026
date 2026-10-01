package dev.devoxx.dashboard.demos._08_nonaiagent;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;

import java.util.List;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._08_nonaiagent.Keys.Plan;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for the <b>non-AI agent</b> demo — Java on both ends, a model in the middle.
 */
public final class NonAiAgentPattern {

    private NonAiAgentPattern() {
    }

    static String run(ChatModel model, String input, StreamingListener listener) {
        var diary = new CatsDiary();
        var check = new CatCheck();

        var beagle = AgenticServices.agentBuilder(DigPlanner.class)
                .chatModel(model)
                .name("DigPlanner")
                .outputKey(Plan.class)
                .build();

        DigPipeline app = AgenticServices.sequenceBuilder(DigPipeline.class)
                .name("Sequential")
                .subAgents(diary, beagle, check)
                .outputKey(Plan.class)
                .listener(listener)
                .build();
        return app.dig(input);
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // The two Java steps are NOT drawn as agents, for the same reason the person in demo 7
        // is not: an identical box either side would say the model did it, which is the one
        // thing this demo denies. They get the 'code' role and read as what they are.
        Topology.Graph topo = graph("stages",
                List.of(node("in", "the request", "input", 0),
                        node("diary", "CatsDiary", "code", 1)
                                .withSub("cat · no model · saw it"),
                        node("write", "DigPlanner", "agent", 2)
                                .withSub("Beagle · the only model"),
                        node("check", "CatCheck", "code", 3)
                                .withSub("cat · no model · checks"),
                        node("out", "the dig plan", "join", 4)),
                List.of(edge("in", "diary"),
                        edge("diary", "write", "where it all is"),
                        edge("write", "check", "the plan"),
                        edge("check", "out")));
        return new PatternDef("nonAiAgent", "Non-AI Agents (plain Java)", "workflow",
                "The humans want the TV remote back. Nobody in the pack remembers where it was "
                        + "buried. The cat saw everything.",
                "Demo 7 said a HumanInTheLoop is a non-AI agent. This is the general "
                        + "case: your own class, no model, same wiring.",
                "An agent does not have to be a model. Any object with one `@Agent` method goes "
                        + "straight into `subAgents(...)` — its `@K` parameters are bound from the "
                        + "scope and its return value is written to its output key, so **the "
                        + "sequence cannot tell**. Here Java is on both ends: the cat's diary "
                        + "supplies the locations, the model writes the plan, and a plain "
                        + "`String::contains` checks the locations survived — a model asked where "
                        + "a remote is buried will happily invent a flowerbed. The test for a step is not \"could a model "
                        + "do this\" — it is \"is this judgement, or is it a lookup\". "
                        + "`AgenticServices.agentAction(scope -> …)` is the one-line lambda form.\n\n"
                        + "**Watch the diagram while it runs.** The middle box lights up and the "
                        + "two Java boxes never do — see the caveat. They are doing the work all "
                        + "the same: `Facts` appears in the Scope tab, and the cat's finding is "
                        + "at the bottom of the result.",
                "**A non-AI agent is invisible to the listener** in `1.20.0-beta30` — it is, "
                        + "after all, a cat. "
                        + "`NonAiAgentInstance.setParent` sets the parent and never calls "
                        + "`registerInheritedParentListener`, which both `AgentInvocationHandler` "
                        + "and `PlannerBasedInvocationHandler` do — so a plain-Java step inherits "
                        + "no listener, emits no events, and is never timed. Worth knowing before "
                        + "you put one on a critical path, and a fair reminder that this module is "
                        + "experimental.\n\nThe smaller trap: `name` has to go on the "
                        + "**annotation**. There is no builder to call `.name(\"X\")` on and the "
                        + "default is the *method* name, so this agent would be called \"lookup\" "
                        + "everywhere. The lambda form has no answer at all — `agentAction(...)` "
                        + "comes out named `run`.",
                topo,
                "the humans want the TV remote back before the match starts. Nobody in the pack "
                        + "remembers where anything is buried.",
                NonAiAgentPattern::run);
    }
}
