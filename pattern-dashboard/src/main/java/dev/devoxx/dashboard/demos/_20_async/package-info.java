/**
 * <b>Asynchronous agents</b>
 *
 * <p>One step in a plain sequence is marked {@code async(true)}, so it starts and the sequence
 * carries on without it. Nothing else about the wiring changes: same builder, same declaration
 * order, same agents.
 *
 * <p>The thing worth seeing is <b>where the waiting happens</b>. An async agent writes an
 * {@code AsyncResponse} into the scope, and the next read of that key blocks until it resolves —
 * so the join is not a step you declare, it is the line that reads the value. Everything between
 * the start and that read is free.
 */
package dev.devoxx.dashboard.demos._20_async;
