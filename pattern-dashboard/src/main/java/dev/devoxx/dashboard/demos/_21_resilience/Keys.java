package dev.devoxx.dashboard.demos._21_resilience;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The Pup Board pins this mission introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    /**
     * Who got hurt, and often nobody — which is the entire point of the optional step that reads
     * it. Most rescues end with everyone in one piece, so most runs never write this key, and an
     * agent that declares it must be prepared to be skipped. It defaults to null, deliberately:
     * give it a default value and the step always has an argument, and stops being optional.
     */
    public record Injuries() implements TypedKey<String> {}

    /** Doc's first-aid note, when there was one to write. */
    public record FirstAid() implements TypedKey<String> {}
}
