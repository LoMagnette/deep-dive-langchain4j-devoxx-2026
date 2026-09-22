package dev.devoxx.dashboard.demos._18_seconddogcouncil;

import java.util.List;

import dev.devoxx.dashboard.demos._11_p2p.Keys.Question;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface CouncilBriefer {
    /**
     * {@code findings} is a {@code List}, not a String, because that is what the parallel
     * mapper writes into the scope — declare it as a String and the invocation dies with a
     * bare "argument type mismatch". Scope values are passed through as-is, never coerced.
     */
    @Agent(description = "Turns what the scouts found into the motion the council will weigh")
    @UserMessage("""
            Write the motion this household should decide on, as one sentence: a second dog or
            not, and if so what kind and when.

            Question: {{Question}}
            What the scouts found: {{Findings}}""")
    String brief(@K(Question.class) String question, @K(Keys.Findings.class) List<String> findings);
}
