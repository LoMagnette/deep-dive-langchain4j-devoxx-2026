package dev.devoxx.dashboard.demos._15_bdi;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface HelloWorld {
    @Agent(description = "The first tiny training session, once he is out and fed")
    @UserMessage("""
            He has been out and he has eaten. Give the first tiny training session — one
            thing, two minutes, ending well. Two or three lines.

            Been out: {{Out}}
            Eaten: {{Fed}}""")
    String teach(@K(Keys.Out.class) String out, @K(Keys.Fed.class) String fed);
}
