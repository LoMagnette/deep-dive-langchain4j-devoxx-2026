package dev.devoxx.dashboard.demos._05_parallelmapper;

import java.util.List;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The Pup Board pins this mission introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    /** One entry per duckling: a name and where it was last seen. */
    public record Ducklings() implements TypedKey<List<String>> {}

    /** What one search found, before the mapper gathers them. */
    public record Sighting() implements TypedKey<String> {}

    /** Every search's result, in the order the ducklings were listed. */
    public record FoundDucklings() implements TypedKey<List<String>> {}
}
