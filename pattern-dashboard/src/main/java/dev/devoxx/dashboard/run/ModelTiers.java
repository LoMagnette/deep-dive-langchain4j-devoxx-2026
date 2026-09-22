package dev.devoxx.dashboard.run;

import dev.langchain4j.model.chat.ChatModel;

/**
 * The two models a run may choose between: a cheap one and a strong one.
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
