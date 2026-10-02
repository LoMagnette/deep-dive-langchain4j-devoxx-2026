package dev.devoxx.dashboard.demos._13_voting;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The Pup Board pins this mission introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    /** Sniff's vote. */
    public record Vote1() implements TypedKey<String> {}

    /** Doc's vote. */
    public record Vote2() implements TypedKey<String> {}

    /** Bolt's vote — computed, not judged. */
    public record Vote3() implements TypedKey<String> {}
}
