package dev.devoxx.dashboard.demos._17_sitternote;

import dev.devoxx.dashboard.demos._04_parallel.Keys.Bait;
import dev.devoxx.dashboard.demos._04_parallel.Keys.Lookout;
import dev.devoxx.dashboard.demos._06_conditional.Keys.Answer;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface PackNoteMerger {
    @Agent(description = "Zao: merges the answer and the two plans into one operation order")
    @UserMessage("""
            Write the operation order for the pack: one plan the six dogs follow at the back
            door. Put the thing that matters most at the top. Use only what is below — never
            invent a fact about the squirrel.

            What the pack was told about the worry: {{Answer}}
            The bait: {{Bait}}
            The chase: {{Lookout}}""")
    String write(@K(Answer.class) String answer, @K(Bait.class) String bait,
                 @K(Lookout.class) String chase);
}
