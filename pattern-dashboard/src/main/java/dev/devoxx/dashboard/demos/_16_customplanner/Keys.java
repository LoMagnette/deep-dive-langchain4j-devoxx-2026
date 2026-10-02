package dev.devoxx.dashboard.demos._16_customplanner;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The Pup Board pins this mission introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    /** Today's missions and everyone's energy, as Officer Jo wrote them on the board. */
    public record Roster() implements TypedKey<String> {}

    /** Energy per Ranger, rewritten by the planner after every feed, nap and mission. */
    public record Energy() implements TypedKey<String> {}

    /** Who went last — the one Ranger who may not go next. */
    public record LastOnMission() implements TypedKey<String> {}

    /** What is still to do. */
    public record MissionQueue() implements TypedKey<String> {}

    /** Everything the planner decided, in order: the day as it happened. */
    public record Schedule() implements TypedKey<String> {}
}
