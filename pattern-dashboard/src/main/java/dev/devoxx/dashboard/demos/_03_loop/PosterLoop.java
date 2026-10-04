package dev.devoxx.dashboard.demos._03_loop;

import static dev.devoxx.dashboard.support.Parsing.reviewScore;

import dev.devoxx.dashboard.run.CurrentRun;
import dev.langchain4j.agentic.declarative.AgentListenerSupplier;
import dev.langchain4j.agentic.declarative.ExitCondition;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.LoopAgent;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;

/**
 * The write-score loop, declared. The builder's six calls — sub-agents, max iterations, exit
 * condition, test-at-end, output key, listener — are the annotation's attributes and two static
 * methods, and nothing else moved.
 */
public interface PosterLoop {

    @LoopAgent(name = "Loop",
               subAgents = {HowlWrites.class, FifiScores.class},
               maxIterations = LoopPattern.TREATS,
               typedOutputKey = Keys.Draft.class)
    ResultWithAgenticScope<String> refine(@K(Keys.Brief.class) String brief, @K(Keys.Rules.class) String rules,
                                          @K(Keys.Feedback.class) String feedback);

    /**
     * The exit condition, as a static method instead of a lambda. The scope read is gone: the
     * framework binds {@code @K(Feedback.class)} the way it binds an agent's own parameters.
     * Parsing Fifi's prose into a number is still ours.
     */
    @ExitCondition(testExitAtLoopEnd = true, description = "Fifi scores it at least 0.8")
    static boolean goodEnough(@K(Keys.Feedback.class) String feedback) {
        return reviewScore(feedback) >= 0.8;
    }

    /** The run's listener. A static no-arg method is the only hook, hence {@link CurrentRun}. */
    @AgentListenerSupplier
    static AgentListener listener() {
        return CurrentRun.observers();
    }
}
