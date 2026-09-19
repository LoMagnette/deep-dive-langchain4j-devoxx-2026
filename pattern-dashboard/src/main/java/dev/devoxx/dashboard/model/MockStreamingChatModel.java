package dev.devoxx.dashboard.model;

import java.util.List;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

/**
 * The offline half of the streaming demo: the same canned answers {@link MockChatModel} gives,
 * handed over a few characters at a time with a small pause between them.
 *
 * <p>It delegates rather than duplicating the rule table — there is exactly one place canned
 * answers are decided, and a second copy would drift from it the first time a prompt changed.
 *
 * <p>The pause is the honest part. Without it every token arrives in the same millisecond and
 * the demo shows a block of text appearing at once, which is precisely what streaming is meant
 * to look different from. {@link #DELAY_MS} is what a small local model roughly feels like.
 */
public class MockStreamingChatModel implements StreamingChatModel {

    /** Slow enough to read arriving, fast enough that a demo does not stall on stage. */
    private static final long DELAY_MS = 18;

    private final MockChatModel answers;

    public MockStreamingChatModel() {
        this(List.of());
    }

    public MockStreamingChatModel(List<ChatModelListener> listeners) {
        this.answers = new MockChatModel(listeners);
    }

    @Override
    public void doChat(ChatRequest request, StreamingChatResponseHandler handler) {
        try {
            // One call to the canned model, then handed out in pieces. The listeners fire here,
            // so the Server log tab still shows one prompt and one answer per call rather than
            // one line per token.
            String text = answers.chat(request).aiMessage().text();
            for (String chunk : chunks(text)) {
                handler.onPartialResponse(chunk);
                Thread.sleep(DELAY_MS);
            }
            handler.onCompleteResponse(
                    ChatResponse.builder().aiMessage(AiMessage.from(text)).build());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            handler.onError(e);
        } catch (RuntimeException e) {
            handler.onError(e);
        }
    }

    /**
     * Word-ish pieces, keeping the trailing space on each so the text reassembles exactly.
     * Splitting on characters looks like a teletype rather than a model, and splitting on words
     * without the separator silently loses every space.
     */
    private static List<String> chunks(String text) {
        return List.of(text.split("(?<=\\s)"));
    }
}
