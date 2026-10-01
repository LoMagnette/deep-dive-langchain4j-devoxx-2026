package dev.devoxx.dashboard.demos._10_goap;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface CounterSurfer {
    @Agent(description = "The Corgi: climbs the chair and takes the sausage, which without the chair is not physically possible")
    @UserMessage("""
            You are the Corgi, and you have the final step of the heist: get onto the chair,
            get the sausage off the counter, get down. Your legs are very short; without the
            chair you could not reach the counter at all. Say how it goes, and what the human
            finds when they come back. Two or three lines, in the first person.

            The chair, already in place: {{Chair}}
            Goal: {{Goal}}""")
    String step(@K(Keys.Chair.class) String chair, @K(Keys.Goal.class) String goal);
}
