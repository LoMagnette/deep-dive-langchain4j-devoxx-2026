package dev.devoxx.dashboard.demos._01_single;

import dev.langchain4j.agentic.declarative.TypedKey;

/** The Pup Board pins this mission introduces. See {@code demos/package-info.java}. */
public final class Keys {

    private Keys() {
    }

    /** What the Ranger has been asked to do, in the words of whoever asked. */
    public record Mission() implements TypedKey<String> {}

    /** Where Sniff found it. Read by Zoom in Mission 2. */
    public record Location() implements TypedKey<String> {}
}
