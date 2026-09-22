package dev.devoxx.dashboard.demos._05_parallelmapper;

import dev.devoxx.dashboard.demos._14_debate.Keys.Verdict;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface BeardOverflow {
    @Agent(name = "BeardOverflow", description = "Says whether one thing found in the dog's beard is a problem, and what to do",
           typedOutputKey = Verdict.class)
    @UserMessage("""
            This came out of the dog's beard. In one line: is it a problem for a dog, and what
            should the owner do — nothing, watch him, or ring the vet now?

            Out of the beard: {{item}}""")
    String check(@V("item") String item);
}
