package dev.devoxx.dashboard.demos._08_nonaiagent;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The Pup Board pins this mission introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    /**
     * How high the ladder has to reach, in metres. A tree here, a water tower in Mission 10 —
     * Rivet does not care which, which is why the pin is not called treeHeight.
     */
    public record Height() implements TypedKey<Double> {}

    /** Rivet's answer, in metres. A number, because Rivet is Java and Java can promise one. */
    public record LadderLength() implements TypedKey<Double> {}

    /** Which ladder Zoom brought, and where it is now. */
    public record Ladder() implements TypedKey<String> {}
}
