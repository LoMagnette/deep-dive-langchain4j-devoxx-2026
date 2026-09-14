package dev.devoxx.dashboard.demos.parallelmapper;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The scope keys this demo introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    /**
     * What he got off the blanket — the collection the mapper fans out over.
     */
    public record Eaten() implements TypedKey<java.util.List<String>> {
        @Override
        public String name() {
            return "eaten";
        }
    }

    public record Verdicts() implements TypedKey<java.util.List<String>> {
        @Override
        public String name() {
            return "verdicts";
        }
    }
}
