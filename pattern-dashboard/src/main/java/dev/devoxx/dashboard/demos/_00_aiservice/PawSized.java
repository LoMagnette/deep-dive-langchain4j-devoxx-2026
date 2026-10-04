package dev.devoxx.dashboard.demos._00_aiservice;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;

/**
 * The output guardrail: runs AFTER the model answers, and may send the answer back with an
 * instruction — a reprompt — instead of failing the call. The rule is one the room can check by
 * eye: the reply has to fit on the Pup HQ noticeboard.
 */
public class PawSized implements OutputGuardrail {

    /** What fits on the noticeboard. */
    static final int MAX_WORDS = 50;

    @Override
    public OutputGuardrailResult validate(AiMessage reply) {
        String text = reply.text() == null ? "" : reply.text().strip();
        int words = text.isEmpty() ? 0 : text.split("\\s+").length;
        if (words > MAX_WORDS) {
            // The reprompt carries the draft itself. A plain AI service has no chat memory
            // unless you give it one, so the retry is sent WITHOUT the original letter — an
            // instruction like "answer again, shorter" reached a live model with nothing to
            // answer, and it replied "please provide the letter".
            return reprompt("Too long for the noticeboard: " + words + " words",
                    "Rewrite this answer to fit the noticeboard: at most three short sentences, "
                            + "under " + MAX_WORDS + " words, keeping who will come and when.\n\n"
                            + text);
        }
        return success();
    }
}
