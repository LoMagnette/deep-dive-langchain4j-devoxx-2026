package dev.devoxx.dashboard.demos._12_blackboard;

import dev.langchain4j.agentic.declarative.TypedKey;

/**
 * The Pup Board pins this mission introduces. See {@code demos/package-info.java}.
 *
 * <p>One pin per clue rather than one list, deliberately: an agent's output OVERWRITES its key, so
 * five Rangers writing one list would keep only the last clue. One pin each is how a board
 * accumulates — and it is what lets a pin be the PRECONDITION of somebody else's contribution.
 */
public final class Keys {

    private Keys() {
    }

    /** Sniff: where the scent goes. Dig cannot start without it. */
    public record ScentClue() implements TypedKey<String> {}

    /** Bolt: the camera log. Doc cannot start without it. */
    public record CameraClue() implements TypedKey<String> {}

    /** Dig: what the prints in the drain say. */
    public record TunnelClue() implements TypedKey<String> {}

    /** Doc: where the crumb in Zao's beard came from. */
    public record CrumbClue() implements TypedKey<String> {}

    /** Zao's ruling. Writing this is the goal state. */
    public record Culprit() implements TypedKey<String> {}
}
