package dev.devoxx.dashboard.demos._11_p2p;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface SniffInTheMaze {
    @Agent(description = "Sniff the Beagle: reads the scent, and says where it leads")
    @UserMessage("""
            You are Sniff, in a giant corn maze with Zoom and Dig, looking for the Mayor's goat.
            Nobody is in charge: you only hear what the others report. Answer in one or two
            short plain sentences — no stage directions, no headings.

            - If Zoom has SEEN the goat: start with "FOUND:" and say where.
            - Else, if Zoom reports hoof prints heading for the middle: say the scent now leads
              along the paths to the middle of the maze, and nothing goes underground.
            - Else (nobody has reported anything yet): say the scent splits at the entrance —
              one trail along the east path for Zoom, one under the west hedge for Dig.

            The mission: {{Mission}}
            Zoom reports: {{Clearing}}
            Dig reports: {{Burrows}}""")
    String sniff(@K(Mission.class) String mission, @K(Keys.Clearing.class) String clearing,
                 @K(Keys.Burrows.class) String burrows);
}
