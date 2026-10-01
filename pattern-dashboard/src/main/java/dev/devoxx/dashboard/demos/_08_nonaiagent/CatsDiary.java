package dev.devoxx.dashboard.demos._08_nonaiagent;

import dev.devoxx.dashboard.demos._08_nonaiagent.Keys.Mission;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;

/**
 * A class, not an interface — and that is the whole lesson. There is no proxy here, no prompt and
 * no model: {@code AgentUtil.nonAiAgentToExecutor} takes any object with one
 * {@code @Agent}-annotated method, binds the {@code @K} parameters from the scope and writes the
 * return value to the output key, exactly as it does for an LLM agent.
 *
 * <p>It is the cat because it is not a dog: every other agent in the pack is a model, and this
 * one does exactly what it does, every time, and tells nobody.
 */
public class CatsDiary {

    /**
     * Stands in for the row this would really come from. Hardcoded rather than faked from a
     * model on purpose: the demo is about where facts come from, and a "database" that is
     * secretly an LLM would be the exact mistake being argued against.
     */
    private static final String DIARY = """
            Mon 07:12 — Zao: one sock (left, the human's), under the rosemary, 30 cm down
            Tue 14:02 — Beagle: half a sausage, left flowerbed, 3 paces from the shed
            Thu 16:45 — Dachshund: the TV remote, sandpit, north-east corner
            Sat 09:30 — Labrador: nothing buried. Ate a tennis ball. Seemed fine.""";

    @Agent(name = "CatsDiary",
           description = "Looks up where everything was buried, in the cat's own records",
           typedOutputKey = Keys.Facts.class)
    public String lookup(@K(Mission.class) String mission) {
        // The parameter is here because the scope binding is the thing worth seeing — a real
        // lookup would key off it. One garden, one cat, so it is the same diary every time.
        return DIARY;
    }
}
