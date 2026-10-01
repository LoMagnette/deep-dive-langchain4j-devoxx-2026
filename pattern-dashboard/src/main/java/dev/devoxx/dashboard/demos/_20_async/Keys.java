package dev.devoxx.dashboard.demos._20_async;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The scope keys this demo introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    /**
     * What the Basset found on his way round the fence. Slow to arrive, needed only at the end.
     */
    public record FenceReport() implements TypedKey<String> {}
}
