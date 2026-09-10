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
        Map<String, ScopeValue> scope,
        Object data) {

    /**
     * One entry of the agentic scope, shaped for the dashboard's variables table.
     *
     * <p>Carries the declared {@code type} and {@code size} alongside the rendered value because
     * a debugger view without them is just a wall of strings — and here the type is part of the
     * lesson: {@code score} shows up as a String, not a Double, which is exactly why
     * {@code Agents.FridgeRuleCheck} returns one (see its javadoc).
     *
     * @param size characters for text, elements for a collection, else null
     */
    public record ScopeValue(String type, Integer size, String value) {
    }

    public static RunEvent of(long seq, String type, String agent, String message,
                              Map<String, ScopeValue> scope, Object data) {
        return new RunEvent(seq, type, agent, message, scope, data);
    }
}
