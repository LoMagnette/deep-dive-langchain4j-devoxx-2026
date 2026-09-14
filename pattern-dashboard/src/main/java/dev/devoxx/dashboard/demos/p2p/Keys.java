package dev.devoxx.dashboard.demos.p2p;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The scope keys this demo introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    /**
     * The rule both halves will actually keep. Writing this is what ends the negotiation.
     */
    public record Agreement() implements TypedKey<String> {
        @Override
        public String name() {
            return "agreement";
        }
    }

    public record Proposal() implements TypedKey<String> {
        @Override
        public String name() {
            return "proposal";
        }
    }

    public record Question() implements TypedKey<String> {
        @Override
        public String name() {
            return "question";
        }
    }
}
