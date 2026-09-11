package dev.devoxx.dashboard.demos.bdi;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface FirstTraining {
    @Agent(description = "The first tiny training session, once he is out and fed")
    @UserMessage("""
            He has been out and he has eaten. Give the first tiny training session — one
            thing, two minutes, ending well. Two or three lines.

            Been out: {{out}}
            Eaten: {{fed}}""")
    String teach(@V("out") String out, @V("fed") String fed);
}
