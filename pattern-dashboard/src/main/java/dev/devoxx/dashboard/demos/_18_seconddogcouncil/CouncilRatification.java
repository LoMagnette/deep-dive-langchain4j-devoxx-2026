package dev.devoxx.dashboard.demos._18_seconddogcouncil;

import dev.langchain4j.agentic.Agent;

/**
 * The ratifying vote, as a real interface — the three assessors read the household from the
 * scope. No {@code .name(...)} on the builder — like every other {@code plannerBuilder()} demo,
 * the wrapper reports itself under this method's name, and the tests filter that noise out
 * under "invoke".
 */
public interface CouncilRatification {
    @Agent
    String invoke();
}
