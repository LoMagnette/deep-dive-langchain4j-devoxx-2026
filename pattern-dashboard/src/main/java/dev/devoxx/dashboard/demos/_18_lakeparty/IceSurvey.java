package dev.devoxx.dashboard.demos._18_lakeparty;

import java.util.List;

import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.ParallelMapperAgent;

/**
 * The spot-by-spot mapper, nested as one step. {@code itemsProvider} is a string because the
 * annotation has no typed form of it — {@code "Spots"} is the record's name, so it is the same key.
 */
public interface IceSurvey {

    @ParallelMapperAgent(name = "ParallelMapper",
                         subAgent = SniffChecksSpot.class,
                         itemsProvider = "Spots",
                         typedOutputKey = Keys.Findings.class)
    List<String> survey(@K(Keys.Spots.class) List<String> spots);
}
