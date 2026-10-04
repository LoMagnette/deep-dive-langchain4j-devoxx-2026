package dev.devoxx.dashboard.demos._16_customplanner;

import dev.devoxx.dashboard.demos._06_conditional.DigOnCall;
import dev.devoxx.dashboard.demos._06_conditional.DocOnCall;
import dev.devoxx.dashboard.demos._06_conditional.SniffOnCall;
import dev.devoxx.dashboard.demos._06_conditional.ZoomOnCall;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.langchain4j.agentic.declarative.AgentListenerSupplier;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.PlannerAgent;
import dev.langchain4j.agentic.declarative.PlannerSupplier;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.planner.Planner;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;

/**
 * A day at Pup HQ, run by Zao's rule, declared. The planner is our own class — a
 * {@code @PlannerSupplier} takes a hand-written {@link NapSchedule} exactly as it takes the
 * library's GOAP or BDI planners.
 */
public interface DaysWork {

    @PlannerAgent(subAgents = {SniffOnCall.class, ZoomOnCall.class, DigOnCall.class, DocOnCall.class},
                  typedOutputKey = Keys.Schedule.class)
    ResultWithAgenticScope<String> invoke(@K(Keys.Roster.class) String roster);

    @PlannerSupplier
    static Planner planner() {
        return new NapSchedule();
    }

    /** The run's listener. A static no-arg method is the only hook, hence {@link CurrentRun}. */
    @AgentListenerSupplier
    static AgentListener listener() {
        return CurrentRun.observers();
    }
}
