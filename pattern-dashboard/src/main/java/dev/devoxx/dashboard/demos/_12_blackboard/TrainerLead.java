package dev.devoxx.dashboard.demos._12_blackboard;

import dev.devoxx.dashboard.demos._04_parallel.Keys.Walks;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface TrainerLead {
    @Agent(description = "Reads the whole board and ranks the likely causes")
    @UserMessage("""
            You are the trainer. From everything on the board, give the two or three most
            likely causes, most likely first, and the one thing to try for each.

            Exercise: {{Walks}}
            What changed: {{Routine}}
            What he sees and hears: {{Home}}""")
    String conclude(@K(Walks.class) String walks, @K(Keys.Routine.class) String routine,
                    @K(Keys.Home.class) String home);
}
