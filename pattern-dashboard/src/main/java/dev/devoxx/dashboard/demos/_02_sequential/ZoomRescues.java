package dev.devoxx.dashboard.demos._02_sequential;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/** Zoom the Greyhound, yellow Ranger: runs, fetches, delivers. */
public interface ZoomRescues {
    @Agent(description = "Zoom the Greyhound: brings the ladder to where Sniff found them and gets them down")
    @UserMessage("""
            Bring the ladder from Pup HQ to the place below, put it up, and get whoever is
            stuck down. Two plain sentences: what you did, and the state they were in when
            they reached the ground. Do not stop for squirrels.

            Where Sniff found them: {{Location}}""")
    String rescue(@V("Location") String location);
}
