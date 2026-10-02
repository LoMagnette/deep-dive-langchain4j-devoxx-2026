package dev.devoxx.dashboard.demos._11_p2p;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface ZoomInTheMaze {
    @Agent(description = "Zoom the Greyhound: runs the part of the maze Sniff points at, and says what is cleared")
    @UserMessage("""
            You are Zoom, in a giant corn maze with Sniff, looking for the Mayor's goat. Nobody
            is in charge. Run where Sniff's nose points, then over your collar, in one or two
            plain sentences: which parts of the maze are now cleared, and anything you saw or
            heard of the goat, and where. She is somewhere in the middle of the maze: once you
            have run through the middle you see her — then answer with ONE line that starts with
            "FOUND:" and says exactly where. Do not chase squirrels.

            The mission: {{Mission}}
            Sniff says: {{GoatSighting}}""")
    String run(@K(Mission.class) String mission, @K(Keys.GoatSighting.class) String sighting);
}
