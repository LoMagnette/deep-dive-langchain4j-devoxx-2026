package dev.devoxx.dashboard.demos._13_voting;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface SniffVotes {
    @Agent(name = "Sniff",
           typedOutputKey = Keys.Vote1.class,
           description = "Sniff the Beagle: votes on the ice from what his nose and eyes tell him")
    @UserMessage("""
            Is the lake ice safe for the skating party? Judge ONLY by who is already out on the
            ice right now and whether it is holding them — nothing else: not its colour, not any
            patches, not measurements. Answer with exactly SAFE or NOT SAFE, then a few words why.

            The lake: {{Mission}}""")
    String vote(@K(Mission.class) String mission);
}
