package dev.devoxx.dashboard.demos._07_humanapproval;

import static java.util.Objects.requireNonNullElse;

import dev.devoxx.dashboard.demos._07_humanapproval.Keys.DigPlan;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.langchain4j.agentic.declarative.HumanInTheLoop;
import dev.langchain4j.agentic.declarative.K;

/**
 * Officer Jo, declared: a static method the framework calls when the sequence reaches her, with
 * Dig's plan bound from the scope like any agent's input. What it returns is her answer.
 *
 * <p><b>Two things the builder form did not need.</b> The person to ask is per run, and a static
 * method has no argument to receive it, so it is reached through {@link CurrentRun} — this one
 * at INVOCATION time, which works because a sequence runs its steps on the caller's thread.
 * And the output key is a string: {@code @HumanInTheLoop} declares {@code typedOutputKey}, but
 * {@code AgenticServices.createHumanInTheLoopAgent} reads only {@code outputKey()} in
 * {@code 1.20.0-beta30}, so a typed key here is silently ignored and Dig never hears her answer.
 * {@code "Approved"} is the record's name, so it is the same key {@link DigActs} reads.
 */
public interface OfficerJo {

    @HumanInTheLoop(name = "OfficerJo",
                    description = "Officer Jo, who runs Pup HQ and approves anything risky",
                    outputKey = "Approved")
    static String approve(@K(DigPlan.class) String plan) {
        return CurrentRun.listener().askHuman("OfficerJo", """
                Dig wants to tunnel under the Mayor's prize roses. Yes, no, or yes-but? \
                Nothing is dug until you say.

                """ + requireNonNullElse(plan, ""));
    }
}
