package dev.devoxx.dashboard.demos._05_parallelmapper;

import java.util.List;

import dev.devoxx.dashboard.demos._05_parallelmapper.Keys.Beard;
import dev.devoxx.dashboard.demos._05_parallelmapper.Keys.Verdicts;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.langchain4j.agentic.declarative.AgentListenerSupplier;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.ParallelMapperAgent;
import dev.langchain4j.agentic.observability.AgentListener;

/**
 * One agent, mapped over a collection. Note {@code subAgent} is singular here where every other
 * composition annotation takes {@code subAgents} — the width of this fan-out is the data, not the
 * declaration, which is the whole distinction between this and demo 4.
 */
public interface ParallelMapperSystem {

    @ParallelMapperAgent(name = "ParallelMapper",
                         subAgent = BeardOverflow.class,
                         itemsProvider = "Beard",
                         typedOutputKey = Verdicts.class)
    List<String> check(@K(Beard.class) List<String> beard);

    @AgentListenerSupplier
    static AgentListener listener() {
        return CurrentRun.listener();
    }
}
