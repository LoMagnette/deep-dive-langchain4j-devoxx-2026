package dev.devoxx.dashboard.demos._11_p2p;

import dev.langchain4j.agentic.declarative.TypedKey;

/**
 * The scope keys this demo introduces. See {@code demos/package-info.java}.
 *
 * <p>Two keys, one per peer, and each peer reads the <b>other's</b>. That shape is not
 * decoration and it is not the obvious one — the obvious one is a single shared draft both of
 * them write, which is what this was tried as first. {@code P2PPlanner} is reactive: an agent
 * re-fires whenever an input of its changes, and with one key both peers fire on their own
 * writes as well as each other's, race, and run to the round cap with the draft oscillating.
 * Distinct keys give the ping-pong a direction without giving either peer authority.
 */
public final class Keys {

    private Keys() {
    }

    public record Question() implements TypedKey<String> {}

    /** Where the bed half has got to. Read by the floor half, never written by it. */
    public record Proposal() implements TypedKey<String> {}

    /** Where the floor half has got to. Read by the bed half, never written by it. */
    public record Counter() implements TypedKey<String> {}
}
