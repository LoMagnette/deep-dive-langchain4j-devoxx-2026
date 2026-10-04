package dev.devoxx.dashboard.demos._07_humanapproval;

import dev.devoxx.dashboard.demos._02_sequential.Keys.RescueStatus;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface DigActs {
    @Agent(name = "DigActs",
           typedOutputKey = RescueStatus.class,
           description = "Dig the Dachshund: digs if Officer Jo said yes, and finds another way if she said no")
    @UserMessage("""
            Officer Jo has answered. Honour her answer exactly. If she said yes, dig as planned
            and say how the rescue went. If she said yes-but, dig with her change. If she said
            no, do not dig at all: say plainly that you are not digging, and give one other way
            to get the animal out. Never dig anything she ruled out. Two plain sentences.

            Your plan: {{DigPlan}}
            What Officer Jo said: {{Approved}}""")
    String act(@K(Keys.DigPlan.class) String plan, @K(Keys.Approved.class) String approved);
}
