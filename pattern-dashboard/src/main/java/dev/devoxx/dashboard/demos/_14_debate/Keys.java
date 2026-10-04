package dev.devoxx.dashboard.demos._14_debate;

import dev.langchain4j.agentic.declarative.TypedKey;
import dev.langchain4j.agentic.patterns.debate.DebatePlanner;

/** The Pup Board pins this mission introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    /** What the council is deciding. */
    public record Motion() implements TypedKey<String> {}

    /**
     * The previous round's statements, one line per debater — written by {@code DebatePlanner}
     * itself, not by any agent. The one key in the demos whose {@code name()} is overridden, and
     * the reason is the rule: the name belongs to the LIBRARY, so it is the library's constant,
     * and a typo here would leave both debaters reading nothing.
     */
    public record DebateContext() implements TypedKey<String> {
        @Override
        public String name() {
            return DebatePlanner.DEBATE_CONTEXT_KEY;
        }
    }

    /** Howl's latest statement. */
    public record HowlTurn() implements TypedKey<String> {}

    /** Mittens' latest statement. */
    public record MittensTurn() implements TypedKey<String> {}

    /** Fifi's ruling. */
    public record Verdict() implements TypedKey<String> {}
}
