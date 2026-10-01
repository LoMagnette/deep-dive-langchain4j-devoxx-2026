package dev.devoxx.dashboard.demos._10_goap;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The scope keys this demo introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    /** The pushed chair. Written by ChairPusher, needed by CounterSurfer. */
    public record Chair() implements TypedKey<String> {}

    /** The human, out of the kitchen. Written by DoorbellDecoy, needed by ChairPusher. */
    public record Decoy() implements TypedKey<String> {}

    public record Goal() implements TypedKey<String> {}

    /** The sausage, off the counter. Writing this is the goal. */
    public record Sausage() implements TypedKey<String> {}
}
