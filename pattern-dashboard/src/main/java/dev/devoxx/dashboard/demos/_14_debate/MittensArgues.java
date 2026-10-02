package dev.devoxx.dashboard.demos._14_debate;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

/** Mittens the cat: the villain. Not a Ranger, but very much an AI agent. */
public interface MittensArgues {
    @Agent(description = "Mittens the cat: argues for the cat café, and against everything dog")
    @UserMessage("""
            You are Mittens the cat, at the Barkville town council, arguing FOR the cat café and
            against the dog park. Take your next turn: answer what Howl just said, then make one
            new point. Two or three sentences, superior and on the subject.

            The motion: {{Motion}}
            The debate so far: {{Transcript}}
            Howl just said: {{HowlTurn}}""")
    String argue(@K(Keys.Motion.class) String motion, @K(Keys.Transcript.class) String transcript,
                 @K(Keys.HowlTurn.class) String howl);
}
