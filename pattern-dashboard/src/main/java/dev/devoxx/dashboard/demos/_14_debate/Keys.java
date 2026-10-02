package dev.devoxx.dashboard.demos._14_debate;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The Pup Board pins this mission introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    /** What the council is deciding. */
    public record Motion() implements TypedKey<String> {}

    /** Howl's latest turn. Overwritten every round — the transcript is where it is kept. */
    public record HowlTurn() implements TypedKey<String> {}

    /** Mittens' latest turn. */
    public record MittensTurn() implements TypedKey<String> {}

    /** Every turn so far, in order. Grows every round; written only by Bolt. */
    public record Transcript() implements TypedKey<String> {}

    /** Fifi's ruling. */
    public record Verdict() implements TypedKey<String> {}
}
