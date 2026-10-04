package dev.devoxx.dashboard.demos._10_goap;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface DocClimbs {
    @Agent(name = "Doc",
           typedOutputKey = Keys.CatSafe.class,
           description = "Doc the St. Bernard: climbs the secured ladder and brings the cat down safely")
    @UserMessage("""
            The ladder is secured. Climb it, bring Marmalade down, and check her. Marmalade is the
            Rangers' arch-enemy — rescue her properly anyway. Two plain sentences: how it went,
            and how Marmalade is.

            The ladder: {{LadderSecured}}""")
    String climb(@K(Keys.LadderSecured.class) String ladderSecured);
}
