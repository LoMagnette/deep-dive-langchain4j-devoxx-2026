package dev.devoxx.dashboard.demos._18_lakeparty;

import java.util.List;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;

/** The spot-by-spot mapper, nested as one step. */
public interface IceSurvey {
    @Agent
    List<String> survey(@K(Keys.Spots.class) List<String> spots);
}
