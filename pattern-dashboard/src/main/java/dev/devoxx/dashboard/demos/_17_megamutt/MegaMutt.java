package dev.devoxx.dashboard.demos._17_megamutt;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.devoxx.dashboard.demos._03_loop.Keys.Feedback;
import dev.devoxx.dashboard.demos._03_loop.Keys.Rules;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;

/** The whole Mega Mutt, as one agent: which is the point of the Mega Mutt. */
public interface MegaMutt {
    @Agent
    ResultWithAgenticScope<String> rescue(@K(Mission.class) String mission,
                                          @K(Rules.class) String rules,
                                          @K(Feedback.class) String feedback);
}
