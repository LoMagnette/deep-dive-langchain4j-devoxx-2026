package dev.devoxx.dashboard.demos._12_blackboard;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The scope keys this demo introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    /** Who could physically have done it. The Collie's note on the board. */
    public record Alibis() implements TypedKey<String> {}

    /** What happened, as reported. The only thing the three investigators read. */
    public record Crime() implements TypedKey<String> {}

    /** Who did it, ranked. Writing this is the goal state. */
    public record Ruling() implements TypedKey<String> {}

    /** What the evidence left behind says. The Shepherd's note on the board. */
    public record Scene() implements TypedKey<String> {}

    /** Where the crumbs go. The Bloodhound's note on the board. */
    public record Trail() implements TypedKey<String> {}
}
