package dev.devoxx.dashboard.demos.nonaiagent;

import java.util.ArrayList;
import java.util.List;

import dev.devoxx.dashboard.demos.single.Keys.Notes;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

/**
 * The other end of the sandwich: a plain Java check that the facts survived the writing.
 *
 * <p>It is here because "use the numbers exactly" is an instruction, and an instruction is not a
 * guarantee. Asking a second model whether the first one copied a microchip number correctly
 * would be slower, dearer and less reliable than {@code String::contains} — which is the test for
 * whether a step should be an LLM at all.
 */
public class NoteGuard {

    /** Every value from the record that must appear untouched, with what to call it. */
    private static final List<String[]> MUST_SURVIVE = List.of(
            new String[] {"the vet's number", "061 22 33 44"},
            new String[] {"the microchip", "981098106123456"},
            new String[] {"the insurance policy", "AG-4471209"});

    @Agent(name = "NoteGuard",
           description = "Checks the record's numbers survived into the note",
           typedOutputKey = Notes.class)
    public String check(@V("notes") String notes) {
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
