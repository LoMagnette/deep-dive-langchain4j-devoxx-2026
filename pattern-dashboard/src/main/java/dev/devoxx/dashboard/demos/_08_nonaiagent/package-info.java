/**
 * <b>Non-AI agents</b>
 *
 * <p>An agent does not have to be a model. Any plain object with one {@code @Agent}-annotated
 * method can be handed straight to {@code subAgents(...)}: LangChain4j binds its {@code @K}
 * parameters from the scope, writes its return value to its output key, and reports it to the
 * listener — so a sequence cannot tell that one of its steps never called anything.
 *
 * <p>{@code HumanInTheLoop} (demo 7) is one of these, provided by the library. This demo is the
 * general case: <b>your own Java, as an agent</b>, on both ends of an LLM step. The file supplies
 * facts nobody should be generating, and the guard checks they survived the writing.
 *
 * <p>Which makes it the far left of the autonomy dial — the position where the model decides
 * nothing at all — and the answer to the question the whole talk is really asking: not "how much
 * autonomy can I have", but "which parts of this genuinely need a model".
 */
package dev.devoxx.dashboard.demos._08_nonaiagent;
