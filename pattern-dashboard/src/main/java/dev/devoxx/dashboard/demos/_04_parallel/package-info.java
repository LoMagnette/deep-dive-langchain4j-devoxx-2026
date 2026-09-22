/**
 * <b>Parallel</b>
 *
 * <p>Two checks that plainly do not need each other, and both of which must pass before you put the
 * lead on. That is fan-out and join — and the join is a DECISION (either check can veto), not a
 * string concatenation.
 */
package dev.devoxx.dashboard.demos._04_parallel;
