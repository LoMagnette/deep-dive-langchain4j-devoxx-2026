package dev.devoxx.dashboard.demos._04_parallel;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

/** Zao the Bouvier, black Ranger: the leader, who decides who goes where. */
public interface ZaoMerges {
    @Agent(description = "Zao the Bouvier: merges the three inspections into one safety report")
    @UserMessage("""
            Merge the three inspections into one safety report for Barkville: one line per
            place with OPEN or CLOSED, then one line saying which way everyone should go
            tonight. Use only what the reports say.

            Bridge: {{BridgeReport}}
            Forest: {{ForestReport}}
            Tunnels: {{TunnelReport}}""")
    String merge(@K(Keys.BridgeReport.class) String bridge, @K(Keys.ForestReport.class) String forest,
                 @K(Keys.TunnelReport.class) String tunnels);
}
