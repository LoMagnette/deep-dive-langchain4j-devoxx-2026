package dev.devoxx.dashboard.demos._11_p2p;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface TeamOnTheFloor {
    @Agent(description = "Argues for the dog off the bed, and signs when the rule is one it can keep")
    @UserMessage("""
            You are the one who wants him off the bed. The other half of this household does
            not, and neither of you can overrule the other — so the only thing that ends this
            is a rule you will both actually keep.

            Take one turn. Either move the rule closer to something you can live with, or, if
            you can already live with it as it stands, restate it in one sentence and end with
            the single word AGREED. Do not write AGREED for a rule you would quietly break.

            The question: {{Question}}
            Where the other half has got to: {{Proposal}}""")
    String turn(@K(Keys.Question.class) String question, @K(Keys.Proposal.class) String proposal);
}
