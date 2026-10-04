package dev.devoxx.dashboard.demos._18_lakeparty;

import java.util.List;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.langchain4j.agentic.declarative.AgentListenerSupplier;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;

/**
 * The whole party decision, as one agent: a mapper, plain-Java glue, a planner and a Ranger, in
 * a row. Two of the four steps are themselves declared systems.
 */
public interface LakeParty {

    @SequenceAgent(name = "Sequential",
                   subAgents = {IceSurvey.class,      // 1. one Sniff, once per spot
                                IceReport.class,      // 2. the findings, written into the mission
                                IceBallot.class,      // 3. Mission 13's vote, unchanged
                                HowlAnnounces.class}, // 4. Howl tells the town
                   typedOutputKey = Keys.Announcement.class)
    ResultWithAgenticScope<String> decide(@K(Mission.class) String mission,
                                          @K(Keys.Spots.class) List<String> spots);

    /** The run's listener. A static no-arg method is the only hook, hence {@link CurrentRun}. */
    @AgentListenerSupplier
    static AgentListener listener() {
        return CurrentRun.observers();
    }
}
