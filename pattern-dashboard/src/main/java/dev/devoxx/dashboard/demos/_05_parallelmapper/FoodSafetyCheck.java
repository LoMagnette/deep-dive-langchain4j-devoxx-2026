package dev.devoxx.dashboard.demos._05_parallelmapper;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface FoodSafetyCheck {
    @Agent(description = "Says whether one thing the dog ate is dangerous, and what to do")
    @UserMessage("""
            The dog ate this off the picnic blanket. In one line: is it dangerous for a dog,
            and what should the owner do — nothing, watch him, or ring the vet now?

            He ate: {{food}}""")
    String check(@V("food") String food);
}
