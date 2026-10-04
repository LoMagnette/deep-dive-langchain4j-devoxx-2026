package dev.devoxx.dashboard.demos._01_single;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Sniff the Beagle, blue Ranger: finds things. The same agent works Mission 2's kitten and
 * Mission 17's Mega Mutt, so the prompt is about finding, never about hats.
 */
public interface SniffFinds {
    @Agent(description = "Sniff the Beagle: finds whatever the mission is looking for, with his nose")
    @UserMessage("""
            Find what this mission is looking for. You have a nose: sniff the places the
            mission mentions, and follow any trail you pick up until it ends. Only report what
            your nose actually told you. Answer in one sentence: where it is now, and if it is up
            high, how high in metres, as a number.

            Mission: {{Mission}}""")
    String find(@V("Mission") String mission);
}
