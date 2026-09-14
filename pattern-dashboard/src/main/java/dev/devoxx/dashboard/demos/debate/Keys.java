package dev.devoxx.dashboard.demos.debate;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The scope keys this demo introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    public record Motion() implements TypedKey<String> {
        @Override
        public String name() {
            return "motion";
        }
    }

    public record Verdict() implements TypedKey<String> {
        @Override
        public String name() {
            return "verdict";
        }
    }
}
