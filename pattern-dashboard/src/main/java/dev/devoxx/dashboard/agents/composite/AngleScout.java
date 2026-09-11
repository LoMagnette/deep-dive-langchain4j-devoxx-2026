package dev.devoxx.dashboard.agents.composite;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * <b>16 — the second-dog council</b>
 *
 * <p>The same question the voting demo asks, but put through a whole council — so the room has
 * already met the three assessors and can watch them ratify a debated motion instead of voting
 * cold. Both agents below are "glue": each exists to hand one pattern's output to the next in
 * the shape that one expects. Composites need more of these than you expect, and they are
 * where the seams show.
 */
public interface AngleScout {
    @Agent(description = "Digs out what one angle of the household really says")
    @UserMessage("""
            Look at this household from one angle only. Say what it tells you and what is
            still unknown on it. Two sentences.

            Angle: {{angle}}""")
    String scout(@V("angle") String angle);
}
