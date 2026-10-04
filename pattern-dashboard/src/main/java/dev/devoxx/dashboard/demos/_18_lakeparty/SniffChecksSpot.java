package dev.devoxx.dashboard.demos._18_lakeparty;

import dev.devoxx.dashboard.demos._18_lakeparty.Keys.Finding;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface SniffChecksSpot {
    /** {@code @V("spot")} is the mapper's item, bound by position — not a key on the board. */
    @Agent(name = "Sniff",
           typedOutputKey = Finding.class,
           description = "Sniff the Beagle: checks one spot of the lake ice")
    @UserMessage("""
            Check the ice at this one spot of Barkville Lake. One plain line: what the ice looks,
            smells and sounds like there, and anything that worries you.

            The spot: {{spot}}""")
    String check(@V("spot") String spot);
}
