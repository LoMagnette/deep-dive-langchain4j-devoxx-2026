package dev.devoxx.dashboard.demos._18_seconddogcouncil;

import java.util.List;

import dev.devoxx.dashboard.demos._18_seconddogcouncil.Keys.Angles;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;

/** The three-angle survey, as a real interface, not {@code UntypedAgent}. */
public interface CouncilSurvey {
    @Agent
    List<String> gather(@K(Angles.class) List<String> angles);
}
