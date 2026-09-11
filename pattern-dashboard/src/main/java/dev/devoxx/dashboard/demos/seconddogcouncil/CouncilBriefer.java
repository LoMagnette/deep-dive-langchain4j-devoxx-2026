package dev.devoxx.dashboard.demos.seconddogcouncil;

import java.util.List;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

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

            Question: {{question}}
            What the scouts found: {{findings}}""")
    String brief(@V("question") String question, @V("findings") List<String> findings);
}
