package dev.devoxx.dashboard.demos._18_lakeparty;

import java.util.List;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The Pup Board pins this mission introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    /** The spots of ice Sniff is sent to, one search each. */
    public record Spots() implements TypedKey<List<String>> {}

    /** One spot's finding, before the mapper gathers them. */
    public record Finding() implements TypedKey<String> {}

    /** Every spot's finding. A List, never a String — scope values are passed through, never coerced. */
    public record Findings() implements TypedKey<List<String>> {}

    /** The count, with both rules — Mission 13's aggregation, pinned so Howl can read it. */
    public record IceVerdict() implements TypedKey<String> {}

    /** What Barkville is told. */
    public record Announcement() implements TypedKey<String> {}
}
