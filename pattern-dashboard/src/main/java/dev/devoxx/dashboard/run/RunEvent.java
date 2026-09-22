package dev.devoxx.dashboard.run;

import java.util.Map;

/**
 * A single streamed event describing what an agentic run is doing.
 * types: run-start, agent-before, agent-after, agent-error, human-ask, human-answer,
 * token, run-result, run-done.
 */
public record RunEvent(
        long seq,
        String type,
        String agent,
        String message,
        Map<String, ScopeValue> scope,
        Object data,
        Long millis) {

    /**
     * One entry of the agentic scope, shaped for the dashboard's variables table.
     */
    public record ScopeValue(String type, Integer size, String value) {
    }

    public static RunEvent of(long seq, String type, String agent, String message,
                              Map<String, ScopeValue> scope, Object data) {
        return new RunEvent(seq, type, agent, message, scope, data, null);
    }

    /** Same, for a step whose duration is worth showing. */
    public static RunEvent of(long seq, String type, String agent, String message,
                              Map<String, ScopeValue> scope, Object data, Long millis) {
        return new RunEvent(seq, type, agent, message, scope, data, millis);
    }
}
