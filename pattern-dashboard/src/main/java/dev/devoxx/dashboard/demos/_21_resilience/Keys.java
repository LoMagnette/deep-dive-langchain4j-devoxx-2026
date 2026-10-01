package dev.devoxx.dashboard.demos._21_resilience;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The scope keys this demo introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    /**
     * Who got hurt, and often nobody — which is the entire point of the optional step that
     * reads it. Most operations end with every dog in one piece, so most runs never write this
     * key, and an agent that declares it must be prepared to be skipped.
     */
    public record Injuries() implements TypedKey<String> {}

    /** The first-aid paragraph, when there was one to write. */
    public record FirstAid() implements TypedKey<String> {}
}
