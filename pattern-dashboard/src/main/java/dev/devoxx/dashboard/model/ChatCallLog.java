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
 *
 * <p>Three decisions worth keeping:
 *
 * <ul>
 * <li><b>INFO, not DEBUG.</b> The model's own {@code logRequests}/{@code logResponses} do this at
 * DEBUG, which meant the most interesting half of the demo was invisible unless somebody
 * remembered to raise a log level before going on stage. {@link ModelFactory} no longer sets
 * those flags — this replaces them, so there is exactly one source of prompt logging and no
 * duplicate lines.</li>
 * <li><b>It works for the mock too.</b> {@code ChatModel.chat()}'s default implementation fires
 * {@code listeners()} and then calls {@code doChat()} — so {@link MockChatModel} overrides
 * {@code doChat}, not {@code chat}, and takes a listener list in its constructor.</li>
 * <li><b>One line per call.</b> {@link #trim} flattens newlines to {@code ⏎} and caps at 700
 * chars; a prompt is a thirty-line text block, and thirty log records per call is a wall nobody
 * reads.</li>
 * </ul>
 *
 * <p>The start time rides in the per-call {@link ChatModelRequestContext#attributes()} map rather
 * than a field or a ThreadLocal: a parallel step runs several calls at once, on several threads,
 * and that map is the only thing the framework guarantees is scoped to one call.
 *
 * <p>This is also the hook §8½ hangs OpenTelemetry off — the same listener interface, a different
 * body.
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
