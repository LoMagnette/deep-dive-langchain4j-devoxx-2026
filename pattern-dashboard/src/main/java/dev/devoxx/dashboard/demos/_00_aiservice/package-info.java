/**
 * <b>Mission 0 · A plain AI service: The Pup HQ Front Desk</b>
 *
 * <p>Before the pack, one pup at a desk — and no agentic module at all. {@code AiServices} turns
 * an interface into an LLM call, and the three things worth showing are all on its builder:
 * tools the model chooses to call, an input guardrail that stops a message before the model
 * sees it, and an output guardrail that sends an answer back for a rewrite. There is no Pup Board
 * here: an AI service has no scope, which is exactly what Mission 1's agent adds.
 */
package dev.devoxx.dashboard.demos._00_aiservice;
