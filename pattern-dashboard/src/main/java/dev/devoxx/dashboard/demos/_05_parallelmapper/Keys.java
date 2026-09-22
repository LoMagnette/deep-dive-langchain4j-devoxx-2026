package dev.devoxx.dashboard.demos._05_parallelmapper;

import java.util.List;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The scope keys this demo introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    /**
     * What he got off the blanket — the collection the mapper fans out over.
     */
    public record Eaten() implements TypedKey<List<String>> {}

    public record Verdicts() implements TypedKey<List<String>> {}
}
