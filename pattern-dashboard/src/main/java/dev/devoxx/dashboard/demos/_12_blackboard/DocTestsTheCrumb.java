package dev.devoxx.dashboard.demos._12_blackboard;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface DocTestsTheCrumb {
    @Agent(description = "Doc the Saint Bernard: tests the crumb in Zao's beard against the cameras")
    @UserMessage("""
            You are Doc, a Saint Bernard on the Pawer Rangers. Examine the crumb in Zao's beard
            using the camera log below, and say in two short plain sentences, no headings: which
            sausage the crumb came from and when Zao ate it, and where Zao was while the
            sausages were being stolen.

            Rivet's cameras:
            {{CameraClue}}""")
    String clue(@K(Keys.CameraClue.class) String cameras);
}
