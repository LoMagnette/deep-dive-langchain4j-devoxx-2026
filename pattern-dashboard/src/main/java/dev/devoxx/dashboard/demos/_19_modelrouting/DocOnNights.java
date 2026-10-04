package dev.devoxx.dashboard.demos._19_modelrouting;

import dev.devoxx.dashboard.demos._06_conditional.Keys.Call;
import dev.devoxx.dashboard.demos._06_conditional.Keys.Response;
import dev.langchain4j.agentic.Agent;
import dev.devoxx.dashboard.demos._06_conditional.Keys.Category;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.agentic.declarative.ChatModelSupplier;
import dev.devoxx.dashboard.run.ModelTiers;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface DocOnNights {
    @Agent(name = "Doc",
           typedOutputKey = Response.class,
           description = "Doc the St. Bernard, on the night phone: answers every call, whatever it is")
    @UserMessage("""
            You are Doc, on the Pup HQ night phone, and you answer every call yourself tonight.
            Answer this one directly and practically: what to do right now, and whether Officer
            Jo needs waking. Keep it short.

            The call: {{Call}}""")
    String answer(@K(Call.class) String call);

    /**
     * THE method. A {@code @ChatModelSupplier} with parameters is resolved when Doc is INVOKED,
     * not when he is built — by which time Zao has written the category, bound here by
     * {@code @K} exactly as an agent's input is. The declarative form of
     * {@code chatModel(Function<AgenticScope, ChatModel>)}.
     */
    @ChatModelSupplier
    static ChatModel model(@K(Category.class) String category) {
        ModelTiers tiers = CurrentRun.tiers();
        return ModelRoutingPattern.serious(category) ? tiers.strong() : tiers.cheap();
    }
}
