package dev.devoxx.dashboard.demos._06_conditional;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The Pup Board pins this mission introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    /** What came in on the emergency phone. */
    public record Call() implements TypedKey<String> {}

    /** lost, underground, hurt or urgent — see Parsing.category, which keeps it honest. */
    public record Category() implements TypedKey<String> {}

    /** What the Ranger who took the call did about it. */
    public record Response() implements TypedKey<String> {}
}
