/**
 * <b>Custom planner</b>
 *
 * <p>Declared cheapest-first, because that order IS the policy in EscalationPlanner. Each one ends
 * with ANSWERED or ESCALATE, which is the only thing the planner reads: the model makes a local
 * judgement about its own competence, and the Java decides what that costs. Everyone already
 * knows this ladder — you look it up, then you ring the trainer, then you ring the vet — and
 * everyone knows you do not start at the vet to ask about kibble.
 */
package dev.devoxx.dashboard.demos._16_customplanner;
