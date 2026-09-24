package dev.devoxx.dashboard.demos._18_seconddogcouncil;

import java.util.List;

import dev.devoxx.dashboard.demos._11_p2p.Keys.Question;
import dev.devoxx.dashboard.demos._18_seconddogcouncil.Keys.Angles;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;

/** The survey → brief → debate → note → ratify spine, as a real interface, not {@code UntypedAgent}. */
public interface CouncilPipeline {
    @Agent
    ResultWithAgenticScope<String> convene(
            @K(Question.class) String question, @K(Angles.class) List<String> angles);
}
