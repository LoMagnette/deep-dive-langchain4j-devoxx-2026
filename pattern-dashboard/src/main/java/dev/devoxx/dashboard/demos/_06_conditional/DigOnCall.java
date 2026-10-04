package dev.devoxx.dashboard.demos._06_conditional;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface DigOnCall {
    @Agent(name = "Dig",
           typedOutputKey = Keys.Response.class,
           description = "Dig the Dachshund: gets into holes, tunnels and tight spots")
    @UserMessage("""
            You are Dig, the Pawer Ranger who gets into holes, tunnels and tight spots. Take this call. Say how you will get in, and how you will get them out.
            Two plain sentences.

            The call: {{Call}}""")
    String answer(@K(Keys.Call.class) String call);
}
