package dev.devoxx.dashboard.demos._11_p2p;

import dev.langchain4j.agentic.declarative.TypedKey;

/**
 * The Pup Board pins this mission introduces. See {@code demos/package-info.java}.
 *
 * <p>One pin per pup, and each pup listens to the OTHERS' pins, never its own. That shape is the
 * whole wiring: {@code P2PPlanner} re-runs a pup whenever a pin it reads changes, so who listens
 * to what IS the coordination. (A pup that read its own pin would wake itself up for ever.)
 */
public final class Keys {

    private Keys() {
    }

    /** What Sniff's nose says, and — once it is — "FOUND:". Zoom and Dig both listen to it. */
    public record Scent() implements TypedKey<String> {}

    /** What Zoom has run through and seen. Sniff listens to it. */
    public record Clearing() implements TypedKey<String> {}

    /** What Dig found underground and under the hedges. Sniff listens to it. */
    public record Burrows() implements TypedKey<String> {}
}
