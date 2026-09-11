/**
 * All agent contracts used by the dashboard, <b>one interface per file</b>. The setting is
 * <b>Zao</b>, a Belgian shepherd, and the household he runs — so the talk's "From Puppy to Pack"
 * thread holds from the puppy's first hour to the question of whether to get a second dog.
 *
 * <p>The sub-packages mirror the catalogue exactly — {@code workflow}, {@code pureagent},
 * {@code zoo}, {@code composite} — so "where does this agent live?" has the same answer as
 * "where does this pattern live?". An agent lives where it is first introduced, and several are
 * reused from there: the three assessors vote in {@code zoo} and ratify in {@code composite},
 * and {@code FridgeRuleCheck} is both the loop's critic and the capstone's. That reuse is the
 * point of these being contracts rather than prompts inlined at the call site.
 *
 * <p>Two rules decide every scenario in here, and they pull against each other:
 *
 * <p><b>1. The pattern must be load-bearing.</b> Take it away and the answer visibly degrades:
 * the vote must be able to split, the critic must have rules to check, the planner must have an
 * order to discover. A demo where one plain prompt would do as well teaches the wiring and
 * nothing else.
 *
 * <p><b>2. The audience must not need the domain explained.</b> Everyone knows chocolate is bad
 * for dogs, that hot pavement burns paws, that a puppy needs the garden before he needs a
 * training session, and that neither half of a couple outranks the other about the bed. Nobody
 * knows what a 21-day rabies clearance is. A scenario that costs a sentence of setup costs it
 * fifteen times over, and the room spends the talk learning the domain instead of the patterns.
 *
 * <p>So the constraints these agents are checked against are ones the room already holds:
 * grapes are dangerous and cheddar is not, a fridge note needs the vet's number on it, you
 * practise recall in the garden before the park.
 *
 * <p>The {@code @UserMessage} prompts are worded so the deterministic
 * {@link dev.devoxx.dashboard.model.MockChatModel} returns parseable output (scores, labels, PASS/FAIL lines, votes). Change the wording and check
 * the rule that matched it — the mock's table keys off phrases like "0.0", "classify this worry",
 * "YES or LATER".
 */
package dev.devoxx.dashboard.agents;
