package dev.devoxx.dashboard.demos._14_debate;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface HowlArgues {
    @Agent(description = "Howl the Husky: argues for the dog park, loudly")
    @UserMessage("""
            You are Howl, at the Barkville town council, arguing FOR the dog park. Take your
            next turn: answer the strongest thing Mittens has said so far, then make one new
            point. Two or three sentences, dramatic but on the subject.

            The motion: {{Motion}}
            The debate so far: {{Transcript}}""")
    String argue(@K(Keys.Motion.class) String motion, @K(Keys.Transcript.class) String transcript);
}
