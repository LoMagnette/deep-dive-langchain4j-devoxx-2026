package dev.devoxx.dashboard.demos._12_blackboard;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface DigTunnels {
    @Agent(name = "Dig",
           typedOutputKey = Keys.TunnelClue.class,
           description = "Dig the Dachshund: crawls the drain Sniff's scent went down")
    @UserMessage("""
            You are Dig, a dachshund on the Pawer Rangers. You have just crawled the drain Sniff
            found. Report it in two short plain sentences, no headings: the paw prints in there
            are small, four-toed, with no claw marks — a cat's, not a dog's — and they come up
            in the garden of number 9, where Marmalade the cat lives.

            Sniff's clue: {{ScentClue}}""")
    String clue(@K(Keys.ScentClue.class) String scent);
}
