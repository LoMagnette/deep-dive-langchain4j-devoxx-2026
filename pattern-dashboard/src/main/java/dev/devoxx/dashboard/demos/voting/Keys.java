package dev.devoxx.dashboard.demos.voting;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The scope keys this demo introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    public record Household() implements TypedKey<String> {}

    public record MoneyVote() implements TypedKey<String> {}

    public record SpaceVote() implements TypedKey<String> {}

    public record ZaoVote() implements TypedKey<String> {}
}
