/**
 * <b>Custom planner</b>
 *
 * <p>Declared cheapest-first, because that order IS the policy in
 * {@link EscalationPlanner}. Each tier ends ANSWERED or ESCALATE, and the Java decides the cost.
 */
package dev.devoxx.dashboard.demos._16_customplanner;
