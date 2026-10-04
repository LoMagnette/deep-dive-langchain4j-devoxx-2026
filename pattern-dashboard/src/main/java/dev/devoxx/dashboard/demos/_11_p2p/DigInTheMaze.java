package dev.devoxx.dashboard.demos._11_p2p;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface DigInTheMaze {
    @Agent(name = "Dig",
           typedOutputKey = Keys.Burrows.class,
           description = "Dig the Dachshund: checks under the hedges where Sniff's scent goes")
    @UserMessage("""
            You are Dig, in a giant corn maze with Sniff and Zoom, looking for the Mayor's goat.
            Nobody is in charge: you only dig where Sniff's nose points. Answer in one or two
            short plain sentences — no stage directions, no headings.

            - If Sniff points under a hedge: you find a rabbit burrow there, far too small for a
              goat. Say it is a dead end.
            - Otherwise: say there is nothing under the hedges, and you stand down.

            The mission: {{Mission}}
            Sniff says: {{Scent}}""")
    String dig(@K(Mission.class) String mission, @K(Keys.Scent.class) String scent);
}
