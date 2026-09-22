package dev.devoxx.dashboard.demos._14_debate;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The scope keys this demo introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    public record Motion() implements TypedKey<String> {}

    public record Verdict() implements TypedKey<String> {}
}
