package dev.devoxx.dashboard.demos._03_loop;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;

/** The write-score loop, as a real interface, not {@code UntypedAgent}. */
public interface PosterLoop {
    @Agent
    ResultWithAgenticScope<String> refine(@K(Keys.Brief.class) String brief, @K(Keys.Rules.class) String rules,
                                          @K(Keys.Feedback.class) String feedback);
}
