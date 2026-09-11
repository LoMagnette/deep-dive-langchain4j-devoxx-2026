/**
 * The small shared pieces that are <b>ours</b>, not LangChain4j's.
 *
 * <p>That distinction is the rule for this package, and it is worth keeping: anything that is
 * LangChain4j API stays inlined in the demo wiring where the room can read it. Building an agent
 * used to live here behind a one-line {@code agent(...)} helper, which made the demos shorter and
 * hid the single most important call in the library from a talk about that library.
 *
 * <p>{@code Parsing} holds defensive readers for what a model <i>actually</i> returns — a score as
 * prose, a category wrapped in a sentence, a list that was not one. Every method exists because a
 * real model broke a pattern in a way that produced no error at all. They take plain strings, so
 * the scope read stays visible at the call site. {@code Errors} flattens a cause chain, so that a
 * dead Ollama and a parse failure do not look identical on stage.
 */
package dev.devoxx.dashboard.support;
