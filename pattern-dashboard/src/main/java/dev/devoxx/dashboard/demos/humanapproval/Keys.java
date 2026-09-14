package dev.devoxx.dashboard.demos.humanapproval;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The scope keys this demo introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    public record Decision() implements TypedKey<String> {
        @Override
        public String name() {
            return "decision";
        }
    }

    public record Draft() implements TypedKey<String> {
        @Override
        public String name() {
            return "draft";
        }
    }

    public record Instruction() implements TypedKey<String> {
        @Override
        public String name() {
            return "instruction";
        }
    }
}
