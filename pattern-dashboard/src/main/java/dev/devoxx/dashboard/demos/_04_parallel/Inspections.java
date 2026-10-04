package dev.devoxx.dashboard.demos._04_parallel;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.ParallelAgent;
import dev.langchain4j.agentic.declarative.ParallelExecutor;

/** The three-way fan-out, declared. */
public interface Inspections {

    @ParallelAgent(name = "Parallel",
                   subAgents = {ZoomChecksBridge.class, SniffChecksForest.class, DigChecksTunnels.class})
    String inspect(@K(Mission.class) String mission);

    /**
     * One thread per inspection. Shared by every run rather than made per run: the supplier is
     * called each time the system is built, and a pool per call that nobody shuts down is a
     * thread leak with a three-thread stride. Daemon threads, so the JVM can still exit.
     */
    ExecutorService PUPS = Executors.newFixedThreadPool(3, Thread.ofPlatform().daemon().name("pup-", 0).factory());

    @ParallelExecutor
    static Executor pups() {
        return PUPS;
    }
}
