package dev.devoxx.dashboard.demos.goap;

import dev.langchain4j.agentic.declarative.TypedKey;

/**
 * The scope keys this demo introduces, as types rather than strings.
 *
 * <p>A key is the contract between two agents that never see each other: one writes
 * it, another reads it, and nothing checks that the two spellings match. This repo has
 * already paid for that twice — a run that died with
 * {@code MissingArgumentException: Missing argument: note} because one place said
 * {@code note} and another {@code notes}, and a mapper whose findings were declared a
 * {@code String} when the scope held a {@code List}. A {@link TypedKey} makes the name a
 * compile-checked identifier and the type a declaration.
 *
 * <p>Each one overrides {@code name()} to return the lowercase string it has always
 * used, so the {@code @V} parameters and the placeholders in the agent prompts are
 * unchanged. The binding on the input side is still by name, and that limit is worth
 * saying out loud on stage.
 *
 * <p>They are records because the framework instantiates a key to ask its name: it needs
 * a public, concrete, no-args-constructible type, and an interface will not do.
 */
public final class Keys {

    private Keys() {
    }

    /**
     * The garden step, which the park step needs.
     */
    public record Garden() implements TypedKey<String> {
        @Override
        public String name() {
            return "garden";
        }
    }

    /**
     * What the owner wants taught.
     */
    public record Goal() implements TypedKey<String> {
        @Override
        public String name() {
            return "goal";
        }
    }

    /**
     * The indoor step, which the garden step needs.
     */
    public record Indoor() implements TypedKey<String> {
        @Override
        public String name() {
            return "indoor";
        }
    }

    /**
     * The park step, which is the goal.
     */
    public record Park() implements TypedKey<String> {
        @Override
        public String name() {
            return "park";
        }
    }
}
