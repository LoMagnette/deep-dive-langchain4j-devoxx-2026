package dev.devoxx.dashboard.demos._08_nonaiagent;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The scope keys this demo introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    /**
     * The cat's record of where everything was buried: facts that exist somewhere authoritative
     * and must never be generated. A non-AI agent writes this key, and nothing downstream can
     * tell the difference — which is the point.
     */
    public record Facts() implements TypedKey<String> {}

    /** What the humans want dug up. */
    public record Mission() implements TypedKey<String> {}

    /** The pack's dig plan — written by the model, then checked and amended by the cat. */
    public record Plan() implements TypedKey<String> {}
}
