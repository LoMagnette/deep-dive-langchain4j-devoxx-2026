package dev.devoxx.dashboard.demos._06_conditional;

import dev.devoxx.dashboard.demos._06_conditional.Keys.Answer;
import dev.devoxx.dashboard.demos._06_conditional.Keys.Category;
import dev.devoxx.dashboard.demos._06_conditional.Keys.Worry;
import dev.devoxx.dashboard.support.Parsing;
import dev.langchain4j.agentic.declarative.ActivationCondition;
import dev.langchain4j.agentic.declarative.ConditionalAgent;
import dev.langchain4j.agentic.declarative.K;

/**
 * The three desks and the rule that picks one. Each {@code @ActivationCondition} takes the
 * router's answer as an argument rather than reading it off a scope — and {@link Parsing#category}
 * is still ours, because normalising "This is best categorised as: **medical**." into one word is
 * not something the framework can do for you.
 */
public interface ConditionalDesks {

    @ConditionalAgent(name = "Desks",
                      subAgents = {EmergencyVet.class, DogTrainer.class, EverydayCare.class},
                      typedOutputKey = Answer.class)
    String ask(@K(Worry.class) String worry, @K(Category.class) String category);

    @ActivationCondition(value = EmergencyVet.class, description = "the worry is medical")
    static boolean emergency(@K(Category.class) String category) {
        return Parsing.category(category).equals("emergency");
    }

    @ActivationCondition(value = DogTrainer.class, description = "the worry is behaviour")
    static boolean training(@K(Category.class) String category) {
        return Parsing.category(category).equals("training");
    }

    @ActivationCondition(value = EverydayCare.class, description = "ordinary dog admin")
    static boolean everyday(@K(Category.class) String category) {
        return Parsing.category(category).equals("everyday");
    }
}
