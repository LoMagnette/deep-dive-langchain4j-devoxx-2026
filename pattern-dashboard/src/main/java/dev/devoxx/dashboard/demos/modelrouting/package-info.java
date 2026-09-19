/**
 * <b>Dynamic chat model selection</b>
 *
 * <p>{@code AgentBuilder.chatModel(...)} has an overload taking a
 * {@code Function<AgenticScope, ChatModel>}, so the model is chosen when the agent is invoked
 * rather than when it is built — from whatever the scope holds by then.
 *
 * <p>The demo deliberately has <b>one</b> answering agent, with one prompt. Routing to three
 * different specialists is demo 6 and the room has already seen it; here nothing changes between
 * runs except which model is behind the same agent. That is the whole subject, and putting a
 * second agent on the page would let it be mistaken for routing.
 */
package dev.devoxx.dashboard.demos.modelrouting;
