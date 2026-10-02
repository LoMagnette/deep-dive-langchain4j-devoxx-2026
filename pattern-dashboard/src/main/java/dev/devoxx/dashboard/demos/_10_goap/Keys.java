package dev.devoxx.dashboard.demos._10_goap;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The Pup Board pins this mission introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    /** Dig holding the ladder steady at the foot of the tower. Needed before anyone climbs. */
    public record LadderSecured() implements TypedKey<String> {}

    /** The goal. When this pin exists, the planner stops. */
    public record CatSafe() implements TypedKey<String> {}
}
