package dev.devoxx.dashboard.demos._18_lakeparty;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface HowlAnnounces {
    @Agent(name = "Howl",
           typedOutputKey = Keys.Announcement.class,
           description = "Howl the Husky: announces the skating party to Barkville — on, or off")
    @UserMessage("""
            Announce the skating party to the whole of Barkville, from the verdict below: on, or
            off, and why. Be as dramatic as you like, but the verdict is final. Three sentences.

            The verdict: {{IceVerdict}}""")
    String announce(@K(Keys.IceVerdict.class) String verdict);
}
