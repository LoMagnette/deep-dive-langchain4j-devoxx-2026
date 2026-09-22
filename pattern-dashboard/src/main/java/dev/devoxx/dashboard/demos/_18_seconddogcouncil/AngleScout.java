package dev.devoxx.dashboard.demos._18_seconddogcouncil;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface AngleScout {
    @Agent(description = "Digs out what one angle of the household really says")
    @UserMessage("""
            Look at this household from one angle only. Say what it tells you and what is
            still unknown on it. Two sentences.

            Angle: {{angle}}""")
    String scout(@V("angle") String angle);
}
