/**
 * <b>One package per demo.</b> Each one holds everything that demo needs and nothing else: its
 * agent contracts (one interface per file), its {@code XxxPattern} wiring, and a
 * {@code package-info} saying what the demo is for. The package is named after the pattern id,
 * so the deep link on a slide ({@code #/loop}) names the package to open on stage
 * ({@code demos.loop}).
 *
 * <p>The setting is <b>Zao</b>, a Belgian shepherd, and the household he runs — so the talk's
 * "From Puppy to Pack" thread holds from the puppy's first hour to the question of whether to
 * get a second dog.
 *
 * <p><b>An agent lives in the demo that introduces it</b>, and later demos import it from there.
 * That is deliberate and is worth pointing at on stage: {@code sitternote} imports the loop's
 * {@code FridgeRuleCheck} and the routing demo's {@code WorryRouter}, and
 * {@code seconddogcouncil} imports the three assessors {@code voting} introduced. A composite
 * reuses the parts rather than re-implementing them, and the import list says so before a word
 * of explanation.
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
 * twenty times over, and the room spends the talk learning the domain instead of the patterns.
 *
 * <p>So the constraints these agents are checked against are ones the room already holds:
 * grapes are dangerous and cheddar is not, a fridge note needs the vet's number on it, you
 * practise recall in the garden before the park.
 *
 * <p>The {@code @UserMessage} prompts are worded so the deterministic
 * {@link dev.devoxx.dashboard.model.MockChatModel} returns parseable output (scores, labels, PASS/FAIL lines, votes). Change the wording and check
 * the rule that matched it — the mock's table keys off phrases like "0.0", "classify this worry",
 * "YES or LATER".
 *
 * <p><b>Scope keys are {@code TypedKey} records</b>, one {@code Keys.java} per demo, imported by
 * later demos the way agents are. A key is the contract between two agents that never see each
 * other, and nothing checks the spellings match. Four things to know before writing one:
 * they must be records (the framework instantiates a key to ask its name, so an interface fails
 * with "doesn't have a no-args constructor"); each overrides {@code name()} to return the
 * lowercase string, which is why the {@code @V} parameters and the placeholders in the prompts
 * are untouched; the input side is still bound by name, so {@code @V} and
 * {@code HumanInTheLoopBuilder.outputKey} take {@code new Draft().name()}; and a typed read
 * returns {@code null} when the key is absent rather than falling back to
 * {@code defaultValue()}, which is a builder-level mechanism.
 */
package dev.devoxx.dashboard.demos;
