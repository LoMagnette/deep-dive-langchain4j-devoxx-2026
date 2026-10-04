package dev.devoxx.dashboard.demos._14_debate;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

/** Marmalade the cat: the villain. Not a Ranger, but very much an AI agent. */
public interface MarmaladeArgues {
    @Agent(name = "Marmalade",
           typedOutputKey = Keys.MarmaladeTurn.class,
           description = "Marmalade the cat: argues for the cat café, and against everything dog")
    @UserMessage("""
            You are Marmalade the cat, at the Barkville town council, arguing FOR the cat café and
            against the dog park. Take your turn for this round. If the last round below is
            empty, the debate has just begun: open your case. Otherwise answer what Howl said,
            then make one new point. Two or three sentences, superior and on
            the subject.

            The motion: {{Motion}}
            Last round: {{debateContext}}""")
    String argue(@K(Keys.Motion.class) String motion,
                 @K(Keys.DebateContext.class) String lastRound);
}
