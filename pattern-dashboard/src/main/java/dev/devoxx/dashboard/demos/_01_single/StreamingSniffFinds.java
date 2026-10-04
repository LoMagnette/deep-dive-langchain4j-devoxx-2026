package dev.devoxx.dashboard.demos._01_single;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.agentic.declarative.ToolsSupplier;
import dev.langchain4j.agentic.declarative.StreamingChatModelSupplier;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * {@link SniffFinds} with one thing changed: it returns a {@link TokenStream}. The prompt is
 * copied word for word, deliberately — the toggle on the page is only honest if the two runs
 * differ in nothing but how the answer arrives. The gear works the same either way: the tool
 * calls happen first, and only the final sentence streams.
 */
public interface StreamingSniffFinds {
    @Agent(name = "Sniff",
           typedOutputKey = Keys.Location.class,
           description = "Sniff the Beagle: finds whatever the mission is looking for, with his nose")
    @UserMessage("""
            Find what this mission is looking for. You have a nose: sniff the places the
            mission mentions, and follow any trail you pick up until it ends. Only report what
            your nose actually told you. Answer in one sentence: where it is now, and if it is up
            high, how high in metres, as a number.

            Mission: {{Mission}}""")
    TokenStream find(@K(Keys.Mission.class) String mission);

    /** His gear, chosen by the model: the declarative form of {@code .tools(new SniffGear())}. */
    @ToolsSupplier
    static Object gear() {
        return new SniffGear();
    }

    /**
     * The streaming model this run was asked for. A static supplier has no arguments, so the
     * run's model reaches it through {@link CurrentRun} — see there for why.
     */
    @StreamingChatModelSupplier
    static StreamingChatModel model() {
        return CurrentRun.listener().streamingModel();
    }
}
