package dev.devoxx.dashboard.demos._12_blackboard;

import dev.langchain4j.agentic.declarative.TypedKey;

/**
 * The Pup Board pins this mission introduces. See {@code demos/package-info.java}.
 *
 * <p>The spec's {@code clues} list is three pins rather than one list, deliberately: an agent's
 * output OVERWRITES its key, so three Rangers writing one list would keep only the last clue.
 * One pin each is how a board accumulates.
 */
public final class Keys {

    private Keys() {
    }

    public record ScentClue() implements TypedKey<String> {}

    public record TunnelClue() implements TypedKey<String> {}

    public record CameraClue() implements TypedKey<String> {}

    /** Zao's ruling. Writing this is the goal state. */
    public record Culprit() implements TypedKey<String> {}
}
