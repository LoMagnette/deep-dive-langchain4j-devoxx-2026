# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

Workspace for a Devoxx Belgium 3-hour deep-dive talk, "Agentic Systems in Java with LangChain4j."
Two distinct halves:

- **Root** (`README.md`) — talk planning: the through-line is "autonomy is a dial," told as "From Puppy
  to Pack." The `NN-*.md` planning docs referenced in the root README are the speaker's notes.
- **`pattern-dashboard/`** — the live demo: a Quarkus web app that visualizes and **runs** all 14
  LangChain4j agentic patterns, set in the life of **Zao**, a Belgian shepherd, and the household
  he runs. This is the code you will actually build and edit.

## Commands (run inside `pattern-dashboard/`)

```bash
mvn quarkus:dev                       # dev mode + live reload; needs Maven 3.9+. Open http://localhost:8080
mvn quarkus:dev -Ddashboard.model=mock  # no Ollama / no API key — deterministic offline run
mvn -DskipTests package               # build fast-jar to target/quarkus-app/
mvn test                              # smoke-runs all 15 patterns + both composites on the mock model
```

Point at a real model by overriding env vars (same code path as the default):
```bash
OPENAI_BASE_URL=https://api.openai.com/v1 OPENAI_API_KEY=sk-... OPENAI_MODEL=gpt-4o-mini mvn quarkus:dev
```

**Packaged-jar gotcha:** the fast-jar `Class-Path` fails to decode from a directory whose path contains
emoji/spaces (`Error decoding percent encoded characters`). Copy `target/quarkus-app` to a plain path
before `java -jar` (e.g. `cp -r target/quarkus-app /tmp/app && java -jar /tmp/app/quarkus-run.jar`).

## Stack notes that aren't obvious

- **Standard LangChain4j, NOT `quarkus-langchain4j`.** Agents are wired by hand via `AgenticServices`
  builders, not CDI/Quarkus LLM extensions. Versions are pinned in `pom.xml`: `langchain4j 1.20.0`,
  `langchain4j-agentic(-patterns) 1.20.0-beta30`. Java 21.
- The `langchain4j-agentic` module is **experimental / subject to change** — the speaker flags this
  on stage. Expect API churn when bumping versions.
- **Default model is `dashboard.model=auto`, against a local Ollama** via the native
  `langchain4j-ollama` client (no `/v1` suffix, no API key). The base URL is **discovered, not
  hardcoded**: `ModelFactory` probes `http://localhost:11434` (Ollama on this machine) then
  `http://host.docker.internal:11434` (app in a container, Ollama on the host). Hardcoding either
  breaks the other environment *invisibly* — a host that doesn't resolve fails exactly like a
  stopped Ollama. Set `OLLAMA_BASE_URL` to pin one endpoint and skip discovery.
- The probe is a cheap `GET /api/tags` — **never a real `chat()` call**, which runs inference and
  can take 10-20s on a thinking model, timing out while Ollama is perfectly healthy. It also
  checks the wanted model is in the returned list, so a typo or un-pulled model is reported at boot
  by name instead of surfacing mid-demo.
- On failure `auto` falls back to `MockChatModel` with a loud WARN, and **re-probes** on the next
  run (10s cooldown) so starting Ollama recovers without restarting the app. Every `run-start`
  event names the live model, so a fallback can't be mistaken for a real run. `dashboard.model=ollama`
  disables the fallback and fails loudly; `mock` forces offline.
  To run against Ollama: `ollama serve && ollama pull <model>`, then `mvn quarkus:dev`.

## Architecture

Backend is **one package per demo** under `src/main/java/dev/devoxx/dashboard/demos/`, plus a
few shared packages; the frontend is four static files (no build step). Every package carries a
`package-info.java` saying what it is for — read that first, it is the shortest path in.

```
demos/<id>/      EVERYTHING for one demo, and nothing else:
                   its agent contracts, one interface per file
                   its XxxPattern — topology + Runner
                   package-info.java — what this demo is for
  single/ sequential/ loop/ parallel/ parallelmapper/ conditional/ humanapproval/
  supervisor/
  goap/ p2p/ blackboard/ voting/ debate/ bdi/ customplanner/
  sitternote/ seconddogcouncil/
catalog/         PatternCatalog (the registry) · PatternDef · Topology
support/         Parsing · Errors — the shared pieces that are OURS, not LangChain4j's
model/           ModelFactory (which ChatModel is live) · MockChatModel (the offline one)
run/             RunEvent · StreamingListener — observing a run
web/             PatternResource · LogResource · LogStream — REST and SSE
```

- **The demo wiring shows the LangChain4j API, never a wrapper around it.** This is a talk about
  that API, so every call the room needs to learn is written out at the call site:
  `AgenticServices.agentBuilder(X.class).chatModel(model).name("X").outputKey("k").build()`,
  `scope.readState("k", "")`, `r.result()`. There used to be a `Wiring.agent(...)` helper that
  collapsed the first of those to one line; it made the demos shorter and hid the single most
  important call in the library. It is gone, and it should stay gone — the verbosity **is** the
  lesson, and the composites paying five lines per agent is the honest price of it.
  Two things that follow from this:
  - **`.name("X")` is load-bearing, not decoration.** An agent's default name is its *method*
    name (`check`, `rewrite`, `plan`), not its interface name — so without it the topology labels
    stop matching, `markNode` never lights a node, and the supervisor's canned plan cannot find
    `RoutinePlanner`. Nine tests go red at once if you drop it, which is how this was established.
  - **`support/Parsing` takes plain strings, not an `AgenticScope`.** Reading the scope is
    LangChain4j API and belongs in the demo; parsing a model's prose into a number is ours. So a
    loop's predicate reads `scope -> Parsing.score(scope.readState("score", "")) >= 0.8`, with
    the scope read visible where the room is looking.
- **The package is named after the pattern id**, lowercased. So the deep link on a slide
  (`#/loop`) names the package to open on stage (`demos.loop`), and `#/secondDogCouncil` is
  `demos.seconddogcouncil`. Keep that rule when adding a demo — it is the whole reason the
  packages are named this way rather than after the concept.
- **An agent lives in the demo that introduces it**, and later demos import it from there. That is
  deliberate, and worth pointing at on stage: `sitternote` imports the loop's `FridgeRuleCheck`
  and the routing demo's `WorryRouter`; `seconddogcouncil` imports the three assessors `voting`
  introduced. A composite reuses the parts rather than re-implementing them, and its import list
  says so before a word of explanation. Two shared default inputs work the same way —
  `SinglePattern.SITTER_MESSAGE` (also used by `sequential`) and `VotingPattern.HOUSEHOLD` (also
  used by the council).
- **`PatternCatalog` is the registry and nothing else**: sixteen `XxxPattern.define()` calls in
  the talk's running order, grouped by comments for the four rail categories. Adding a demo is a
  new package plus one line here.
- **Every step is timed, and the two numbers on screen are an argument.** `RunEvent.millis` is
  filled in for `agent-after`, `agent-error`, `human-answer`, `run-result` and `run-done`, and the
  page shows it three ways: under each node on the diagram, at the right of each line in the Run
  events pane, and as a **badge beside the Run and Reset buttons** carrying the whole run with
  how long the agents were busy inside it. The badge sits with the controls rather than in a dock
  tab so it is readable whichever pane is open, and it is **hidden until there has been a run** —
  `reset()` hides it, so switching pattern or resetting never leaves a stale number next to a Run
  button, where it would read as this run's. The gap between its two numbers is the whole case
  for half the catalogue, and it needs no explaining on stage:
  ```
  sequential       whole run 387 ms   agents busy 328 ms
  parallel         whole run 162 ms   agents busy 304 ms
  parallelMapper   whole run 165 ms   agents busy 783 ms
  ```
  Two details that are load-bearing:
  - **Durations are keyed by `agentId()`, not the agent's name.** A loop invokes the same name
    several times and a mapper fans one agent out over every item at once — names repeat, ids do
    not. The map is concurrent because a parallel step calls back from several threads.
  - **A workflow step is itself reported as an agent** (`Sequential`, `Parallel`,
    `ParallelMapper`, `Loop`), so its duration is the wall clock of that step. That is the number
    `everyStepIsTimedAndParallelStepsActuallyOverlap` asserts on — the library's own measurement
    rather than one taken around the call — and it is what makes "the branches really do overlap"
    a test rather than a claim.
  A node invoked more than once shows the latest time and a `×n` count, so a loop cannot look
  like it went round once.
- **`run` is deliberately not part of `web`.** A run is observable whether or not anything is
  watching over HTTP, which is exactly what lets the tests assert on the same `RunEvent`s the
  browser animates. `catalog` and every demo depend on `run`; nothing depends on `web`.
- **Tests mirror the shared packages**: `catalog/PatternCatalogTest` (the pattern runs, the
  demo-integrity claims, the topology claims), `support/ParsingTest`, `support/ErrorsTest`,
  `run/StreamingListenerTest`.
- **Every scope key is a `TypedKey`, never a string literal.** Each demo has a `Keys.java`
  holding the keys it introduces, and later demos import them the way they import agents —
  `demos/loop/Keys.Score`, `demos/single/Keys.Notes`. A key is the contract between two agents
  that never see each other, and nothing checks that the two spellings match: this repo lost a
  run to `"note"` against `"notes"`, and another to `findings` declared `String` when the scope
  held a `List`. Four things worth knowing before writing one:
  - **They are records, not interfaces.** The framework *instantiates* a key to ask its name
    (`AgentUtil.keyName` → `stateInstance` → `name()`), so it needs a public, concrete,
    no-args-constructible type. An interface fails with "doesn't have a no-args constructor".
  - **Each overrides `name()` to return the lowercase string it always used.** That is why the
    `@V("notes")` parameters and the `{{notes}}` placeholders in the prompts are untouched —
    the default name is the class's simple name, which would have capitalised every key.
  - **The input side is still bound by name.** `@V` takes a string, and
    `HumanInTheLoopBuilder.outputKey` has no `TypedKey` overload where `AgentBuilder` does — so
    those sites read `new Draft().name()`. The typing is only ever as good as the narrowest API
    you touch, which is worth saying out loud on stage.
  - **A typed read returns `null` when the key is absent** — it does *not* fall back to
    `defaultValue()`, which is a builder-level mechanism. Hence `requireNonNullElse(...)` at the
    display sites, and nothing at all where `Parsing.score`/`category` already treat null as "no
    answer".
  What it buys, concretely: the mapper's gathered verdicts read as a `List<String>` with no cast
  and no `instanceof`, because `Verdicts` is a `TypedKey<List<String>>`.
  `noDemoAddressesTheScopeWithAStringLiteral` reads the demo sources and fails on a relapse.
- **The demos build on each other, and that is the narration.** Each `PatternDef` carries a
  `story` (its beat: a weekend away, a picnic, the chocolate, a baby coming, the second dog) and a
  `buildsOn` naming what it inherits. Read in catalogue order the seventeen beats are one passage;
  read down the `buildsOn` lines they are one system being assembled. The tester shows both above
  the explanation, the gallery cards show the beat so the grid reads as the story, and `←`/`→`
  walk the catalogue in order.
  **Three spines carry the reuse:**
  - **The sitter note** — `single` introduces `SitterCardClerk`; `sequential` reuses it and adds
    `FridgeChecklist`; `loop` reuses *that* agent unchanged and draws a critic and a loop around
    it; `sitterNote` uses the same two a third time. Nothing about the agent changes between
    demos 2, 3 and 16 — only the control around it, which is the entire argument.
  - **The three desks** — `conditional` introduces `EverydayCare`/`DogTrainer`/`EmergencyVet`, and
    then four demos put a different control flow around the same cast: routing picks one,
    `humanApproval` adds a person before the answer is acted on, `supervisor` picks several and
    decides when to stop, `customPlanner` tries them cheapest-first. **`supervisor` contains no
    agent of its own** — that is the §6 pivot made concrete rather than asserted, and a test
    asserts it.
    The supervisor's claim is not "it calls more than one" — a fan-out does that. It is that
    **the second call exists because of what the first one said**, which neither routing nor a
    fan-out can produce. It took three attempts to make that land on a real model, and the
    failures are the useful part:
    - **Do not build a hand-off on an agent refusing.** The first version had the trainer decline
      cases that smelled of pain. It reads beautifully and it called one agent on a live model: a
      refusal is a *conditional exception* sitting under a positive instruction ("give the owner
      one thing to change this week"), and a model — a small local one especially — takes the
      positive instruction every time.
    - **So the first call is a `TriageNurse`, whose job IS to hand on.** She never treats and
      never trains; she assesses and ends by naming who is needed (`NEEDS: vet`). She always
      succeeds at what she was asked, so the supervisor's next decision rests on a fact it was
      given rather than a judgement the model had to volunteer. This is the one agent the demo
      adds; the three it calls are the routing demo's, unchanged.
    - **`supervisorContext(...)` must match the scenario it is written for.** An earlier version
      described a message holding *several separate problems* — left over from a previous
      scenario — and a live planner did exactly as told: one problem, one answer, stop.
    - **The result is one answer with its route, not a set of opinions.**
      `output(SupervisorPattern::answerWithItsRoute)` leads with the path
      (`TriageNurse → EmergencyVet`), then the **last** answer in full, then the earlier call in
      italics as the *reason* the next one happened. Printing every call as a peer block is what
      a parallel workflow produces, and it made this demo read as one. Only the final answer is
      output; an assessment is work.
    Same wiring, three routes, decided by what the nurse names: a sudden behaviour change reaches
    the vet, pulling and barking reach the trainer, and grass-eating settles with the nurse and
    stops there. `theSupervisorCallsASecondAgentBecauseOfWhatTheFirstSaid` asserts all three,
    plus that no protocol marker (`NEEDS:`, `ESCALATE`) leaks into the answer.
    **A warning about the mock**: it had the hand-off special-cased, so every test passed while
    the live demo called one agent and stopped. A deterministic stand-in proves the wiring, never
    that a real model will follow a prompt — check this demo against Ollama after touching any of
    these prompts.
  - **The second dog** — `voting` introduces the three assessors; `secondDogCouncil` has them
    ratify a debated motion instead of voting cold.
  `parallelMapper`, `goap`, `p2p`, `blackboard`, `debate` and `bdi` stand alone, honestly: they
  are about different subjects and forcing a link would damage them.
  **Two rules that must not be broken:**
  - **The narration is written to fit the rail order, never the other way round.** That order is
    the autonomy dial, which is the talk's thesis. `bdi` is a deliberate flashback ("think back to
    his very first hour") because the puppy's first hour is chronologically first and sits near
    the end of the dial.
  - **The chain runs through the DEFAULT INPUTS, not at run time.** `parallel`'s default input is
    literally what `single` prints. Nothing is passed between demos while they run, so a skipped
    section, a deep link from a slide, or one failed run never strands what follows.
  `theDemosReuseWhatTheEarlierOnesBuilt` asserts the reuse from the topologies, and
  `everyDemoHasItsBeatInTheNarration` fails the build on a demo with no beat. The first of those
  already caught the capstone quietly using its own `NoteTightener` while claiming to reuse demo
  3's checklist — the wiring was changed to match the claim, not the claim to match the wiring.
- **The demo problems obey two rules that pull against each other.** Both are load-bearing, and
  the catalogue has been rewritten twice for getting one of them wrong — read this before
  inventing a new scenario.
  - **1. The pattern must be load-bearing.** Take it away and the answer visibly degrades. The
    test is *"would one plain prompt do as well?"* Four failure modes to escape:
    - *Nothing to disagree about.* Voting once ran three copies of one prompt over an obviously
      positive sentence, so the tally was decoration. It now runs three *different criteria*
      (space and hours / money / what Zao would say) over a household where the money is fine
      and everything else is not — a real 2-1 split, in the mock as well as on a live model.
    - *Nothing to satisfy.* The loop's critic once scored a story out of 1.0, so the mock had to
      fake 0.60/0.95 to make it iterate and nobody watching could tell a good pass from a bad
      one. It now scores a **fraction of four named rules**.
    - *Patterns that are secretly a sequence.* GOAP, BDI and blackboard were all straight lines.
      GOAP now has a real precondition chain and is registered **backwards on purpose**; BDI has
      three desires whose priorities (not declaration order) pick the winner; blackboard's three
      contributors each read only `problem`, so any of them can go first.
    - *Output nobody can check.* "Write a vivid tale" makes a failed run look like a good one.
  - **2. The audience must not need the domain explained.** This is the rule the *second* rewrite
    was for. A version of this catalogue set in a professional boarding kennel satisfied rule 1
    perfectly and still failed on stage: bloat, 21-day rabies clearances, run sizes and discharge
    notes all have to be *taught* before the pattern can be discussed, and a sentence of setup
    per demo is fifteen sentences across the talk — during which the room is learning kennels,
    not patterns. So every constraint a demo turns on is now one the room already holds: grapes
    are dangerous and cheddar is not, hot pavement burns paws, a fridge note needs the vet's
    number on it, a puppy goes to the garden before he gets a training session, recall works in
    the garden before it works at the park, and neither half of a couple outranks the other about
    the bed. **The test for a new scenario: would a dev in row 20 know the right answer before
    you finished reading the input aloud?** If not, it is the wrong scenario however good the
    pattern fit is.
  `PatternCatalogTest.theDemoProblemsActuallyDemonstrateTheirPattern` asserts the rule-1 claims,
  so a prompt tweak that quietly turns a pattern back into decoration goes red. Extend it too.
- **`humanApproval` is the brake on the dial**, and the only pattern where the run stops and
  waits for a person. `HumanInTheLoop` (from `AgenticServices.humanInTheLoopBuilder()`) is a
  non-AI agent: it reads a key from the scope and writes one back, so the sequence around it
  cannot tell that the answer came from a browser. Three pieces make that work here, and they are
  the part worth understanding before touching it:
  - **`run/AskHuman`** — a one-method interface, deliberately blocking. It is an interface and
    not a method on the web layer for one reason: the demo has to run under `mvn test`, where
    the "human" is a lambda. Swapping the person for a stub is the only way to test a pattern
    that waits for one, and `AskHuman.NOBODY` is what every other run gets.
  - **`run/HumanQuestions`** — the meeting point between a blocked run and the POST carrying the
    answer. SSE is one-way, so the reply cannot travel down the pipe the question came from; every
    run announces a `runId` in its `run-start` event and the answer is posted to
    `POST /api/patterns/runs/{runId}/answer`. The wait has a **timeout** and ending a run
    **cancels** its question — runs execute on a pool of four, so a question nobody answers would
    otherwise hold a thread for ever and the next few runs would silently never start.
  - **`StreamingListener.askHuman`** carries it, because the listener already *is* the per-run
    context object and only one demo out of seventeen asks anybody anything. It emits `human-ask`
    **before** waiting — do it the other way round and the run blocks on a question nobody has
    been shown.
  The diagram gives the person the `human` role rather than `agent`, and
  `everyTopologyShowsWhatItsPatternActuallyDoes` asserts it: drawn like the boxes either side, the
  picture would say the model decided, which is the one thing this pattern denies.
  A trap this already paid for: `MockChatModel`'s reply lambda is handed the **raw** prompt, not
  the lowercased one its rule matched on, so `indexOf("what they said:")` returned -1, the slice
  landed somewhere arbitrary, and the refusal path quietly produced the approved answer. The test
  missed it too, because the result echoes the person's words and a naive `contains("nothing")`
  passed on those — assert on the instruction section, not the whole result.
- **`customPlanner` is the §7 "middle ground" made runnable**, and the only pattern whose
  behaviour lives in this repo rather than in the library. `demos/customplanner/` holds all of
  it — the three tier agents, the planner, and the wiring. `EscalationPlanner` implements
  `dev.langchain4j.agentic.planner.Planner` — which is a smaller interface than it looks:
  `nextAction(PlanningContext)` returns `call(...)` to invoke agents or `done()` / `done(result)`
  to stop, `init(InitPlanningContext)` hands you the sub-agents in declaration order, and
  `firstAction` defaults to `nextAction` (so it runs once before anything has been invoked and
  `previousAgentInvocation()` is null that first time). Everything else is a default method.
  The policy is a **cost ladder**: `PuppyBook` → `TrainerOnCall` → `VetOnCall`, stopping at the
  first rung whose answer ends `ANSWERED` rather than `ESCALATE`. Declaration order *is* the cost
  order; no prompt says "cheapest first".
  Why it has to be a custom planner, which is the only reason to write one — a sequence runs all
  three every time, a conditional picks a rung up front from the question alone, a loop re-runs
  the same agents, and a supervisor would hand an LLM your cost policy. The decision here depends
  on **what came back**, which is what `PlanningContext.previousAgentInvocation().output()` is
  for and what none of the built-in builders can express.
  Two things worth keeping if you touch it:
  - **The result names the rung that settled it** ("asked 1 of 3 rungs"), read from
    `AgenticScope.agentInvocations()`. Return just the answer and the one thing that distinguishes
    this from a sequence becomes invisible.
  - **`MockChatModel.kind()` reads only the question, never the tier's instructions.** Every
    tier's prompt explains what is past it ("anything about pain, injury or illness"), so a match
    against the whole prompt finds "injur" every time, classifies everything as medical, and the
    ladder walks to the top no matter what is asked — a planner that behaves exactly like a
    sequence. `PatternCatalogTest.theCustomPlannerStopsAtTheFirstRungThatCanAnswer` is what
    caught that, and it asserts the early exit on three different questions for that reason.
  On stage: the default input (a limp) walks all three rungs; type "which food should I buy?" and
  it stops at the book, or "he pulls like a train on the lead" and it stops at the trainer. That
  works offline too — the mock has three canned ladders.
- **`sitterNote` is the capstone, and the payoff of the talk's arc.** It is a system rather than a
  pattern: conditional routing sends the owner's worry to the right person, a parallel step plans
  the meals and the walks, a sequence merges all three into one note for the fridge door, and a
  loop tightens it until it passes the **same four rules** the standalone loop demo uses — a
  composite reuses the parts, it does not re-implement them. It exists to show that the builders
  *nest* — each composite is itself an `UntypedAgent` that another builder takes as a sub-agent —
  and to make the dial visible: deterministic scaffolding with LLM judgement at three points. Its
  diagram uses the `stages` layout, where each node carries an explicit column number, because no
  automatic layout recovers the real order of a composite's steps. When adding another composite,
  give it `category: "composite"` — the gallery counts patterns and composites separately.
  One seam it pays for out loud: it seeds the scope with the same text under **both** `worry` and
  `stay`, because the router and the three specialists ask "what is the worry" while the two
  planners ask "what is the stay". Reusing an agent means accepting the key it already declared;
  get it wrong and you get `MissingArgumentException` pointing at a step that looks unrelated.
- **`secondDogCouncil` is the second composite, mixing zoo patterns with plain plumbing**: a
  parallel mapper reads three angles of the household, one agent turns the findings into a motion,
  a **debate** argues it to a ruling, and the **same three assessors from the voting demo** ratify
  it — so the room has already met the voters and watches them ratify a debated motion instead of
  voting cold. Its lesson is the opposite of the first one's: the exotic planners are the easy
  part, and most of the work is the small adapter agents between them (`CouncilBriefer`,
  `CouncilNote`) because each pattern expects its input under its own key. Its result is composed
  from the scope (ruling, restated motion **and** ratification) rather than the debate's `verdict`
  alone — otherwise the last third of the diagram looks decorative because nothing it produced
  reaches the screen.
  Worth noticing: this debate's two advocates disagree, so `unanimous()` never converges and it
  runs its full two rounds, while the holiday debate's converge in one. Both behaviours on one
  page is deliberate.
  Three traps these composites already paid for, worth knowing before writing a third:
  - **Scope values are passed through, never coerced.** The mapper writes `findings` as a `List`;
    declaring `@V("findings") String` fails at runtime with a bare `argument type mismatch`.
  - **Parallel steps invoke the listener from several threads.** Anything collecting those events
    must be thread-safe — a plain `ArrayList` in a test silently drops them and reads as a flaky
    "that agent never ran". The SSE path is fine (Mutiny's emitter serialises), and is verified.
  - **A refinement loop feeds its own output back into the next prompt.** In the mock that means
    a rule matching a word which appears in the *note* hijacks the loop's second pass, and the
    composite returns the wrong stage's answer with no error at all. See the rule ordering note
    in `MockChatModel`.
- **`demos/<id>/*`** — one public interface per agent (`@Agent` + `@UserMessage`/`@V`), so
  LangChain4j can build JDK proxies. Prompts are worded so `MockChatModel` returns parseable output.
- **`ModelFactory`** — resolves the shared `ChatModel` (Ollama or mock). Eager (observes `StartupEvent`)
  so the endpoint discovery and probe run at boot; `activeModel()` reports what is actually live, and
  `currentModel()` re-probes when the last attempt fell back. `PatternResource` calls `currentModel()`
  per run rather than injecting a `ChatModel` once — that is what makes recovery-without-restart work.
- **`MockChatModel`** — deterministic, no-network `ChatModel`, and the thing `mvn test` runs
  against. It pattern-matches **the last user message** (never the accumulated conversation —
  that would pin multi-turn planners to their first choice) against an **ordered rule table**,
  and returns canned, PARSEABLE answers. The table is ordered on purpose and each rule's comment
  says what it stands in front of, because prompts overlap heavily: three agents talk about the
  sitter note, the park step quotes the garden step, and the *specific* rule has to come first.
  Three hazards it already handles, each of which produced a wrong demo with no error:
  - **Whitespace is collapsed before matching.** The prompts are text blocks, so "PASS or FAIL"
    is one phrase to a reader and `"PASS or\nFAIL"` to `String.contains` — a rule that looks
    obviously right silently never fires.
  - **Rules that match on quoted content go below rules that match on an instruction.** A
    refinement loop feeds the note it just wrote back in; a rule keyed on a word inside that
    note hijacks the loop's second pass.
  - **Trigger words must not be ordinary English.** The score rule used to fire on the word
    "number", which quietly claimed every agent whose rules mention "the vet's telephone
    number" — so they answered `0.60` instead of writing a note. It now keys on "0.0 to 1.0".
  The demo-critical values: a score alternating 0.60/0.95 so loops visibly iterate then exit;
  **different** one-word votes per assessor (`YES` for money, `LATER` for the other two) so the
  offline vote is a genuine 2-1 majority; identical replies for the holiday advocates so
  `ConvergenceStrategy.unanimous()` fires, and differing ones for the council's so it does not;
  a 3-step supervisor plan routine→training→done; and an item-aware food table so the mapper
  really does clear the cheddar and condemn the grapes. Its worry-routing rule must stay in step
  with `Parsing.CATEGORIES`, and its canned supervisor plan names `RoutinePlanner`/
  `TrainingPlanner` literally — renaming those two agents breaks the supervisor demo.
- **`Errors`** — flattens a throwable's cause chain for display. LangChain4j reports every agent failure
  as `AgentInvocationException: Failed to invoke agent method`, so surfacing only `getMessage()` makes a
  dead Ollama and a parse failure look identical.
- **`PatternResource`** — REST/SSE endpoints. `GET /api/patterns` (metadata), and
  `GET /api/patterns/{id}/run?input=...` streams `RunEvent`s over Server-Sent Events. Runs execute on a
  small fixed thread pool.
- **`StreamingListener`** — implements LangChain4j's `AgentListener`; `inheritedBySubagents()` returns
  true so every sub-agent invocation in a composite is observed. Bridges before/after/error callbacks
  (plus a scope-state snapshot) into `RunEvent`s pushed to the SSE sink.
- **`LogStream` / `LogResource`** — mirrors the server log into the browser's "Server log" tab.
  `LogStream` attaches a JUL handler to the root logger (Quarkus logs via JBoss LogManager, a JUL
  implementation), keeps a 400-line ring buffer and broadcasts to `GET /api/logs` over SSE. It observes
  `StartupEvent` at `@Priority(1)` so it is listening before `ModelFactory` logs which model is live.
  Two hazards it already handles: emitting a record can itself log (guarded by a thread-local, else
  infinite recursion), and dev-mode reload would otherwise stack a second handler and double every line.
- **`Topology` / `RunEvent`** — plain records describing the graph and the streamed events.
  `RunEvent.ScopeValue` (type + size + rendered value) is what makes the Scope tab a variables
  table rather than a wall of strings. `StreamingListener.describe` names types the way a reader
  expects — `List(3)`, not `ImmutableCollections$ListN` — and skips `__`-prefixed planner
  bookkeeping. Worth noticing on stage: `score` shows as `String`, which is exactly why
  `demos.loop.FridgeRuleCheck` returns one.
- **`src/main/resources/META-INF/resources/`** — the frontend, four files, no build step:
  `index.html` (90 lines of markup), `app.css`, `render.js` (pure rendering: HTML escaping, the
  markdown subset, topology layout/drawing — functions of their arguments, which is why the same
  `layout()` serves both the live diagram and the gallery thumbnails) and `app.js` (routing, the
  catalogue, SSE runs, the dock, layout chrome). Classic deferred scripts in that order, not ES
  modules — they share globals and load in sequence. The **theme bootstrap stays inline in
  `<head>`**: it has to set `data-theme` before first paint or dark users get a white flash, and an
  external file cannot guarantee that. Two hash routes share the main column: `#/` is the **gallery** and `#/<id>` is the
  **tester**. The gallery is **one section per category, not a flat grid of tagged cards**: the
  categories are separated physically, under a heading that carries the group's name, a one-line
  gloss in the talk's own words (`CAT_NOTES` — "you decide the path" / "the model decides the
  path") and a count. Same categories in the same order as the rail, so the two views never teach
  different arrangements. A category chip on every card was the earlier design and it was worse
  twice over: it made the reader sort what the layout can sort for them, and it competed with the
  pattern's own name for the top-left of the card. The colour each chip carried survives as a
  small dot on the group heading. A card holds the pattern's name, its `useful` line, and a
  label-free thumbnail of its topology drawn by the same `layout()` the real diagram uses, so a
  fan-out is recognisable from a chain at a glance. **Three cards a row at most** — the track
  minimum is `max(255px, (100% - 28px)/3)`, so a wide screen lands on exactly three and a narrow
  one still falls back to two and then one, with no width in between that yields four. Unbounded
  `auto-fill` put five or six across a large monitor, which read as a list and shrank the
  thumbnails past recognising. It stays `auto-fill` rather than `auto-fit` so the one- and
  two-pattern sections keep cards the same size as every other section. `buildGallery` orders the sections by
  `CAT_LABELS` and then appends any category not named there, so a new `category` value shows up
  in the gallery even before someone gives it a label. Cards are real `<a href="#/id">` anchors, so Back, keyboard and open-in-new-tab work
  without JS, and a pattern can be deep-linked straight from a slide. An unknown id falls back to the
  gallery rather than rendering a blank page. The tester's layout is
  title → run controls → full-width SVG diagram → bottom dock. The dock has four tabs: **Result**
  (rendered markdown), **Scope state** (a debugger-style variables table: name / type / value, with
  the rows an agent just wrote highlighted, and long values clamped until clicked — expansion
  survives the next update so a row doesn't collapse mid-run), **Run events**
  (`/api/patterns/{id}/run`) and
  **Server log** (`/api/logs`, with a level filter). A dot flags a WARN/ERROR — or a finished result —
  on a tab you haven't looked at. Finishing a run switches to Result automatically, *unless* the viewer
  picked a tab themselves during that run (`tabPinned`) — never yank the view out from under someone.
- **`[hidden]{display:none !important}` is declared once in `app.css`, and it has to be.** The
  `hidden` attribute is only `[hidden]{display:none}` in the browser's own stylesheet, so any
  author rule that sets `display` on the same element silently beats it. The runtime badge
  (`.ran{display:inline-flex}`) sat in the controls as an empty pill before the first run for
  exactly that reason. Eight elements on this page are toggled with `el.hidden` — the badge, the
  builds-on line, the human-in-the-loop panel, the two unread dots and the three dock panes — so
  this is a rule about the page, not about one bug.
- **The tester leads with the story; the teaching text is folded away.** `useful` and `caveat`
  live in a native `<details class="notes">`, **closed by default** — on stage the story beat is
  what gets said out loud, and the explanation is what you open when somebody asks. Native
  `<details>` rather than a JS toggle, so it needs no script and the keyboard works. The
  open/closed state persists with the other layout prefs (`notesOpen`), because someone who opens
  it once is usually comparing patterns and should not have to re-open it on every navigation.
  Both fields are run through `renderMarkdown` rather than set as text: the catalogue prose
  carries `**bold**` and `` `code` `` that used to show as literal asterisks and backticks.
  That is safe — `renderMarkdown` escapes before it introduces any tag — and it is the reason the
  order of those two steps in `render.js` must not be swapped.
  `renderMarkdown` is ~40 lines with no dependency (a CDN is the one thing sure to fail on conference
  wifi). It escapes the text **before** introducing any tag, so model output can never inject markup;
  keep that order if you extend it. Known simplification: nested bullets flatten to one level.
  **Theming rule: the dog is in the craft, not in the jokes.** This is shown on a Devoxx stage, so
  the canine character lives in the palette (a Belgian shepherd's fawn/rust coat on warm paper), a
  drawn paw mark shared by the header and favicon, a near-subliminal paw texture on the empty
  canvas, and the pulse on a working agent. It must NOT live in emoji decoration, pun button labels
  ("Fetch"/"Heel") or twee empty states — those read as kitsch on a projector and undercut the
  talk. Labels stay plain; the agent names already carry the theme. Two glyphs remain, both
  functional rather than decorative: ☰ for the rail toggle and ⚠ on the caveat.
  Visually it is a light, card-based shell — floating rounded surfaces with soft elevation on a
  tinted page — rather than the bordered-box admin look it started as. The primary action is ink,
  not brand colour; the accent is reserved for identity and selection (a tinted chip, not a
  saturated slab). Theme is an explicit `data-theme` on `<html>`, resolved by a small script in
  `<head>` **before first paint** (else dark users get a white flash), so the tokens are declared
  once each instead of duplicated across a media query. A header toggle overrides the OS and
  persists with the other layout prefs — on a projector the OS preference is rarely the right one.
  Every colour pair is checked against WCAG 4.5:1 in both themes — the event and log panes are dark
  in *both*, so their text colours are deliberately theme-independent (theme-following inks
  measured 2.8:1 there).
  The rail collapses (header ☰) and the dock is drag-resizable by its grip (arrow keys too,
  double-click to reset); both sizes persist in `localStorage` under `dashboard.layout`, so a reload
  or a dev-mode restart mid-talk doesn't undo how the room's view was set up. Every storage access is
  wrapped — a private-mode browser where `localStorage` throws must still boot the page.
- **Edge labels are placed, not just positioned** (`placeEdgeLabels` / `fitEdgeLabels` in
  `render.js`). They are the only thing that says *what* travels along an arrow — `score < 0.8` is
  the loop's exit condition — and they were frequently unreadable for four reasons, none of which
  was the font size. Worth knowing before touching that code, because each fix has an invariant:
  - They were appended **before** the nodes, so any label landing on a node box was painted over
    by it — total, not partial, because node fills are opaque. They now go on last. Keep it that
    way: it is why the label wins when a diagram is genuinely too tight to avoid a box.
  - Their anchor was the midpoint between node **centres**, which in a fan-out, a star, or any
    edge spanning two columns is regularly inside a third node. Each label now slides along its
    own curve (`at(t)`, which follows the real line/bow/arc it was drawn as) and perpendicular to
    it, taking the first position clear of every node box and every label already placed. In a
    chain the boxes are 150 wide and ~15 apart, so **no multi-word label can ever fit between
    them** — the offsets deliberately reach far enough to sit above or below the row instead.
  - Reserved boxes carry a margin (8×5), because two labels that merely fail to overlap still
    read as one run of text: "needs been out" and "needs fed" landed 0.4px apart on the BDI
    diagram and a plain collision check was perfectly happy with it.
  - The diagram is a viewBox scaled to fit its pane, so a wide composite in a short dock is drawn
    at under half size. `fitEdgeLabels` grows the labels in user units to hold them at a constant
    size **on screen**, re-fired by a `ResizeObserver` in `app.js` (re-fit, never redraw — a
    redraw would throw away which nodes are mid-run). **`EDGE_FS_MAX` is load-bearing:**
    `placeEdgeLabels` reserves space at that size, so raising the cap without re-checking
    placement silently brings the overlaps back.
  Legibility, not just geometry: labels are haloed in the canvas colour via `paint-order` so the
  glyphs sit on top of their own arrow, and inked at ~8:1 (`--edge-ink`) rather than `--muted`.
  Edges and arrowheads use `--edge-line`, a shade darker than `--node-line`, which sits at ~1.3:1
  on the canvas — fine for the outline of a filled box, invisible for a hairline arrow at the
  back of a room, and the arrows *are* the topology.
- **The diagrams deliberately do not draw the AgenticScope.** It was the identical terminal box in all
  13 topologies, saying nothing about the pattern, and the scope now has its own tab. The one exception
  is Blackboard, where the shared board *is* the pattern — remove it there and you get four
  disconnected agents. The graph layout treats a `board` node as optional and re-centres without it.
- **A topology must show what the pattern actually does, not just who is involved.** The test for a
  diagram is whether someone who can't hear the speaker would infer the mechanism. Concretely:
  - fan-out patterns need their **join** (`role: "join"` — parallel's `combine`, voting's
    `majority()`, the mapper's `gather`). Without it the picture splits work and never merges it,
    which is half the pattern missing.
  - Conditional routing gets its own `branch` layout — three columns, input → router → alternatives.
    A router sharing a column with its branches doesn't read as routing. It has **no** join: only one
    branch runs, so merging them would be a lie.
  - Mutual relationships (debate rebuttals, supervisor invoke/result, blackboard read/write) are
    drawn as two bowed curves. Straight lines for `A→B` and `B→A` land exactly on top of each other,
    so the picture silently loses one direction. Chain-like layouts are exempt: they already arc the
    return edge overhead, which is how the loop's exit condition reads.
  `PatternCatalogTest.everyTopologyShowsWhatItsPatternActuallyDoes` asserts these claims (plus: no
  edge to a missing node, no node left unconnected). Extend it when you add a pattern.

### The data flow for one run

`index.html` opens an EventSource → `PatternResource.run` looks up the `PatternDef`, builds a
`StreamingListener`, and calls `def.run(model, input, listener)` → the `Runner` wires agents via
`AgenticServices.*Builder()` with the listener attached and invokes → agent callbacks + scope snapshots
stream back as `RunEvent`s → the page animates the topology and updates the scope panel.

## Adding a pattern (the common task)

1. Make a package `demos/<id>/`, named after the pattern id in lowercase.
2. Put one file per agent in it (one `@Agent` interface each), an `XxxPattern` with a
   `public static PatternDef define()`, and a `package-info.java` saying what the demo shows.
   Then add one line to `PatternCatalog.build()`.
3. If running under the mock, add a rule to `MockChatModel`'s table — and mind where you put it:
   the table is ordered, and a rule keyed on a word that appears in quoted content will hijack
   another agent's prompt.
4. Extend `PatternCatalogTest.theDemoProblemsActuallyDemonstrateTheirPattern` with the claim the
   new pattern makes, and `everyTopologyShowsWhatItsPatternActuallyDoes` with what its diagram
   must show.
The frontend needs no change — it renders whatever `/api/patterns` returns.

## Foreign agent config detected

An OpenAI Codex config exists at `~/.codex` (AGENTS.md + skills). Reply `/import` to scan and list what's
importable, then `/import --yes` to apply user-level items.
