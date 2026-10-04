package dev.devoxx.dashboard.demos._03_loop;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The Pup Board pins this mission introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    /** What the piece must say. The poster's facts, or — in Mission 17 — the Gazette's. */
    public record Brief() implements TypedKey<String> {}

    /**
     * The four rules Fifi scores against. A pin rather than part of her prompt, so the same loop
     * can grade a poster here and a Gazette story inside the Mega Mutt (17).
     */
    public record Rules() implements TypedKey<String> {}

    /** Howl's latest version. Overwritten every pass. */
    public record Draft() implements TypedKey<String> {}

    /**
     * Fifi's review: "SCORE: 3/4" on the first line, then what is wrong. A String, not a number,
     * because Fifi is a model and models answer in prose — Parsing.reviewScore reads the number.
     */
    public record Feedback() implements TypedKey<String> {}
}
