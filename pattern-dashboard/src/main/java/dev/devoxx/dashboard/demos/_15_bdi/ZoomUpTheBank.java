package dev.devoxx.dashboard.demos._15_bdi;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface ZoomUpTheBank {
    @Agent(description = "Zoom, squirrel plan step 1: chase it up the riverbank")
    @UserMessage("""
            You are Zoom the Greyhound, and you are chasing a squirrel: step one, chase it up
            the riverbank. From the top of the bank you can see across the river. Say what you
            see there in two plain lines. Unless the radio says the kid is already safe, you
            see that the bridge is out and a kid is STRANDED on the far bank — say exactly that.

            What the radio said: {{Beliefs}}""")
    String act(@K(Keys.Beliefs.class) String beliefs);
}
