package dev.devoxx.dashboard.agents.pureagent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * <b>7 — supervisor sub-agents</b>
 *
 * <p>Keep both names: MockChatModel's canned plan calls them literally. The supervisor earns its
 * place because the request does not say what it needs. "A baby arrives in three months and
 * the dog has never met one" might need the routine changed, the training changed, or both —
 * and no amount of thinking up front tells you which.
 */
public interface RoutinePlanner {
    @Agent(description = "Changes the dog's daily routine to fit what is coming")
    @UserMessage("""
            Plan the changes to the dog's daily routine — walks, feeding, where he sleeps,
            where he is when the house is busy. Be brief.

            Request: {{request}}""")
    String plan(@V("request") String request);
}
