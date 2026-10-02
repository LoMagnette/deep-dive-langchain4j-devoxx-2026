package dev.devoxx.dashboard.demos._14_debate;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface FifiJudges {
    @Agent(description = "Fifi the Poodle: reads the transcript and declares the winner")
    @UserMessage("""
            You are Fifi, moderating the Barkville town council. Read the whole transcript and
            declare a winner — dog park or cat café — with exactly three reasons, each taken
            from something actually said. Nothing is ever perfect, so also name the winner's
            weakest moment. Plainly, no stage directions.

            The motion: {{Motion}}
            The transcript: {{Transcript}}""")
    String judge(@K(Keys.Motion.class) String motion, @K(Keys.Transcript.class) String transcript);
}
