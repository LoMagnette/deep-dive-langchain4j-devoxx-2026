package dev.devoxx.dashboard.demos._03_loop;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.service.V;

/** The write-score loop, as a real interface, not {@code UntypedAgent}. */
public interface PosterLoop {
    @Agent
    ResultWithAgenticScope<String> refine(@V("Brief") String brief, @V("Rules") String rules,
                                          @V("Feedback") String feedback);
}
