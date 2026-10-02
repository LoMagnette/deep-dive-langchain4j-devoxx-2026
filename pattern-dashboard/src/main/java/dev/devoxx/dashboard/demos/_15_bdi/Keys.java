package dev.devoxx.dashboard.demos._15_bdi;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The Pup Board pins this mission introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    /** What Zoom believes about the world right now. Seeded from the radio, in plain Java. */
    public record Beliefs() implements TypedKey<String> {}

    /** The rescue desire is satisfied once this exists. */
    public record Rescued() implements TypedKey<String> {}

    /** The squirrel desire is satisfied once this exists. */
    public record Chased() implements TypedKey<String> {}

    /** The nap desire is satisfied once this exists. */
    public record Napped() implements TypedKey<String> {}
}
