package dev.devoxx.dashboard.demos._05_parallelmapper;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface SniffSearches {
    /**
     * {@code @V("duckling")} names nothing on the Pup Board: the mapper binds each item to the
     * agent's FIRST argument by position, so this is the one name in the demos that is not a key.
     */
    @Agent(description = "Sniff the Beagle: searches for one duckling, starting where it was last seen")
    @UserMessage("""
            Search for this one duckling, starting where it was last seen. Ducklings never get far:
            you always find it close by. One line: exactly where you found it, and what it was up
            to when you did.

            The duckling: {{duckling}}""")
    String search(@V("duckling") String duckling);
}
