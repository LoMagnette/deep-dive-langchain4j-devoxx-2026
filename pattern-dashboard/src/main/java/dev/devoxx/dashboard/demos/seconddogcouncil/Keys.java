package dev.devoxx.dashboard.demos.seconddogcouncil;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The scope keys this demo introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    public record Angles() implements TypedKey<java.util.List<String>> {
        @Override
        public String name() {
            return "angles";
        }
    }

    /**
     * One scout's finding, before they are gathered.
     */
    public record Finding() implements TypedKey<String> {
        @Override
        public String name() {
            return "finding";
        }
    }

    /**
     * What they found. A List, never a String — declaring it wrong fails at runtime with a
     * bare argument type mismatch, which is the sort of thing this whole mechanism exists to
     * stop.
     */
    public record Findings() implements TypedKey<java.util.List<String>> {
        @Override
        public String name() {
            return "findings";
        }
    }

    public record Ratified() implements TypedKey<String> {
        @Override
        public String name() {
            return "ratified";
        }
    }
}
