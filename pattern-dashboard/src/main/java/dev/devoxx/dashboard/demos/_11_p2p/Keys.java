package dev.devoxx.dashboard.demos._11_p2p;

import dev.langchain4j.agentic.declarative.TypedKey;

/**
 * The Pup Board pins this mission introduces. See {@code demos/package-info.java}.
 *
 * <p>Two pins, one per peer, and each peer reads the OTHER's. That shape is not decoration:
 * {@code P2PPlanner} is reactive — an agent re-fires whenever an input of its changes — so two
 * peers writing one shared pin trigger each other and themselves, race, and run to the cap.
 * Distinct pins give the conversation a direction without giving either pup authority.
 */
public final class Keys {

    private Keys() {
    }

    /** What Zoom has run through. Read by Sniff, written only by Zoom. */
    public record ClearedAreas() implements TypedKey<String> {}

    /** What Sniff's nose says, and — once it is — "FOUND:". Read by Zoom, written only by Sniff. */
    public record GoatSighting() implements TypedKey<String> {}
}
