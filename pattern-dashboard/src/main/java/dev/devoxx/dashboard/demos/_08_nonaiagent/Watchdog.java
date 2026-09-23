package dev.devoxx.dashboard.demos._08_nonaiagent;

import java.util.ArrayList;
import java.util.List;

import dev.devoxx.dashboard.demos._01_single.Keys.Notes;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;

/**
 * The other end of the sandwich: a plain Java check that the facts survived the writing.
 */
public class Watchdog {

    /** Every value from the record that must appear untouched, with what to call it. */
    private static final List<String[]> MUST_SURVIVE = List.of(
            new String[] {"the vet's number", "061 22 33 44"},
            new String[] {"the microchip", "981098106123456"},
            new String[] {"the insurance policy", "AG-4471209"});

    @Agent(name = "Watchdog",
           description = "Checks the record's numbers survived into the note",
           typedOutputKey = Notes.class)
    public String check(@K(Notes.class) String notes) {
        List<String> missing = new ArrayList<>();
        for (String[] fact : MUST_SURVIVE) {
            if (notes == null || !notes.contains(fact[1])) {
                missing.add(fact[0] + " (" + fact[1] + ")");
            }
        }
        if (missing.isEmpty()) {
            return notes + "\n\n*Checked against the household file: every number matches.*";
        }
        // Appended rather than substituted: the note is still the model's, and a sitter reading
        // it needs the missing number more than the system needs to look tidy.
        return notes + "\n\n**From the household file — left out of the note above:**\n"
                + String.join("\n", missing.stream().map(m -> "- " + m).toList());
    }
}
