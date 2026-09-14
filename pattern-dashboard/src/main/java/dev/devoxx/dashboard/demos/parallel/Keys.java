package dev.devoxx.dashboard.demos.parallel;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The scope keys this demo introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    public record Meals() implements TypedKey<String> {
        @Override
        public String name() {
            return "meals";
        }
    }

    public record Stay() implements TypedKey<String> {
        @Override
        public String name() {
            return "stay";
        }
    }

    public record Walks() implements TypedKey<String> {
        @Override
        public String name() {
            return "walks";
        }
    }
}
