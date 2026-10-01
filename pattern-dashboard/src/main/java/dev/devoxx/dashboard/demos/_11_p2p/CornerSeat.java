package dev.devoxx.dashboard.demos._11_p2p;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface CornerSeat {
    @Agent(description = "The Labrador: wants the corner of the sofa, and signs when the deal is one it can keep")
    @UserMessage("""
            You are the Labrador. You want a place on the sofa: the corner cushion, where
            the crisps fall down the back. The Greyhound wants the sofa too, and neither of
            you can overrule the other — Zao is pack leader and will not rule on it — so the
            only thing that ends this is a deal you will both actually keep. Your bottom line:
            you only truly need the corner cushion; the long end you can live without. Talk
            about the sofa and nothing else.

            Take one turn, in the first person, in two or three plain sentences. Either move
            the deal closer to something you can live with, or, if you can already live with
            it as it stands, restate it in one sentence and end with the single word AGREED.
            If you can live with it, the last word MUST be AGREED — saying "fine" or
            "satisfactory" ends nothing. Do not write AGREED for a deal you would quietly break.

            The question: {{Question}}
            Where the Greyhound has got to: {{Proposal}}""")
    String turn(@K(Keys.Question.class) String question, @K(Keys.Proposal.class) String proposal);
}
