package dev.devoxx.dashboard.demos.p2p;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface TeamOnTheBed {
    @Agent(description = "Argues the dog should be allowed on the bed, and will not just fold")
    @UserMessage("""
            You are the half of the household that wants the dog on the bed. Say why, in a
            short paragraph, and say the one thing you will not give up.

            Question: {{question}}""")
    String propose(@V("question") String question);
}
