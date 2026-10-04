package dev.devoxx.dashboard.demos._14_debate;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface FifiJudges {
    @Agent(name = "Fifi",
           typedOutputKey = Keys.Verdict.class,
           description = "Fifi the Poodle: hears the closing statements and declares the winner")
    @UserMessage("""
            You are Fifi, moderating the Barkville town council. The debate is over; below are
            the closing statements. Declare a winner — dog park or cat café — with exactly three
            reasons, each taken from something actually said. Nothing is ever perfect, so also
            name the winner's weakest moment. Plainly, no stage directions.

            The motion: {{Motion}}
            Closing statements: {{debateContext}}""")
    String judge(@K(Keys.Motion.class) String motion,
                 @K(Keys.DebateContext.class) String closing);
}
