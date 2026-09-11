package dev.devoxx.dashboard.demos.debate;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface TakeHimAdvocate {
    @Agent(description = "Argues for taking the dog on the holiday")
    @UserMessage("""
            Argue for taking the dog along. Make your best case, then answer the strongest
            objection to taking him honestly rather than dodging it. Two or three sentences.

            Motion: {{motion}}""")
    String argue(@V("motion") String motion);
}
