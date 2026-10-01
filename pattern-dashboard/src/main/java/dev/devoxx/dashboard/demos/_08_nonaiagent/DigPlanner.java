package dev.devoxx.dashboard.demos._08_nonaiagent;

import dev.devoxx.dashboard.demos._08_nonaiagent.Keys.Mission;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

/**
 * The one step here that genuinely needs a model: turning a diary into a plan a pack can
 * follow with its paws. It is given the locations rather than asked for them, which is the
 * arrangement the whole demo is arguing for.
 */
public interface DigPlanner {
    @Agent(description = "The Beagle: writes the dig plan from the cat's diary")
    @UserMessage("""
            You are the Beagle. Write the dig plan for the pack from the cat's diary below:
            which dog digs where, and in what order — whatever the humans asked for comes
            first. Use the diary's locations exactly as they are written — copy them, do not
            reword them. Say nothing the diary does not contain. Under 120 words.

            The diary:
            {{Facts}}

            What the humans want: {{Mission}}""")
    String write(@K(Keys.Facts.class) String facts, @K(Mission.class) String mission);
}
