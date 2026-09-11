package dev.devoxx.dashboard.demos.humanapproval;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface FinalNote {
    @Agent(description = "Writes what actually happens, once a person has had their say")
    @UserMessage("""
            Write the final instruction, honouring the person's decision exactly. If they approved
            it, restate the plan as the thing to do. If they changed it, apply their change. If
            they refused it, say plainly that nothing is being given and what to do instead.

            Never reinstate anything they took out.

            The draft: {{draft}}
            What they said: {{decision}}""")
    String write(@V("draft") String draft, @V("decision") String decision);
}
