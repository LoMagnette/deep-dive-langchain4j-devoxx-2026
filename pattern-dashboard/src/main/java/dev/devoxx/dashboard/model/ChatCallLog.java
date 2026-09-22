package dev.devoxx.dashboard.model;

import java.util.List;

import org.jboss.logging.Logger;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;

/**
 * Logs the prompt sent and the answer returned, one line each, under the logger name
 * {@code chat}. It is what makes the "Server log" tab worth projecting: without it the tab
 * carries only the framework talking about itself ("activating agent X"), which is the shape of
 * a run and none of its content.
 */
public class ChatCallLog implements ChatModelListener {

    /** Not the class name: the tab and the log config both address this as "chat". */
    private static final Logger LOG = Logger.getLogger("chat");

    /** Long enough to read the ask, short enough that one call is one line on a projector. */
    private static final int MAX = 700;

    private static final String STARTED_AT = "chatCallLog.startedAt";

    @Override
    public void onRequest(ChatModelRequestContext ctx) {
        ctx.attributes().put(STARTED_AT, System.nanoTime());
        LOG.infof("→ %s", trim(prompt(ctx.chatRequest().messages())));
    }

    @Override
    public void onResponse(ChatModelResponseContext ctx) {
        ChatResponse response = ctx.chatResponse();
        LOG.infof("← %s  [%s, %s]", trim(response.aiMessage().text()),
                elapsed(ctx.attributes().get(STARTED_AT)), tokens(response.tokenUsage()));
    }

    /**
     * Without this a failed call logs a "→" with no "←", which on stage reads as a hang rather
     * than an error — and a dead Ollama is exactly when you are looking at this tab.
     */
    @Override
    public void onError(ChatModelErrorContext ctx) {
        LOG.warnf("✗ %s  [%s]", trim(String.valueOf(ctx.error())),
                elapsed(ctx.attributes().get(STARTED_AT)));
    }

    /**
     * The system message carries the agent's role and the user message carries the task; both
     * are worth seeing, and neither alone explains what the model was asked.
     */
    private static String prompt(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder();
        for (ChatMessage m : messages) {
            if (m instanceof SystemMessage sm) {
                sb.append(sb.isEmpty() ? "" : " ").append(sm.text());
            } else if (m instanceof UserMessage um && um.hasSingleText()) {
                sb.append(sb.isEmpty() ? "" : " ").append(um.singleText());
            }
        }
        return sb.toString();
    }

    /** One line: newlines become ⏎ so a thirty-line text block stays one log record. */
    private static String trim(String text) {
        if (text == null) {
            return "";
        }
        String flat = text.replaceAll("\\s*\\R\\s*", " ⏎ ").trim();
        return flat.length() <= MAX ? flat : flat.substring(0, MAX) + " …";
    }

    private static String elapsed(Object startedAt) {
        if (!(startedAt instanceof Long start)) {
            return "? ms";
        }
        return (System.nanoTime() - start) / 1_000_000 + " ms";
    }

    /** Ollama reports usage; a mock does not, and "no count" is the honest thing to print. */
    private static String tokens(TokenUsage usage) {
        if (usage == null || usage.totalTokenCount() == null) {
            return "no token count";
        }
        return usage.totalTokenCount() + " tokens";
    }
}
