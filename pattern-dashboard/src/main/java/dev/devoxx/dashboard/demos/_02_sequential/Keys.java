package dev.devoxx.dashboard.demos._02_sequential;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The scope keys this demo introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    public record Checklist() implements TypedKey<String> {}
}
