package dev.devoxx.dashboard.demos._04_parallel;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The scope keys this demo introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    public record Meals() implements TypedKey<String> {}

    public record Stay() implements TypedKey<String> {}

    public record Walks() implements TypedKey<String> {}
}
