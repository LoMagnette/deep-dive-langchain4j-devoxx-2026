package dev.devoxx.dashboard.demos._00_aiservice;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;

import java.util.List;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.run.AiServiceBridge;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.guardrail.InputGuardrailException;
import dev.langchain4j.guardrail.OutputGuardrailException;
import dev.langchain4j.guardrail.config.OutputGuardrailsConfig;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for <b>Mission 0</b> — one AI service, with tools and guardrails, and nothing agentic.
 */
public final class AiServicePattern {

    private AiServicePattern() {
    }

    /** How many times PawSized may send an answer back. */
    static final int MAX_REWRITES = 3;

    static String run(ChatModel model, String input, StreamingListener listener) {
        // How the page sees a plain AI service: its own observability API, bridged onto the
        // run's listener. The demo works without this line; the diagram would just stay dark.
        var bridge = new AiServiceBridge(listener, "Zao");

        // TODO live: AiServices.builder
        return "TODO live: AiServices.builder";
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // Guardrails and tools are plain Java around ONE model call, so they are drawn as code
        // boxes around a single agent-shaped box — which says "not an agent" on its second line,
        // because that is the point of starting here.
        Topology.Graph topo = graph("stages",
                List.of(node("in", "letter", "input", 0).withSub("to Pup HQ"),
                        node("in-g", "NoCatsAllowed", "code", 1).withSub("input guardrail"),
                        node("zao", "Zao", "agent", 2).withSub("AiServices · no agent").as("zao"),
                        node("t1", "rangerFor(job)", "code", 3).withSub("tool"),
                        node("t2", "onDuty(ranger)", "code", 3).withSub("tool"),
                        node("out-g", "PawSized", "code", 4).withSub("output guardrail"),
                        node("out", "the answer", "join", 5).withSub("fits the noticeboard")),
                List.of(edge("in", "in-g"),
                        edge("in-g", "zao", "unless a cat wrote it"),
                        edge("zao", "t1", "it decides"), edge("zao", "t2"),
                        edge("zao", "out-g", "draft"),
                        edge("out-g", "zao", "reprompt · too long"),
                        edge("out-g", "out", "≤ 50 words")));
        return new PatternDef("aiService", "AI Service (no agents yet)", "classic",
                "Before the pack, the front desk: Zao answers letters to Pup HQ. One pup, one "
                        + "model call, and a strict rule about cats.",
                null,
                "Plain LangChain4j: `AiServices.builder(PupHqDesk.class)` turns an interface into "
                        + "an LLM call — no agentic module at all. Three things on the builder: "
                        + "**`.tools(...)`**, which the model chooses to call (watch the Run events "
                        + "pane); **`.inputGuardrails(...)`**, which runs BEFORE the model and can "
                        + "stop the call entirely — send a letter signed by Marmalade and the model is "
                        + "never called; and **`.outputGuardrails(...)`**, which runs AFTER and can "
                        + "send the answer back with an instruction (`reprompt`) — a reply too long "
                        + "for the noticeboard comes back shorter. Notice what is missing: no Pup "
                        + "Board. An AI service has no scope; Mission 1's agent is where that starts.",
                "Guardrails are plain Java, so they only catch what you wrote down — keep them "
                        + "checks a reader can verify, not a second model's opinion. An input "
                        + "guardrail that FAILS throws (`InputGuardrailException`), so the caller has "
                        + "to expect it; an output reprompt costs another model call, and there is a "
                        + "retry limit after which it throws too. And the observability is a "
                        + "different API from the agents': `registerListeners(...)` on the AI "
                        + "service, bridged onto this page by `AiServiceBridge`.",
                topo,
                // Asks for "everything", so a live model writes far too much and PawSized sends it
                // back — the output guardrail fires on the first click. For the input guardrail,
                // sign a letter "Marmalade".
                "Dear Pup HQ, I have lost my reading glasses again. Please tell me everything: "
                        + "which Ranger can find them, all about how he works, his whole history "
                        + "with Barkville, and exactly when he can come. Leave nothing out! — The Mayor",
                AiServicePattern::run)
                .gist("One model call with tools and guardrails — no agents yet.");
    }
}
