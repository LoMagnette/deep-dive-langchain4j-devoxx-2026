package dev.devoxx.dashboard.demos._20_async;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The scope keys this demo introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    /**
     * What the out-of-hours line came back with.
     *
     * <p>While the async agent is still running, the scope holds an {@code AsyncResponse} under
     * this key rather than a String — which is why the Scope tab shows it as {@code <pending>}
     * for the first part of the run. Reading it is what blocks.
     */
    public record VetLine() implements TypedKey<String> {}
}
