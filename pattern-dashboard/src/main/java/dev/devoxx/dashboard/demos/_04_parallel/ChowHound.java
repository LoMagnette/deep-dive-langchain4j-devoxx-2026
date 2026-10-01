package dev.devoxx.dashboard.demos._04_parallel;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ChowHound {
    @Agent(description = "The Labrador: plans the bait for the squirrel trap, and does not eat it")
    @UserMessage("""
            Plan the bait for the squirrel trap: what to use from the kitchen, where on the
            squirrel's route it goes, and which dog guards it so the Labrador does not eat it —
            there are no humans in this plan, only the pack. Only things that are safe for a dog
            if somebody does eat them. Use only the route the
            mission gives and invent nothing about the squirrel. Four short lines at most, no
            headings.

            The mission: {{Mission}}""")
    String plan(@V("Mission") String mission);
}
