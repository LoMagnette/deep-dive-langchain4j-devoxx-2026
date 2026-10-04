package dev.devoxx.dashboard.demos._08_nonaiagent;

import dev.devoxx.dashboard.demos._08_nonaiagent.Keys.Height;
import dev.devoxx.dashboard.demos._08_nonaiagent.Keys.LadderLength;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;

/**
 * A class, not an interface — and that is the whole lesson. No proxy, no prompt, no model:
 * {@code AgentUtil.nonAiAgentToExecutor} takes any object with one {@code @Agent} method, binds the
 * {@code @K} parameters from the scope and writes the return value to the output key, exactly as
 * it does for an LLM agent.
 *
 * <p>Ladder math is the job a model is worst at and Java is best at: a model asked for a ladder
 * length is plausible, and Rivet is right.
 */
public class Rivet {

    /** A ladder stands at 75°, and needs a metre above the branch to climb off safely. */
    private static final double ANGLE = Math.toRadians(75);
    private static final double OVERHANG = 1.0;

    @Agent(name = "Rivet",
           description = "Rivet the robot dog: computes the ladder length, in plain Java",
           typedOutputKey = LadderLength.class)
    public double ladderLength(@K(Height.class) double height) {
        double needed = height / Math.sin(ANGLE) + OVERHANG;
        // Ladders come in half metres; round UP — a ladder 10 cm short is not a ladder.
        return Math.ceil(needed * 2) / 2;
    }
}
