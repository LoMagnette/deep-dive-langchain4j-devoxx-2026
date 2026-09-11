package dev.devoxx.dashboard.support;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.model.chat.ChatModel;

/** Building an agent and getting values back out of the scope — the two lines every pattern
 * repeats. */
public final class Wiring {

    private Wiring() {
    }

    public static <T> T agent(Class<T> cls, ChatModel model, String name, String outputKey) {
        var b = AgenticServices.agentBuilder(cls).chatModel(model).name(name);
        if (outputKey != null) {
            b = b.outputKey(outputKey);
        }
        return b.build();
    }

    public static String str(AgenticScope s, String key) {
        Object v = s.readState(key, "");
        return v == null ? "" : v.toString();
    }

    public static String result(ResultWithAgenticScope<?> r, String preferKey) {
        if (preferKey != null && r.agenticScope() != null && r.agenticScope().hasState(preferKey)) {
            return String.valueOf(r.agenticScope().readState(preferKey));
        }
        return String.valueOf(r.result());
    }
}
