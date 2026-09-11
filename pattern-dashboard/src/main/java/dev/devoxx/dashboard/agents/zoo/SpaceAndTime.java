package dev.devoxx.dashboard.agents.zoo;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * <b>11 — a second dog?</b>
 *
 * <p>Three voters with DELIBERATELY DIFFERENT criteria over the same household, so they can
 * genuinely split — which is the only way a majority means anything. Three copies of one
 * prompt always agree, and then the tally is decoration. Each answers in one word, because
 * that is what a voting strategy can tally: a one-line reason per voter would make every
 * answer unique and no majority could ever form. The reasoning is still visible — the result
 * pane shows all three votes side by side.
 */
public interface SpaceAndTime {
    @Agent(description = "Votes on a second dog on space and hours alone")
    @UserMessage("""
            Should this household get a second dog? Judge ONLY the space they have and the
            hours the dogs would be alone. Ignore money and ignore the dog they already have.
            Answer with one word: YES or LATER.

            Household: {{household}}""")
    String vote(@V("household") String household);
}
