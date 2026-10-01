package dev.devoxx.dashboard.demos._04_parallel;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface LeadDeveloper {
    @Agent(description = "The Greyhound: plans the lookout and leads the chase")
    @UserMessage("""
            Plan the lookout and the chase: which dog watches the squirrel's route, from where,
            and which dog leads the chase when it comes down — there are no humans in this plan,
            only the pack. Nobody goes over the fence and the cat is
            not a target. Use only the route the mission gives and invent nothing about the
            squirrel. Four short lines at most, no headings.

            The mission: {{Mission}}""")
    String plan(@V("Mission") String mission);
}
