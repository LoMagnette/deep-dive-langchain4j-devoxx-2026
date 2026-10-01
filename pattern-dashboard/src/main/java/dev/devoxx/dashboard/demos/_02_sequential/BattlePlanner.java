package dev.devoxx.dashboard.demos._02_sequential;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * <b>Sequential</b>
 */
public interface BattlePlanner {
    @Agent(description = "The Border Collie: turns what we know into the battle plan for the back door")
    @UserMessage("""
            Turn this into the battle plan for the back door: one line per dog, in the order
            they move, nothing a dog has to work out for itself. The pack is Zao, the Beagle,
            the Labrador, the Dachshund, the Greyhound and the Corgi. No title, no headings, and
            never invent a fact about the squirrel that the notes do not give. All four rules
            must hold:
            1. every dog in the pack has a position
            2. nobody goes over the fence
            3. the cat is not a target
            4. under 100 words, short enough to bark

            What we know so far: {{Notes}}""")
    String plan(@V("Notes") String notes);
}
