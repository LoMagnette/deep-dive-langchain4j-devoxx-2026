package dev.devoxx.dashboard.demos._02_sequential;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The Pup Board pins this mission introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    /** What Zoom did with the ladder. Doc reads this and nothing else. */
    public record RescueStatus() implements TypedKey<String> {}

    /** Doc's verdict on whoever was rescued. Howl reads this and nothing else. */
    public record HealthReport() implements TypedKey<String> {}

    /** The Barkville Gazette story. */
    public record Article() implements TypedKey<String> {}
}
