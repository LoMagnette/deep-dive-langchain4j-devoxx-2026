package dev.devoxx.dashboard.demos._08_nonaiagent;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;

import java.util.List;
import java.util.Map;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._04_parallel.Keys.Stay;
import dev.devoxx.dashboard.demos._01_single.Keys.Notes;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for the <b>non-AI agent</b> demo — Java on both ends, a model in the middle.
 */
public final class NonAiAgentPattern {

    private NonAiAgentPattern() {
    }

    /** The wiring. Everything below it is the dashboard telling itself how to draw this. */
    static String run(ChatModel model, String input, StreamingListener listener) {
        // Two of these three "agents" are `new`. No builder, no chatModel, no prompt — the
        // objects go straight into subAgents(), and everything downstream treats them the same
        // as the one in the middle: bound from the scope, written to an output key, timed and
        // reported to the listener.
        var file = new HouseholdFile();
        var guard = new NoteGuard();

        var writer = AgenticServices.agentBuilder(NoteFromFile.class)
                .chatModel(model)
                .name("NoteFromFile")
                .outputKey(Notes.class)
                .build();

        UntypedAgent app = AgenticServices.sequenceBuilder()
                .subAgents(file, writer, guard)
                .outputKey(Notes.class)
                .listener(listener)
                .build();
        var r = app.invokeWithAgenticScope(Map.of(new Stay().name(), input));
        return String.valueOf(r.result());
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // The two Java steps are NOT drawn as agents, for the same reason the person in demo 7
        // is not: an identical box either side would say the model did it, which is the one
        // thing this demo denies. They get the 'code' role and read as what they are.
        Topology.Graph topo = graph("stages",
                List.of(node("in", "the stay", "input", 0),
                        node("file", "HouseholdFile", "code", 1)
                                .withSub("no model · your records"),
                        node("write", "NoteFromFile", "agent", 2),
                        node("guard", "NoteGuard", "code", 3)
                                .withSub("no model · checks facts"),
                        node("out", "the note", "join", 4)),
                List.of(edge("in", "file"),
                        edge("file", "write", "facts"),
                        edge("write", "guard", "notes"),
                        edge("guard", "out")));
        return new PatternDef("nonAiAgent", "Non-AI Agents (plain Java)", "workflow",
                "Which is fine, as long as what she is told is true. Nobody should be "
                        + "inventing a microchip number, and a model asked for one has one.",
                "Demo 7 said a HumanInTheLoop is a non-AI agent. This is the general "
                        + "case: your own class, no model, same wiring.",
                "An agent does not have to be a model. Any object with one `@Agent` method goes "
                        + "straight into `subAgents(...)` — its `@K` parameters are bound from the "
                        + "scope and its return value is written to its output key, so **the "
                        + "sequence cannot tell**. Here Java is on both ends: the file supplies "
                        + "the numbers, the model writes the prose, and a plain `String::contains` "
                        + "checks the numbers survived. The test for a step is not \"could a model "
                        + "do this\" — it is \"is this judgement, or is it a lookup\". "
                        + "`AgenticServices.agentAction(scope -> …)` is the one-line lambda form.\n\n"
                        + "**Watch the diagram while it runs.** The middle box lights up and the "
                        + "two Java boxes never do — see the caveat. They are doing the work all "
                        + "the same: `facts` appears in the Scope tab, and the guard's finding is "
                        + "at the bottom of the result.",
                "**A non-AI agent is invisible to the listener** in `1.20.0-beta30`. "
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
                "my sister has Zao from Friday to Sunday and she has never looked after him "
                        + "before",
                NonAiAgentPattern::run);
    }
}
