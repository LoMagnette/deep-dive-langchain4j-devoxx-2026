package dev.devoxx.dashboard.demos._05_parallelmapper;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface BeardOverflow {
    @Agent(description = "The St Bernard: says whether one thing found in Zao's beard is a problem, and what to do")
    @UserMessage("""
            This came out of Zao's beard, and he may have eaten some of it. In one line,
            starting with "Dangerous" or "Fine": is it a problem for a dog, and what should the
            pack do — nothing, watch him, or wake the human for the vet now?

            Out of the beard: {{item}}""")
    String check(@V("item") String item);
}
