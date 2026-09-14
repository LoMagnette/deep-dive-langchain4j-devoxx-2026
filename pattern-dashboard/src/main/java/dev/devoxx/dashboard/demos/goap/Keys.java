package dev.devoxx.dashboard.demos.goap;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The scope keys this demo introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    public record Garden() implements TypedKey<String> {
        @Override
        public String name() {
            return "garden";
        }
    }

    public record Goal() implements TypedKey<String> {
        @Override
        public String name() {
            return "goal";
        }
    }

    public record Indoor() implements TypedKey<String> {
        @Override
        public String name() {
            return "indoor";
        }
    }

    public record Park() implements TypedKey<String> {
        @Override
        public String name() {
            return "park";
        }
    }
}
