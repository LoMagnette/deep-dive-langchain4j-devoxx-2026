/**
 * <b>Mission 14 · Debate: Dog Park vs. Cat Café</b>
 *
 * <p>At the town council, Howl argues for a dog park and Mittens for a cat café, for three rounds,
 * and Fifi rules. Wired with LangChain4j's own {@code DebatePlanner}: every sub-agent but the last
 * is a debater, the last is the judge, and between rounds the planner writes the previous round's
 * statements into {@code debateContext} for everyone to answer. Let the room vote before Fifi does.
 */
package dev.devoxx.dashboard.demos._14_debate;
