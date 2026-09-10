package dev.devoxx.dashboard;

import java.util.Map;

/**
 * A single streamed event describing what an agentic run is doing.
 * types: run-start, agent-before, agent-after, agent-error, run-result, run-done.
 */
public record RunEvent(
        long seq,
        String type,
        String agent,
        String message,
        Map<String, Object> scope,
        Object data) {

    public static RunEvent of(long seq, String type, String agent, String message,
                              Map<String, Object> scope, Object data) {
        return new RunEvent(seq, type, agent, message, scope, data);
    }
}
