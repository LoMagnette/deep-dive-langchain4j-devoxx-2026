/**
 * <b>One package per mission</b>, holding its agent contracts, its {@code Keys} (the Pup Board
 * pins it introduces), its {@code XxxPattern} and a {@code package-info}. A package is
 * {@code _NN_<id>} — the mission number, then the pattern id — so the tree reads in mission order
 * and {@code #/loop} names {@code demos._03_loop}. The leading underscore is Java's: a package
 * segment cannot start with a digit.
 *
 * <p><b>The agents are the Pawer Rangers</b>, and an agent's {@code .name(...)} is its Ranger:
 * Sniff finds, Zoom runs, Dig digs, Doc decides what is safe, Howl writes and argues, Fifi
 * judges, Zao leads, and Bolt — a plain Java class, no model — does the maths. The interface name
 * says the job in this mission ({@code SniffFinds}, {@code ZoomFetchesLadder}), because the same
 * Ranger has a different job in different missions; the name on the diagram stays the Ranger's,
 * so the room learns the cast once and then only has to learn the pattern.
 */
package dev.devoxx.dashboard.demos;
