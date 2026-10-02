package dev.devoxx.dashboard.demos._11_p2p;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface SniffInTheMaze {
    @Agent(description = "Sniff the Beagle: follows the goat's scent through the maze, and says where Zoom should run")
    @UserMessage("""
            You are Sniff, in a giant corn maze with Zoom, looking for the Mayor's goat. Nobody
            is in charge.

            First decide: has Zoom seen the goat, or heard her bleat? If he has, she is exactly
            where he heard her, and your nose confirms it. Answer with ONE line that starts with
            "FOUND:" and says where she is — nothing else.

            Otherwise, over your collar, in one or two plain sentences: what your nose says now,
            and where Zoom should run next.

            The mission: {{Mission}}
            Zoom has cleared: {{ClearedAreas}}""")
    String sniff(@K(Mission.class) String mission, @K(Keys.ClearedAreas.class) String cleared);
}
