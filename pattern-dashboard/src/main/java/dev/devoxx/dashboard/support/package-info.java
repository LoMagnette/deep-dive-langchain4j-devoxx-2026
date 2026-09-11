/**
 * The small shared pieces every pattern repeats.
 *
 * <p>{@code Wiring} is the builder boilerplate. {@code Parsing} holds defensive readers for what
 * a model <i>actually</i> returns — every method there exists because a real model broke a
 * pattern in a way that produced no error at all. {@code Errors} flattens a cause chain, so that
 * a dead Ollama and a parse failure do not look identical on stage.
 */
package dev.devoxx.dashboard.support;
