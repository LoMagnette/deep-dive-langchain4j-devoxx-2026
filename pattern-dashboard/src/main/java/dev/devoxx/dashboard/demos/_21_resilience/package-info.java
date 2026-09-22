/**
 * <b>Optional agents and error handling</b>
 *
 * <p>Two different answers to "what happens when a step does not produce a value", which are
 * easy to confuse because both of them keep the run alive:
 *
 * <ul>
 *   <li><b>{@code optional(true)}</b> is about a <i>missing input</i>. The agent is skipped when
 *       an argument it declares is not in the scope. It has nothing to do with the agent
 *       failing — a step that throws is not optional's problem, however optional it is.</li>
 *   <li><b>{@code errorHandler(...)}</b> is about a <i>failing call</i>. It sees every
 *       {@code AgentInvocationException} in the run and chooses: retry, substitute a value, or
 *       rethrow.</li>
 * </ul>
 *
 * <p>The demo does one of each in the same run, on the note that has to reach the fridge door
 * whatever else goes wrong.
 */
package dev.devoxx.dashboard.demos._21_resilience;
