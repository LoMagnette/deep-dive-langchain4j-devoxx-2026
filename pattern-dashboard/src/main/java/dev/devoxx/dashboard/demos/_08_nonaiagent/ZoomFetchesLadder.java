package dev.devoxx.dashboard.demos._08_nonaiagent;

import dev.devoxx.dashboard.demos._08_nonaiagent.Keys.LadderLength;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.ToolsSupplier;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

/** Zoom, with his gear. Reused by GOAP (10) and the Mega Mutt (17). */
public interface ZoomFetchesLadder {
    @Agent(name = "Zoom",
           typedOutputKey = Keys.Ladder.class,
           description = "Zoom the Greyhound: fetches the shortest ladder that is long enough, and delivers it")
    @UserMessage("""
            Rivet says the ladder must be at least {{LadderLength}} metres. Fetch the shortest
            ladder in the shed that is at least that long — never a shorter one — and deliver it
            to the rescue. One plain sentence: which ladder, and where it is now.""")
    String fetch(@K(LadderLength.class) double ladderLength);

    /** His gear: the declarative form of {@code .tools(new ZoomGear())}. */
    @ToolsSupplier
    static Object gear() {
        return new ZoomGear();
    }
}
