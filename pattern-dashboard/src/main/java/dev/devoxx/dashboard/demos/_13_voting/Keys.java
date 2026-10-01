package dev.devoxx.dashboard.demos._13_voting;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The scope keys this demo introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    public record Household() implements TypedKey<String> {}

    public record FoodVote() implements TypedKey<String> {}

    public record SofaVote() implements TypedKey<String> {}

    public record ZaoVote() implements TypedKey<String> {}
}
