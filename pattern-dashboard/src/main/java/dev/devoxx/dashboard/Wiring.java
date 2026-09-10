package dev.devoxx.dashboard;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.model.chat.ChatModel;

/** Building an agent and getting values back out of the scope — the two lines every pattern
 * repeats. */
final class Wiring {

    private Wiring() {
    }

    static <T> T agent(Class<T> cls, ChatModel model, String name, String outputKey) {
        var b = AgenticServices.agentBuilder(cls).chatModel(model).name(name);
        if (outputKey != null) {
            b = b.outputKey(outputKey);
        }
        return b.build();
    }

    static String str(AgenticScope s, String key) {
        Object v = s.readState(key, "");
        return v == null ? "" : v.toString();
    }

    static String result(ResultWithAgenticScope<?> r, String preferKey) {
        if (preferKey != null && r.agenticScope() != null && r.agenticScope().hasState(preferKey)) {
            return String.valueOf(r.agenticScope().readState(preferKey));
        }
        return String.valueOf(r.result());
    }
}
