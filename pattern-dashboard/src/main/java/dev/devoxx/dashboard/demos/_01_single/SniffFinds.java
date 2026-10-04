package dev.devoxx.dashboard.demos._01_single;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.ToolsSupplier;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

/**
 * Sniff the Beagle, blue Ranger: finds things. The same agent works Mission 2's kitten and
 * Mission 17's Mega Mutt, so the prompt is about finding, never about hats.
 */
public interface SniffFinds {
    @Agent(name = "Sniff",
           typedOutputKey = Keys.Location.class,
           description = "Sniff the Beagle: finds whatever the mission is looking for, with his nose")
    @UserMessage("""
            Find what this mission is looking for. You have a nose: sniff the places the
            mission mentions, and follow any trail you pick up until it ends. Only report what
            your nose actually told you. Answer in one sentence: where it is now, and if it is up
            high, how high in metres, as a number.

            Mission: {{Mission}}""")
    String find(@K(Keys.Mission.class) String mission);

    /** His gear, chosen by the model: the declarative form of {@code .tools(new SniffGear())}. */
    @ToolsSupplier
    static Object gear() {
        return new SniffGear();
    }
}
