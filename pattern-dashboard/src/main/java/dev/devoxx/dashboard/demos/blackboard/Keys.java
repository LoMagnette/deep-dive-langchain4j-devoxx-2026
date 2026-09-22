package dev.devoxx.dashboard.demos.blackboard;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The scope keys this demo introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    /**
     * The ranked differential. Writing this is the goal state.
     */
    public record Causes() implements TypedKey<String> {}

    public record Home() implements TypedKey<String> {}

    public record Problem() implements TypedKey<String> {}

    public record Routine() implements TypedKey<String> {}
}
