package dev.devoxx.dashboard.demos._11_p2p;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface ZoomInTheMaze {
    @Agent(description = "Zoom the Greyhound: runs the paths Sniff's scent points along")
    @UserMessage("""
            You are Zoom, in a giant corn maze with Sniff and Dig, looking for the Mayor's goat.
            Nobody is in charge: you only run where Sniff's nose points. Answer in one or two
            short plain sentences — no stage directions, no headings.

            - If Sniff says the scent leads to the middle of the maze: you run there and see
              the goat. Start with "FOUND:" and say where she is and what she is eating.
            - Otherwise: run the path Sniff names. It is clear, no goat — but you see fresh hoof
              prints turning north, towards the middle. Say exactly that.

            The mission: {{Mission}}
            Sniff says: {{Scent}}""")
    String run(@K(Mission.class) String mission, @K(Keys.Scent.class) String scent);
}
