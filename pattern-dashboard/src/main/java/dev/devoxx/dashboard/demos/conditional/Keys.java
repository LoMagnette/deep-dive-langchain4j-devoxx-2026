package dev.devoxx.dashboard.demos.conditional;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The scope keys this demo introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    public record Answer() implements TypedKey<String> {}

    /**
     * Which desk the router picked. One of emergency, training, everyday — see
     * Parsing.category, which is what keeps it honest.
     */
    public record Category() implements TypedKey<String> {}

    public record Worry() implements TypedKey<String> {}
}
