package dev.devoxx.dashboard.demos._14_debate;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;

/**
 * Bolt keeps the minutes. An agent's output OVERWRITES its key, so a transcript that grows needs
 * somebody whose job is appending — and the record of what was said is exactly the thing a model
 * must not paraphrase. Plain Java, one round per call.
 */
public class BoltMinutes {

    @Agent(name = "Bolt",
           description = "Bolt the robot dog: appends the round to the transcript, word for word",
           typedOutputKey = Keys.Transcript.class)
    public String minute(@K(Keys.Transcript.class) String transcript,
                         @K(Keys.HowlTurn.class) String howl,
                         @K(Keys.MittensTurn.class) String mittens) {
        boolean first = transcript == null || transcript.startsWith("(");
        int round = first ? 1 : (int) transcript.lines().filter(l -> l.startsWith("Round ")).count() + 1;
        return (first ? "" : transcript + "\n\n")
                + "Round " + round + "\nHowl: " + howl.strip() + "\nMittens: " + mittens.strip();
    }
}
