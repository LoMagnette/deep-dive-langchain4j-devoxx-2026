package dev.devoxx.dashboard.demos._10_goap;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The scope keys this demo introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    public record Garden() implements TypedKey<String> {}

    public record Goal() implements TypedKey<String> {}

    public record Indoor() implements TypedKey<String> {}

    public record Park() implements TypedKey<String> {}
}
