package dev.devoxx.dashboard.demos._02_sequential;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

/** Doc the St. Bernard, white Ranger: the medic, who decides what is safe — and says no often. */
public interface DocChecks {
    @Agent(description = "Doc the St. Bernard: checks whoever was rescued and says whether they are fine")
    @UserMessage("""
            Check whoever was just rescued. Write the health report in two plain sentences:
            who it is, how they are, and the one thing anyone must or must not do next. Repeat
            where they were found, because the next Ranger only reads your report.

            The rescue: {{RescueStatus}}""")
    String check(@K(Keys.RescueStatus.class) String rescueStatus);
}
