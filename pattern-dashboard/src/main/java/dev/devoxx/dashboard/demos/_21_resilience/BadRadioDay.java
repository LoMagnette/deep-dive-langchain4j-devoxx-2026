package dev.devoxx.dashboard.demos._21_resilience;

import static java.util.Objects.requireNonNullElse;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.devoxx.dashboard.demos._01_single.SniffFinds;
import dev.devoxx.dashboard.demos._02_sequential.Keys.RescueStatus;
import dev.devoxx.dashboard.demos._02_sequential.ZoomRescues;
import dev.devoxx.dashboard.demos._21_resilience.Keys.Attempts;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.langchain4j.agentic.agent.ErrorContext;
import dev.langchain4j.agentic.agent.ErrorRecoveryResult;
import dev.langchain4j.agentic.declarative.AgentListenerSupplier;
import dev.langchain4j.agentic.declarative.ErrorHandler;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;

/**
 * Sniff → Doc (maybe) → Zoom, declared. Doc's {@code optional = true} is on his own {@code @Agent}
 * ({@link DocFirstAid}); {@code injuries} is passed as {@code null} when nobody is hurt, and a
 * missing argument still reads as missing, so the step is still skipped.
 */
public interface BadRadioDay {

    @SequenceAgent(name = "Sequential",
                   subAgents = {SniffFinds.class, DocFirstAid.class, ZoomRescues.class},
                   typedOutputKey = RescueStatus.class)
    ResultWithAgenticScope<String> rescue(@K(Mission.class) String mission, @K(Keys.Injuries.class) String injuries);

    /**
     * The declarative {@code errorHandler(...)}: on the workflow, so it sees every failure in the
     * run. The counter is not decoration — RETRY re-executes the agent and a second failure comes
     * straight back here, so a handler that always retries never terminates. It lives on the
     * board because a static method has nothing to close over.
     */
    @ErrorHandler
    static ErrorRecoveryResult radioDropped(ErrorContext error) {
        AgenticScope scope = error.agenticScope();
        int attempts = requireNonNullElse(scope.readState(Attempts.class), 0) + 1;
        scope.writeState(Attempts.class, attempts);
        return attempts <= ResiliencePattern.MAX_RETRIES
                ? ErrorRecoveryResult.retry()
                : ErrorRecoveryResult.result("(Sniff's radio is down — no location)");
    }

    /** The run's listener. A static no-arg method is the only hook, hence {@link CurrentRun}. */
    @AgentListenerSupplier
    static AgentListener listener() {
        return CurrentRun.observers();
    }
}
