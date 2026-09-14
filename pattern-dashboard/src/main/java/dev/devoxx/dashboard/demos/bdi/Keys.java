package dev.devoxx.dashboard.demos.bdi;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The scope keys this demo introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    public record Fed() implements TypedKey<String> {
        @Override
        public String name() {
            return "fed";
        }
    }

    public record Hour() implements TypedKey<String> {
        @Override
        public String name() {
            return "hour";
        }
    }

    /**
     * He has been to the garden. A precondition, not prose.
     */
    public record Out() implements TypedKey<String> {
        @Override
        public String name() {
            return "out";
        }
    }

    public record Session() implements TypedKey<String> {
        @Override
        public String name() {
            return "session";
        }
    }
}
