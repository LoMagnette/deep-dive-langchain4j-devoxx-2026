package dev.devoxx.dashboard.demos.single;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The scope keys this demo introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    public record Message() implements TypedKey<String> {
        @Override
        public String name() {
            return "message";
        }
    }

    /**
     * Everything we know about the stay, in whatever shape it is currently in — a card at
     * first, a fridge list later. It crosses four demos, which is why it is typed.
     */
    public record Notes() implements TypedKey<String> {
        @Override
        public String name() {
            return "notes";
        }
    }
}
