package dev.devoxx.dashboard.demos._04_parallel;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The Pup Board pins this mission introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    public record BridgeReport() implements TypedKey<String> {}

    public record ForestReport() implements TypedKey<String> {}

    public record TunnelReport() implements TypedKey<String> {}

    /** Zao's merge. Cannot be written until all three reports are in. */
    public record SafetyReport() implements TypedKey<String> {}
}
