package dev.devoxx.dashboard.agents.composite;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface NoteTightener {
    @Agent(description = "Tightens a sitter note without dropping any instruction")
    @UserMessage("""
            Tighten this note so it satisfies all four rules, without dropping any
            instruction: every meal has a time and an amount, it says where the lead and poo
            bags are, it gives the vet's telephone number, and it is under 100 words so it
            fits on the fridge door.

            Note: {{note}}""")
    String tighten(@V("note") String note);
}
