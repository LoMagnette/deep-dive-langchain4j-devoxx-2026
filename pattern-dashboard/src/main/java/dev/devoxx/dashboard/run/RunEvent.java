package dev.devoxx.dashboard.run;

import java.util.Map;

/**
 * A single streamed event describing what an agentic run is doing.
 * types: run-start, agent-before, agent-after, agent-error, human-ask, human-answer,
 * token, run-result, run-done.
 *
 * <p>{@code token} is one chunk of an answer being generated, carried in {@code data}. It is the
 * only event type with no {@code message}: there is nothing to say about a token that the token
 * does not already say, and the page appends it to the Result pane rather than logging it.
 *
 * <p>{@code millis} is how long the step took, and is null on the events where that means
 * nothing (an agent starting, a question being asked). It is the cheapest observability there
 * is, and on stage it is the whole argument for half the catalogue: a parallel run whose two
 * branches take 900ms each and whose total is 950ms has demonstrated the pattern in a way no
 * diagram can.
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
     *
     * <p>Carries the declared {@code type} and {@code size} alongside the rendered value because
     * a debugger view without them is just a wall of strings — and here the type is part of the
     * lesson: {@code score} shows up as a String, not a Double, which is exactly why
     * {@code agents.workflow.FridgeRuleCheck} returns one (see its javadoc).
     *
     * @param size characters for text, elements for a collection, else null
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
