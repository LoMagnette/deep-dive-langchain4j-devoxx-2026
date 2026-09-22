package dev.devoxx.dashboard.demos._11_p2p;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface TeamOnTheBed {
    @Agent(description = "Argues the dog should be allowed on the bed, and will not just fold")
    @UserMessage("""
            You are the half of the household that wants the dog on the bed. Say why, in a
            short paragraph, and say the one thing you will not give up.

            Question: {{Question}}""")
    String propose(@K(Keys.Question.class) String question);
}
