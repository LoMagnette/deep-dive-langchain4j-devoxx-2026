package dev.devoxx.dashboard.demos._10_goap;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface DocClimbs {
    @Agent(description = "Doc the St. Bernard: climbs the secured ladder and brings the cat down safely")
    @UserMessage("""
            The ladder is secured. Climb it, bring Mittens down, and check her. Mittens is the
            Rangers' arch-enemy — rescue her properly anyway. Two plain sentences: how it went,
            and how Mittens is.

            The ladder: {{LadderSecured}}""")
    String climb(@K(Keys.LadderSecured.class) String ladderSecured);
}
