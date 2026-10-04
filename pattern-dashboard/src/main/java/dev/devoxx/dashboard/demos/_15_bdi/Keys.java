package dev.devoxx.dashboard.demos._15_bdi;

import dev.langchain4j.agentic.declarative.TypedKey;

/**
 * The Pup Board pins this mission introduces. See {@code demos/package-info.java}.
 *
 * <p>Beliefs come in two kinds here, and that is the point: {@code Beliefs} is what the radio
 * said before the run, {@code Lookout} is what Zoom SEES during it. A belief written mid-run is
 * what lets a higher desire become achievable halfway through a lower one's plan.
 */
public final class Keys {

    private Keys() {
    }

    /** What Zoom was told on the radio. The input. */
    public record Beliefs() implements TypedKey<String> {}

    /** What Zoom saw from the top of the riverbank — a belief revised mid-run. */
    public record Lookout() implements TypedKey<String> {}

    /** The rescue, step 1: how he got across with the bridge out. */
    public record Crossing() implements TypedKey<String> {}

    /** The rescue desire is satisfied once this exists. */
    public record Rescued() implements TypedKey<String> {}

    /** The squirrel desire is satisfied once this exists. */
    public record Treed() implements TypedKey<String> {}

    /** The nap desire is satisfied once this exists. */
    public record Napped() implements TypedKey<String> {}
}
