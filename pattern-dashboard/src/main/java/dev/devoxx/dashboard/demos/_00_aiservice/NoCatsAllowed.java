package dev.devoxx.dashboard.demos._00_aiservice;

import java.util.Locale;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;

/**
 * The input guardrail: runs BEFORE the model sees the letter, and a failure means the model is
 * never called at all. Plain Java on purpose — the check that stops a prompt injection must not
 * itself be a prompt.
 */
public class NoCatsAllowed implements InputGuardrail {

    @Override
    public InputGuardrailResult validate(UserMessage letter) {
        String text = letter.singleText().toLowerCase(Locale.ROOT);
        if (text.contains("mittens")) {
            return fatal("Pup HQ does not take letters from cats.");
        }
        if (text.contains("ignore your instructions") || text.contains("ignore all previous")
                || text.contains("ignore previous instructions")) {
            return fatal("That is not a letter, that is a trick.");
        }
        return success();
    }
}
