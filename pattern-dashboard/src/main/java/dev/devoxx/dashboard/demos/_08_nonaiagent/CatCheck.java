package dev.devoxx.dashboard.demos._08_nonaiagent;

import java.util.ArrayList;
import java.util.List;

import dev.devoxx.dashboard.demos._08_nonaiagent.Keys.Plan;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;

/**
 * The other end of the sandwich: a plain Java check that the cat's facts survived the writing.
 */
public class CatCheck {

    /** Every location from the diary that must appear untouched, with what is buried there. */
    private static final List<String[]> MUST_SURVIVE = List.of(
            new String[] {"the sock", "under the rosemary"},
            new String[] {"the sausage", "3 paces from the shed"},
            new String[] {"the TV remote", "north-east corner"});

    @Agent(name = "CatCheck",
           description = "The cat: checks the diary's locations survived into the dig plan",
           typedOutputKey = Plan.class)
    public String check(@K(Plan.class) String plan) {
        List<String> missing = new ArrayList<>();
        for (String[] fact : MUST_SURVIVE) {
            if (plan == null || !plan.contains(fact[1])) {
                missing.add(fact[0] + ": " + fact[1]);
            }
        }
        if (missing.isEmpty()) {
            return plan + "\n\n*Checked against the cat's diary: every location matches.*";
        }
        // Appended rather than substituted: the plan is still the model's, and a pack holding
        // shovels needs the missing location more than the system needs to look tidy.
        return plan + "\n\n**From the cat's diary — left out of the plan above:**\n"
                + String.join("\n", missing.stream().map(m -> "- " + m).toList());
    }
}
