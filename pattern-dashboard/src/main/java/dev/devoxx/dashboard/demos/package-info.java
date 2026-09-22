/**
 * <b>One package per demo.</b> Each one holds everything that demo needs and nothing else: its
 * agent contracts (one interface per file), its {@code XxxPattern} wiring, and a
 * {@code package-info} saying what the demo is for. A package is {@code _NN_<id>} — its place in
 * the running order, then the pattern id — so the tree reads in talk order and the deep link on a
 * slide ({@code #/loop}) still names the package to open on stage ({@code demos._03_loop}). The
 * leading underscore is Java's requirement, not a style choice: a package segment cannot start
 * with a digit.
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
 * for dogs, that a dog who suddenly starts snapping is a vet question and not a training one,
 * that a puppy needs the garden before he needs a training session, and that neither half of a
 * couple outranks the other about the bed. Nobody
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
 * with "doesn't have a no-args constructor"); <b>a key declares nothing but its type</b> —
 * {@code TypedKey.name()} defaults to the record's simple name, so {@code Notes} is the key
 * {@code "Notes"} and the prompt placeholder is spelled the same way; <b>the input side is typed
 * too</b> — a parameter takes {@code @K(Notes.class)}, not {@code @V("Notes")}, so
 * neither end of the contract is a string the compiler cannot see; and a typed read returns
 * {@code null} when the key is absent rather than falling back to {@code defaultValue()}, which
 * on the <i>input</i> side {@code @K} does honour.
 *
 * <p>Two places still spell a key out, and both are the API's limit rather than a choice:
 * {@code HumanInTheLoopBuilder} has no {@code TypedKey} overload where {@code AgentBuilder} does,
 * so those sites read {@code new Draft().name()}; and the parallel mapper's item is bound to the
 * sub-agent's <i>first argument</i> positionally, so {@code @V("food")} and {@code @V("angle")}
 * name nothing in the scope and deliberately have no {@code Keys} entry. They are the only
 * {@code @V} left in the demos, which is worth pointing at on stage: the typing is only ever as
 * good as the narrowest API you touch.
 */
package dev.devoxx.dashboard.demos;
