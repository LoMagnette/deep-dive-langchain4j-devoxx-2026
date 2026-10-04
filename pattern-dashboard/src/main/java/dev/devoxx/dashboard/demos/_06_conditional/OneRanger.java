package dev.devoxx.dashboard.demos._06_conditional;

import static dev.devoxx.dashboard.support.Parsing.category;

import dev.devoxx.dashboard.demos._06_conditional.Keys.Category;
import dev.langchain4j.agentic.declarative.ActivationCondition;
import dev.langchain4j.agentic.declarative.ConditionalAgent;
import dev.langchain4j.agentic.declarative.K;

/**
 * The conditional step, declared: four Rangers on the bench, one sent. Each branch's condition is
 * a static method that names the Ranger it activates, and reads Zao's category through {@code @K}.
 */
public interface OneRanger {

    @ConditionalAgent(name = "Conditional",
                      subAgents = {SniffOnCall.class, DigOnCall.class, DocOnCall.class, ZoomOnCall.class})
    String answer();

    @ActivationCondition(value = SniffOnCall.class, description = "something is lost")
    static boolean lost(@K(Category.class) String category) {
        return "lost".equals(category(category));
    }

    @ActivationCondition(value = DigOnCall.class, description = "someone is stuck underground")
    static boolean underground(@K(Category.class) String category) {
        return "underground".equals(category(category));
    }

    @ActivationCondition(value = DocOnCall.class, description = "someone is hurt")
    static boolean hurt(@K(Category.class) String category) {
        return "hurt".equals(category(category));
    }

    @ActivationCondition(value = ZoomOnCall.class, description = "it is urgent")
    static boolean urgent(@K(Category.class) String category) {
        return "urgent".equals(category(category));
    }
}
