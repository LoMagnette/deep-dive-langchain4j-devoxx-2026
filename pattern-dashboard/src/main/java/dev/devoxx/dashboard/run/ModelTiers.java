package dev.devoxx.dashboard.run;

import dev.langchain4j.model.chat.ChatModel;

/**
 * The two models a run may choose between: a cheap one and a strong one.
 *
 * <p>It exists for exactly one demo — {@code demos._19_modelrouting}, which picks a model per
 * invocation with {@code AgentBuilder.chatModel(Function<AgenticScope, ChatModel>)}. Sixteen
 * other demos want one model and get it as the {@code Runner}'s first argument; widening that
 * signature for the seventeenth would have cost every demo a parameter it never reads.
 *
 * <p>{@code distinct} is the honest bit. Two tiers only mean something when they really are two
 * models, and on a laptop with one model pulled they are not. The demo says so in its result
 * rather than quietly implying a choice that never happened — a demo that claims to save money
 * while calling the same model twice is worse than no demo.
 *
 * @param cheap      the model for questions where being wrong is cheap
 * @param cheapName  what to call it on screen
 * @param strong     the model for questions where being wrong is expensive
 * @param strongName what to call it on screen
 * @param distinct   false when both tiers are really the same model
 */
public record ModelTiers(ChatModel cheap, String cheapName, ChatModel strong, String strongName,
                         boolean distinct) {

    /** One model doing both jobs — the common case, and it must not pretend otherwise. */
    public static ModelTiers single(ChatModel model, String name) {
        return new ModelTiers(model, name, model, name, false);
    }

    /** Two genuinely different models. */
    public static ModelTiers of(ChatModel cheap, String cheapName,
                                ChatModel strong, String strongName) {
        return new ModelTiers(cheap, cheapName, strong, strongName, true);
    }

    /** What to print under a result so the room knows whether it watched a real choice. */
    public String note() {
        return distinct
                ? "cheap tier: " + cheapName + " · strong tier: " + strongName
                : "both tiers are the same model (" + cheapName
                        + ") — set dashboard.ollama.cheap-model-name to see two";
    }
}
