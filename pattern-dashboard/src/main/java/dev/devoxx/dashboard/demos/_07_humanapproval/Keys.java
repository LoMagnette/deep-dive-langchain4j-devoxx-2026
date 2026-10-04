package dev.devoxx.dashboard.demos._07_humanapproval;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The Pup Board pins this mission introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    /** Dig's plan, waiting on Officer Jo. Nothing is dug while this is all there is. */
    public record DigPlan() implements TypedKey<String> {}

    /** What Officer Jo said — yes, no, or yes-but. Written by a person, read by a dog. */
    public record Approved() implements TypedKey<String> {}
}
