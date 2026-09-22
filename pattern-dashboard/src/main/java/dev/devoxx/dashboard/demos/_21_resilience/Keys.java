package dev.devoxx.dashboard.demos._21_resilience;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The scope keys this demo introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    /**
     * The medication details, and often not there at all — which is the entire point of the
     * optional step that reads it. Most dogs are not on anything, so most runs never write this
     * key, and an agent that declares it must be prepared to be skipped.
     */
    public record Meds() implements TypedKey<String> {}

    /** The medication paragraph, when there was one to write. */
    public record MedNote() implements TypedKey<String> {}
}
