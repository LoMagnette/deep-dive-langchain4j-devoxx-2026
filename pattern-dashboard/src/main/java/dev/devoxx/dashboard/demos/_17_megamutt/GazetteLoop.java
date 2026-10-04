package dev.devoxx.dashboard.demos._17_megamutt;

import static dev.devoxx.dashboard.support.Parsing.reviewScore;

import dev.devoxx.dashboard.demos._03_loop.FifiScores;
import dev.devoxx.dashboard.demos._03_loop.HowlWrites;
import dev.devoxx.dashboard.demos._03_loop.Keys.Feedback;
import dev.devoxx.dashboard.demos._03_loop.LoopPattern;
import dev.langchain4j.agentic.declarative.ExitCondition;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.LoopAgent;

/**
 * Mission 3's loop, nested: an agent inside the sequence, like any other step. Being declared, it
 * is named in {@link MegaMutt}'s {@code subAgents} by class — exactly as a Ranger is.
 */
public interface GazetteLoop {

    @LoopAgent(name = "Loop",
               subAgents = {HowlWrites.class, FifiScores.class},
               maxIterations = LoopPattern.TREATS)
    String polish();

    @ExitCondition(testExitAtLoopEnd = true, description = "Fifi scores it at least 0.8")
    static boolean goodEnough(@K(Feedback.class) String feedback) {
        return reviewScore(feedback) >= 0.8;
    }
}
