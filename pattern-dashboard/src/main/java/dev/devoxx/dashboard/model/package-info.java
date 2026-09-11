/**
 * Which {@code ChatModel} is live, and the deterministic stand-in for when none is.
 *
 * <p>{@code ModelFactory} discovers a local Ollama and falls back loudly; {@code MockChatModel}
 * is the no-network model that {@code mvn test} runs against, and the reason the demo still
 * works on conference wifi.
 */
package dev.devoxx.dashboard.model;
