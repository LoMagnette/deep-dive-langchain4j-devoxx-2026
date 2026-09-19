package dev.devoxx.dashboard.demos.resilience;

import java.util.concurrent.atomic.AtomicInteger;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

/**
 * A model that drops its first {@code failures} calls and then behaves. It is the only way to
 * demonstrate a retry policy deterministically: a demo that waits for the network to misbehave
 * on its own is a demo that works perfectly on stage and teaches nothing.
 *
 * <p>It wraps rather than replaces, so the real model (or the mock) still answers the calls that
 * do go through — and because it delegates to {@code chat} rather than reimplementing it, the
 * delegate's own listeners still fire and the prompts still reach the Server log tab.
 */
final class FlakyModel implements ChatModel {

    private final ChatModel delegate;
    private final int failures;
    private final AtomicInteger calls = new AtomicInteger();

    FlakyModel(ChatModel delegate, int failures) {
        this.delegate = delegate;
        this.failures = failures;
    }

    /** How many times it has been called, so the result can say what actually happened. */
    int calls() {
        return calls.get();
    }

    @Override
    public ChatResponse doChat(ChatRequest request) {
        if (calls.getAndIncrement() < failures) {
            // Thrown from inside the model, exactly where a dropped connection would be. The
            // framework wraps it in an AgentInvocationException, which is what the error handler
            // is handed — and what Errors.explain has to dig through to say anything useful.
            throw new IllegalStateException("the practice's line dropped");
        }
        return delegate.chat(request);
    }
}
