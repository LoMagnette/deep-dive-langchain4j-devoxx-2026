package dev.devoxx.dashboard.demos._20_async;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The scope keys this demo introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    /**
     * What the out-of-hours line came back with.
     */
    public record VetLine() implements TypedKey<String> {}
}
