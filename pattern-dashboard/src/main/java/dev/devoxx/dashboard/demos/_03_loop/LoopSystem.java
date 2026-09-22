package dev.devoxx.dashboard.demos._03_loop;

import dev.devoxx.dashboard.demos._01_single.Keys.Notes;
import dev.devoxx.dashboard.demos._02_sequential.FridgeMagnet;
import dev.devoxx.dashboard.demos._03_loop.Keys.Score;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.devoxx.dashboard.support.Parsing;
import dev.langchain4j.agentic.declarative.AgentListenerSupplier;
import dev.langchain4j.agentic.declarative.ExitCondition;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.LoopAgent;
import dev.langchain4j.agentic.observability.AgentListener;

/**
 * The loop, declared rather than built. Compare with the builder form this replaced:
 *
 * <pre>{@code
 * AgenticServices.loopBuilder()
 *         .subAgents(writer, check)          // each one its own agentBuilder(...) chain
 *         .maxIterations(5)
 *         .exitCondition(good)
 *         .testExitAtLoopEnd(true)
 *         .outputKey(Notes.class)
 *         .listener(listener)
 *         .build();
 * }</pre>
 *
 * <p>Same six facts, moved from a call chain onto an annotation. The sub-agents are named by
 * <b>class</b>, which is why demo 2's {@link FridgeMagnet} is reused here by naming it and
 * nothing else — there is no second {@code agentBuilder} chain repeating its model, its name and
 * its output key.
 *
 * <p>Two things this form costs, both visible below. The exit predicate becomes a static method
 * rather than a lambda at the call site, which is fine. The <b>listener</b> is the awkward one —
 * see {@link CurrentRun}.
 */
public interface LoopSystem {

    // name = "Loop" is not decoration. A composed agent's default name is the METHOD name, so
    // without it this step reports as "refine" — the same trap .name("X") existed for on the
    // builder, one level up. Every name the diagram and the tests use has to be stated here now.
    @LoopAgent(name = "Loop",
               subAgents = {FridgeMagnet.class, RuffDraftCritic.class},
               maxIterations = 5,
               typedOutputKey = Notes.class)
    String refine(@K(Notes.class) String notes);

    /**
     * The exit condition, as a static method instead of a {@code Predicate} handed to a builder.
     *
     * <p>Note what happened to the scope read: it is gone. The builder form was
     * {@code scope -> Parsing.score(scope.readState(Score.class)) >= 0.8}, and this form injects
     * the value the same way an agent's own parameters are injected — so {@code @K} is doing the
     * reading. Parsing a critic's prose into a number is still ours; the scope access is now the
     * framework's, which is arguably where it belonged.
     */
    @ExitCondition(testExitAtLoopEnd = true, description = "at least 3 of the 4 rules hold")
    static boolean goodEnough(@K(Score.class) String score) {
        return Parsing.score(score) >= 0.8;
    }

    /** See {@link CurrentRun}: a static no-arg supplier is the only hook the API offers here. */
    @AgentListenerSupplier
    static AgentListener listener() {
        return CurrentRun.listener();
    }
}
