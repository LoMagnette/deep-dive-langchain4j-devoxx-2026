package dev.devoxx.dashboard.demos._07_humanapproval;

import dev.langchain4j.agentic.Agent;
import dev.devoxx.dashboard.demos._06_conditional.Keys.Answer;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface FinalNote {
    @Agent(name = "FinalNote", description = "Writes what the sitter is told, once a person has had their say",
           typedOutputKey = Keys.Instruction.class)
    @UserMessage("""
            Write what the sitter is actually told to do, honouring the person's decision
            exactly. If they approved it, restate it as the thing to do. If they changed it,
            apply their change. If they refused it, say plainly that it is not being done and
            what to do instead.

            Never reinstate anything they took out.

            The draft: {{Answer}}
            What they said: {{Decision}}""")
    String write(@K(Answer.class) String draft, @K(Keys.Decision.class) String decision);
}
