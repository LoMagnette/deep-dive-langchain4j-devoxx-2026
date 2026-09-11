package dev.devoxx.dashboard.agents.zoo;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface TrainerLead {
    @Agent(description = "Reads the whole board and ranks the likely causes")
    @UserMessage("""
            You are the trainer. From everything on the board, give the two or three most
            likely causes, most likely first, and the one thing to try for each.

            Exercise: {{walks}}
            What changed: {{routine}}
            What he sees and hears: {{home}}""")
    String conclude(@V("walks") String walks, @V("routine") String routine,
                    @V("home") String home);
}
