package dev.devoxx.dashboard.demos.conditional;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface EmergencyVet {
    @Agent(description = "Answers when the dog may be in danger right now")
    @UserMessage("""
            You are the emergency vet on the telephone. Say what the owner must do in the next
            ten minutes, and whether this is a get-in-the-car-now case. Be brief.

            Worry: {{worry}}""")
    String handle(@V("worry") String worry);
}
