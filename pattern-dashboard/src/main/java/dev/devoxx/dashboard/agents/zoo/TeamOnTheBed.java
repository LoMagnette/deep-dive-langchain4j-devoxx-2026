package dev.devoxx.dashboard.agents.zoo;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * <b>9 — the household argument</b>
 *
 * <p>Two peers with opposed positions and NO authority over each other, which is the one
 * situation where peer-to-peer beats a supervisor: nobody can be told to give way, so the only
 * way out is a rule both will actually keep. Every household has had this argument.
 */
public interface TeamOnTheBed {
    @Agent(description = "Argues the dog should be allowed on the bed, and will not just fold")
    @UserMessage("""
            You are the half of the household that wants the dog on the bed. Say why, in a
            short paragraph, and say the one thing you will not give up.

            Question: {{question}}""")
    String propose(@V("question") String question);
}
