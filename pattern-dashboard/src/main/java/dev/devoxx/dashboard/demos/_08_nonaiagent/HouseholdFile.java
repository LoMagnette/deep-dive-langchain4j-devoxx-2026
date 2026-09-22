package dev.devoxx.dashboard.demos._08_nonaiagent;

import dev.devoxx.dashboard.demos._04_parallel.Keys.Stay;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;

/**
 * A class, not an interface — and that is the whole lesson. There is no proxy here, no prompt and
 * no model: {@code AgentUtil.nonAiAgentToExecutor} takes any object with one
 * {@code @Agent}-annotated method, binds the {@code @K} parameters from the scope and writes the
 * return value to the output key, exactly as it does for an LLM agent.
 */
public class HouseholdFile {

    /**
     * Stands in for the row this would really come from. Hardcoded rather than faked from a
     * model on purpose: the demo is about where facts come from, and a "database" that is
     * secretly an LLM would be the exact mistake being argued against.
     */
    private static final String RECORD = """
            Name: Zao — Belgian shepherd (Malinois), male, 4 years
            Weight: 32 kg — 300 g of food per meal, twice a day
            Microchip: 981098106123456
            Vet: Dr Cluysen, 061 22 33 44 (out of hours: same number, diverts)
            Insurance: AG-4471209""";

    @Agent(name = "HouseholdFile",
           description = "Looks the dog up in the household's own records",
           typedOutputKey = Keys.Facts.class)
    public String lookup(@K(Stay.class) String stay) {
        // The parameter is here because the scope binding is the thing worth seeing — a real
        // lookup would key off it. One household, one dog, so it is the same row every time.
        return RECORD;
    }
}
