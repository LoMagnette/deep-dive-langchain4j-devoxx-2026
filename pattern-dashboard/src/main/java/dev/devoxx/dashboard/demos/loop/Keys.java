package dev.devoxx.dashboard.demos.loop;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The scope keys this demo introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    /**
     * The critic's mark out of 1.0 — a String, not a double, because a real model answers with
     * prose like 'I would rate this 0.85' and a double return type blows the loop up. See
     * Parsing.score.
     */
    public record Score() implements TypedKey<String> {}
}
