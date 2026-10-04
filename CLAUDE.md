# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

Workspace for a Devoxx Belgium 3-hour deep-dive talk, "Agentic Systems in Java with LangChain4j."
Two distinct halves:

- **Root** (`README.md`) — talk planning: the through-line is "autonomy is a dial," told as "From Puppy
  to Pack." The `NN-*.md` planning docs referenced in the root README are the speaker's notes.
- **`pattern-dashboard/`** — the live demo: a Quarkus web app that visualizes and **runs** the
  LangChain4j agentic patterns as missions of the **Pawer Rangers** — a team of dogs led by **Zao**,
  a Bouvier des Flandres, in the town of Barkville. This is the code you will actually build and edit.
- **`Pawer Rangers – Agentic Patterns Demo Spec.md`** — THE spec for the demos: the cast, the Pup
  Board pins, and one mission per pattern (16), in four acts. The code follows it mission by
  mission; when they disagree, the spec is what the speaker is presenting from. Missions 17–21 (two
  Mega Mutts and three production demos) are not in the spec and are built in its world.

## Commands (run inside `pattern-dashboard/`)

```bash
mvn quarkus:dev                       # dev mode + live reload; needs Maven 3.9+. Open http://localhost:8080
mvn quarkus:dev -Ddashboard.model=mock  # no Ollama / no API key — deterministic offline run
mvn -DskipTests package               # build fast-jar to target/quarkus-app/
mvn test                              # smoke-runs all 19 patterns + both composites on the mock model
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
demos/_NN_<id>/  EVERYTHING for one mission, and nothing else:
                   its agent contracts, one interface per file (and its gear, if any)
                   its Keys — the Pup Board pins it introduces
                   its XxxPattern — topology + Runner
                   package-info.java — what this mission is for
                 NN is the spec's mission number, so the tree reads in mission order
  Before the pack              _00_aiservice/   (a plain AI service — not agentic)
  Act 1 · Meet the team        _01_single/ _08_nonaiagent/
  Act 2 · Workflows            _02_sequential/ _03_loop/ _04_parallel/ _05_parallelmapper/
                               _06_conditional/ _07_humanapproval/
  Act 3 · Planners             _09_supervisor/ _10_goap/ _11_p2p/ _12_blackboard/
  Act 4 · Many minds           _13_voting/ _14_debate/ _15_bdi/ _16_customplanner/
  The Mega Mutt (composites)   _17_megamutt/ _18_lakeparty/
  Running it for real          _19_modelrouting/ _20_async/ _21_resilience/
catalog/         PatternCatalog (the registry) · PatternDef · Topology
support/         Parsing · Errors — the shared pieces that are OURS, not LangChain4j's
model/           ModelFactory (which ChatModel is live) · MockChatModel (the offline one)
                 · MockStreamingChatModel · ChatCallLog
run/             RunEvent · StreamingListener · ModelTiers — observing a run
web/             PatternResource · LogResource · LogStream — REST and SSE
```

- **Every `XxxPattern` is two methods, in this order: `run` then `define`.** `run(model, input,
  listener)` is the wiring and nothing else — the agents, the builder, the invocation. It is what
  gets opened on stage, so it comes first in the file and carries no topology or catalogue text.
  `define()` holds the `Topology.Graph` and the `PatternDef`: how the page draws this demo and
  what the gallery says about it, which is the dashboard talking to itself. `define()` hands the
  wiring over as a method reference (`LoopPattern::run`), so the two never tangle. Anything else
  a demo needs — result formatting, a constant — goes **below** `run`, not above it.
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
    `FirstSniff`. Nine tests go red at once if you drop it, which is how this was established.
    Visible in the wild at `p2p`: `plannerBuilder()` takes no `.name(...)`, so the wrapper itself
    reports as `invoke` — which is why that demo's test filters the roll-call to its two peers.
  - **`support/Parsing` takes plain strings, not an `AgenticScope`.** Reading the scope is
    LangChain4j API and belongs in the demo; parsing a model's prose into a number is ours. So a
    loop's predicate reads `scope -> Parsing.score(scope.readState("score", "")) >= 0.8`, with
    the scope read visible where the room is looking.
    **`Parsing.score` reads a FRACTION first, and must keep doing so.** The critic is asked for
    "the fraction of rules that hold" over four named rules, so it answers `3/4`, `3 of 4`, or
    `4 of 4 rules hold: 1.0`. Taking the *first* number — which this did — reads that last one as
    **0.4**: the exit condition never fires, the loop runs to `maxIterations` on a note that was
    already perfect, and there is no error and no red test, just a demo that looks like a critic
    nobody can satisfy. Failing over: fraction, then the last number already in 0.0–1.0 (a model
    states its conclusion last, and anything before it is usually a rule index), then the last
    number rescaled. Emphasis is stripped first because `**0.85** out of 1.0` puts the asterisks
    exactly between the two halves of the fraction.
- **A demo package is `_NN_<id>`: its place in the running order, then the pattern id**,
  lowercased. So the packages sort into the talk's order in the IDE tree, and the deep link on a
  slide (`#/loop`) still names the package to open on stage (`demos._03_loop`), with
  `#/megaMutt` at `demos._17_megamutt`. `NN` is also the spec's mission number. Two things about
  that shape:
  - **The leading `_` is not decoration — a package segment cannot start with a digit.** `01_single`
    is a compile error ("illegal underscore"); `_` is one of the three characters Java allows a
    segment to begin with, so it is the price of having the number first.
  - **`NN` is the index into `PatternCatalog.build()`, and nothing reads it at run time.** The id
    in `PatternDef` is still `loop`, the route is still `#/loop`, and no code derives a package
    from an id — so a number that has drifted out of step with the catalogue is invisible to the
    build and wrong only to a reader. **Reordering the rail now means renaming packages**, on top
    of the `buildsOn` renumbering that moving `nonAiAgent` already cost once. That is the standing
    price of this scheme; pay it deliberately or not at all.
- **An agent lives in the mission that introduces it**, and later missions import it from there —
  see "The missions build on each other" below for the map.
- **`PatternCatalog` is the registry and nothing else**: twenty-one `XxxPattern.define()` calls in
  mission-number order, commented with the act each belongs to. Adding a demo is a
  new package plus one line here.
- **An agent does not have to be a model, and `nonAiAgent` is the general case.**
  `AgentUtil.agentToExecutor` falls through to `nonAiAgentToExecutor` for anything that is not
  already an agent, so **any plain object with one `@Agent` method goes straight into
  `subAgents(...)`** — `@K` parameters bound from the scope, return value written to the output
  key, the sequence unable to tell. `HumanInTheLoop` (demo 7) is the library's own instance of
  this; `demos/_08_nonaiagent/Rivet` is your own class, in front of an LLM step. **It is Mission
  8, and the spec puts it in Act 1** beside Mission 1 — "not every dog needs a brain" — which the
  `team` category does without renumbering. Three things it pinned down:
  - **A non-AI agent is INVISIBLE to the listener in `1.20.0-beta30`.**
    `NonAiAgentInstance.setParent` sets the parent and never calls
    `registerInheritedParentListener` — which `AgentInvocationHandler:253` and
    `PlannerBasedInvocationHandler:343` both do. The field, the method and
    `composeWithInherited` are all there; the one call is missing. So a plain-Java step emits no
    `agent-before`/`agent-after`, is never timed, and **its node never lights on the diagram**.
    The demo makes that the lesson rather than hiding it, and
    `rivetDoesTheMathsWithNoBrainAndNoEvents` pins the current
    behaviour: **if that assertion goes red on a version bump the library fixed it — delete the
    assertion and rewrite the demo's caveat, which will have become wrong.** (This is also why
    `humanApproval` works: `StreamingListener.askHuman` emits `human-ask`/`human-answer` by hand,
    so that demo never depended on the inheritance that is missing here.)
  - **`name` goes on the annotation, not a builder.** There is no builder for a POJO, and the
    default is the *method* name — `Rivet` would be called `ladderLength` everywhere. Same trap
    as `.name("X")` one layer down. `agentAction(scope -> …)` has no answer at all: it comes out
    named `run`, which is why anything you want on a diagram is better as a class.
  - **`typedOutputKey = Keys.Facts.class`** is the annotation's `outputKey(Facts.class)`, so a
    non-AI agent obeys the no-string-literals rule like everything else.
- **`role: "code"` exists for the same reason `role: "human"` does.** The framework genuinely
  cannot tell a plain-Java step from an LLM one — that *is* the lesson — but the picture has to,
  or a diagram of a pipeline with a database lookup in it claims the model did the lookup. Drawn
  as a tinted, square-ish box with a monospaced name; `render.js` and `app.css` both key off it,
  and `everyTopologyShowsWhatItsPatternActuallyDoes` asserts the two Java steps are not agents.
- **The fifth category, `production`, is NOT a position on the dial** — and that is the whole
  reason it exists as a separate group rather than three more entries in the four above it. Every
  other demo is a *shape* (a chain, a fan-out, a loop, a star); `modelRouting`, `async` and
  `resilience` are **modifiers on an agent or a builder**, each one call:
  `chatModel(Function<AgenticScope, ChatModel>)`, `async(true)`, `optional(true)`,
  `errorHandler(...)`. They can be bolted onto any demo above, which is exactly why putting them
  on the dial would be a lie — the rail order is the autonomy argument, and these are orthogonal
  to it. The gallery gloss says so out loud ("Not where on the dial — what it takes to run it").
  `buildGallery` already appends unknown categories, so a sixth group costs two lines in
  `CAT_LABELS`/`CAT_NOTES` and nothing else.
  Four things these three demos pinned down, each of which is easy to get backwards:
  - **`optional(true)` is about a missing INPUT, not a failing agent.** In `AgentExecutor` the
    `optional()` check sits inside `catch (MissingArgumentException e)` — a step that *throws* is
    not optional's problem however optional it is. So `resilience` skips Doc (`DocFirstAid`) because
    most rescues end with nobody hurt and nothing writes `Injuries`, and the seeding of that key
    is **plain Java in `run`**: deciding whether you hold a value is not a job for a model.
    Delete "a thorn in its paw" from the input on stage and the step vanishes with no error.
  - **`errorHandler(...)` lives on `AgenticService`, so it is set on the *workflow* builder, not
    on `AgentBuilder`** — it rides on the scope (`DefaultAgenticScope.withErrorHandler`) and sees
    every `AgentInvocationException` in the run. **`RETRY` re-executes and a second failure comes
    straight back to your handler**, so a handler without a counter is an infinite loop.
    `ResiliencePattern.MAX_RETRIES` is that counter and it is not decoration.
  - **An async agent's join is the line that READS the key.** `async(true)` writes an
    `AsyncResponse` into the scope and `readState` blocks on it, so the waiting moves to the
    reader and the Scope tab shows `<pending>` until then. Measured, not asserted: against a
    200 ms model the `async` demo's `Sequential` step is ~453 ms against ~682 ms of agent time.
    Its diagram is `stages` with a deliberate **three-column skip-ahead edge** from the async
    step to the join — drawn flat it would disappear behind the boxes between its ends, and that
    edge is the agent's lifetime.
  - **Two model tiers must not be faked.** `ModelTiers.distinct()` is false when both tiers are
    really the same model, and `modelRouting` prints that instead of implying a saving that never
    happened. Set `dashboard.ollama.cheap-model-name` (and pull it) for a real two-model run.
    The tiers reach the demo through `StreamingListener`, following the `AskHuman` precedent —
    the listener already *is* the per-run context object, and widening `Runner` for one demo out
    of twenty-one would cost the other twenty a parameter they never read.
- **A demo that is ABOUT failing gets one narrow exemption in the smoke test, and only one.**
  `everyPatternCompletesUnderTheMockModel` treats any `agent-error` as a broken demo, which is
  right for twenty of them; `resilience` is exempted *by pattern id and by agent name* so that
  anything else erroring there is still a bug, and its result assertion still has to hold.
  Widening that filter is how this test stops being worth running.
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
  **Every demo owes a claim of its own, not just a smoke run.** `everyPatternCompletesUnderTheMockModel`
  only says "it did not throw", which is true of a pattern that has quietly turned back into a
  sequence. Three demos went a long time with nothing else — and they were the wrong three, since
  `blackboard` is one of the patterns this file records as having *been* a straight line once:
  the fix was made and never pinned. They now have `theCouncilArguesThreeRoundsBeforeFifiRules`,
  `oneScentWakesTwoPeersAndTheSearchEndsOnTheGoat` and
  `eachClueUnlocksTheNextAndTheBoardDecidesTheOrder`. The last two assert from the
  **interfaces** first, then from a run, and that is the general lesson: a run shows one order,
  and one order is exactly what a sequence shows too — so "who can act when" has to be read off
  the declared `@K` keys, which is what actually makes it true.
- **There is an untyped version of demos 1–6 in the history, for the live demo.** Commit
  `31c3967` ("Example without typedkey: demo 1-6") is the whole catalogue with demos 1–6 using
  `@V("Notes")` / `.outputKey("Notes")` instead of `@K` and `TypedKey`, so §5 can argue for typed
  keys against a *before* the room has just been looking at. `git show 31c3967` is the diff to
  replay; the tree is typed again as of the commit after it. Three things learned doing it, which
  matter if it is redone live:
  - **The string must be the capitalised record name.** `TypedKey.name()` defaults to the
    record's simple name, so `@V("Worry")` and `@K(Worry.class)` are the same key at run time —
    which is what lets demos 1–6 go untyped while demos 7, 9, 16 and 19 keep reading what demo
    6's desks wrote. Spell it `@V("worry")` — the natural thing to type when hand-writing "the
    bad version" — and **nothing fails where you typed it**: the agent receives nothing and a
    *later* demo breaks. That is worth doing on purpose on stage; it is the argument.
  - **The prompts need no editing either way**, because `{{Notes}}` is already the record's name.
  - **`parallelMapper` shows the cost in one line.** Typed, it is `readState(Verdicts.class)`.
    Untyped it becomes `readState("Verdicts", List.<String>of())`, where the `List<String>` is
    asserted by the *default value* rather than by the key — and putting `""` there compiles and
    fails at the cast. That error happened for real while writing the untyped version.
  `noDemoAddressesTheScopeWithAStringLiteral` enforces the typed rule across all demos, so it
  goes red the moment demos 1–6 are flipped — which is correct, and is the reminder to flip them
  back before committing.
- **Every scope key is a `TypedKey`, never a string literal.** Each demo has a `Keys.java`
  holding the keys it introduces, and later demos import them the way they import agents —
  `demos/_03_loop/Keys.Score`, `demos/_01_single/Keys.Notes`. A key is the contract between two agents
  that never see each other, and nothing checks that the two spellings match: this repo lost a
  run to `"note"` against `"notes"`, and another to `findings` declared `String` when the scope
  held a `List`. Four things worth knowing before writing one:
  - **They are records, not interfaces.** The framework *instantiates* a key to ask its name
    (`AgentUtil.keyName` → `stateInstance` → `name()`), so it needs a public, concrete,
    no-args-constructible type. An interface fails with "doesn't have a no-args constructor".
  - **A key declares its type and nothing else** — no `name()` override, no body:
    `public record Notes() implements TypedKey<String> {}`. `TypedKey.name()` already defaults to
    the record's simple name, so the key *is* `"Notes"` and the `{{Notes}}` placeholders in the
    prompts are spelled the same way. The keys were briefly written with an override returning a
    lowercase spelling, to keep the prompts untouched; that is one more line per key, and one
    more thing that can disagree with the record's name, to buy a lowercase `n`.
  - **The input side is typed too: `@K(Notes.class)`, not `@V("Notes")`.** `@K` (in
    `agentic.declarative`, alongside `TypedKey`) takes the key *class* and resolves the prompt
    variable through `TypedKey.name()`, so `{{Notes}}` is unchanged and neither end of the
    contract is a string any more. Two things follow. It is wired by **ServiceLoader** —
    `AgenticParameterNameResolver`, declared in the agentic jar's `META-INF/services` — which is
    also what teaches `AgentInvoker.parameterName` about `@K`; a build that loses that file
    (shading, native, the module path) fails loudly with "Parameter name not specified and no @V
    or @K annotation present", not silently. And `@K` alone honours a key's `defaultValue()` on
    the input side, via `AgentUtil.parameterDefaultValue` — which matters for `resilience`: every
    key here defaults to null, so `Meds` still raises `MissingArgumentException` and
    `optional(true)` still skips the step. **Give a key a non-null `defaultValue()` and that demo
    stops working**, because the step would then always have an argument.
    **The one name that is NOT a key** is the parallel mapper's item: `MapperAgentInvoker` binds
    it to the sub-agent's *first argument* positionally, so `@V("food")` and `@V("angle")` name
    nothing in the scope, correctly have no `Keys` entry, and are the only `@V` left in the
    demos. Beyond them, three sites spell a key out because their API has no typed form:
    `HumanInTheLoopBuilder.inputKey`/`outputKey` (hence `new Draft().name()`),
    `MockChatModel.WORRY_ARG` (a planner's JSON names the agent's *parameter*, and the offline
    model must not depend on `demos`), and `PatternCatalogTest.declaredKey`, which reads the
    annotation the way the framework does. The typing is only ever as good as the narrowest API
    you touch, which is worth saying out loud on stage.
  - **A typed *read* returns `null` when the key is absent** — `readState(Notes.class)` does not
    fall back to `defaultValue()`. Hence `requireNonNullElse(...)` at the display sites, and
    nothing at all where `Parsing.score`/`category` already treat null as "no answer".
  What it buys, concretely: the mapper's gathered verdicts read as a `List<String>` with no cast
  and no `instanceof`, because `Verdicts` is a `TypedKey<List<String>>`.
  `noDemoAddressesTheScopeWithAStringLiteral` reads the demo sources and fails on a relapse.
- **Mission 0 (`aiService`) is a plain `AiServices` interface — deliberately NOT agentic.** It is
  the baseline the room needs before an `@Agent` means anything: one model call, with
  `.tools(new DutyRoster())`, `.inputGuardrails(new NoCatsAllowed())` and
  `.outputGuardrails(new PawSized())` on the builder. Category `classic` ("Before the pack"), at
  catalogue index 0 — which is why the package-numbering test counts from ZERO. Five things it
  pinned down:
  - **An AI service is invisible to the agentic listener.** It reports through a different
    API — `AiServices.registerListeners(...)` with one listener per event type (started, tool
    executed, input/output guardrail executed, completed, error). `run/AiServiceBridge` maps those
    onto the run's `StreamingListener` (via its public `emitEvent`), so the page animates Mission 0
    like any agent; guardrails report as type `guardrail` under their class name, which is their
    box's label. `ToolExecutedEvent` only fires AFTER the tool ran, so call and result are emitted
    together.
  - **There is no scope.** No Pup Board, no Scope tab rows — the test asserts it. That absence is
    the bridge to Mission 1: the scope is what the agentic module adds.
  - **A reprompt is sent WITHOUT the conversation** unless the service has chat memory. The first
    version re-prompted with "answer the letter again, shorter", and a live model replied
    "please provide the letter you would like me to answer". `PawSized` now puts its own draft in
    the reprompt text, so the instruction is self-contained.
  - **Output retries are capped, and the cap throws.** Two reprompts by default; past that the
    call throws `OutputGuardrailException`. The wiring sets it out loud
    (`outputGuardrailsConfig(...maxRetries(3))`) and catches it. An input guardrail that fails
    throws `InputGuardrailException` too — and the model is never called (no tool events).
  - **The default letter asks for "everything"** so a live model writes 110–220 words and
    `PawSized` fires on the first click; a polite short letter passes first time and shows nothing.
    For the input guardrail, sign a letter "Marmalade".
- **The missions build on each other: the cast is met once and reused.** Each `PatternDef`
  carries a `story` (the mission in one sentence, as the speaker says it) and a `buildsOn` naming
  what it inherits; the gallery cards show the story so the grid reads as Barkville's week. An
  agent lives in the mission that introduces it, and later missions import it from there — so a
  mission's import list says what it is made of before a word of explanation.
  `laterMissionsReuseTheRangersTheyAlreadyMet` asserts these imports:
  - **Sniff finds** (`_01_single.SniffFinds`, with `SniffGear`) — the hat (1), the kitten (2),
    the Mega Mutt (17), the bad radio day (21).
  - **The four Rangers on call** (`_06_conditional.SniffOnCall/DigOnCall/DocOnCall/ZoomOnCall`,
    all reading `Call`) — routing picks one (6), Zao supervises them (9), the nap schedule
    rations them (16). Mission 19 reuses Zao's classifier.
  - **Rivet and Zoom's ladder** (`_08_nonaiagent.Rivet`, `ZoomFetchesLadder` with `ZoomGear`) —
    GOAP chains them (10), the Mega Mutt drops them into Mission 2 (17), exactly as the spec's
    presenter notes ask.
  - **Howl and Fifi's loop** (`_03_loop`) — the poster (3), and nested inside the Mega Mutt for
    the Gazette story (17). Fifi's rules are a `Rules` PIN, not part of her prompt, so the same
    critic grades a poster and an article.
  - **The storm inspections** (`_04_parallel`) — in parallel (4), and as a sequence with one async
    step (20). **The ice voters** (`_13_voting`) — cold (13), and after a real inspection (18).
  **The chain runs through the DEFAULT INPUTS, never at run time**: `SequentialPattern.KITTEN` is
  shared by 2, 17 and 21, `VotingPattern.LAKE` by 13 and 18, `SinglePattern.LOST_HAT` by 1. Nothing
  is passed between missions while they run, so a skipped mission or a deep link from a slide never
  strands what follows.
- **The Pawer Rangers: one cast, one colour, one job — and the name says it.** From the spec:
  | Colour | Ranger | Breed | Job | In the code |
  |---|---|---|---|---|
  | Black | **Zao** | Bouvier des Flandres | leader: decides who goes where, names the culprit | classifier (6), merge (4), supervisor (9), blackboard verdict (12) |
  | Blue | **Sniff** | Beagle | finds things — gear: `sniff`, `followTrail` | 1, 2, 4, 5, 6, 9, 11, 12, 13, 18, 21 |
  | Yellow | **Zoom** | Greyhound | runs, fetches, delivers — gear: `fetch`, `deliver` | 2, 4, 8, 10, 11, 15 |
  | Green | **Dig** | Dachshund | tunnels and tight spots | 4, 6, 7, 10, 12 |
  | White | **Doc** | St. Bernard | medic, decides what is safe, says no often | 2, 6, 10, 13, 19, 21 |
  | Purple | **Howl** | Husky | writes and argues, loud | 2, 3, 14, 18 |
  | Pink | **Fifi** | Poodle | critic and judge | 3, 14 |
  | Silver | **Rivet** | robot dog | maths and lookups — **plain Java, no model** | 8, 10, 12, 13, 17, 18 |
  Supporting cast: **Officer Jo** (the human, Mission 7), **Marmalade the cat** (the villain — an AI
  agent in the debate, the prime suspect in every crime), **the Mayor** (loses things, owns roses).
  **Rivet and Marmalade were Bolt and Mittens until 2026-10-04**, renamed because a super-powered
  dog called Bolt next to a cat called Mittens is the cast of Disney's *Bolt* (2008). When naming
  any new character, check it is not a well-known one first — and avoid Disney's cats in
  particular (Duchess, Figaro, Lucifer).
  Rules that fell out of building it:
  - **An agent's `.name(...)` is its Ranger; its interface name is its job in this mission**
    (`SniffFinds`, `ZoomFetchesLadder`, `DigSteadies`). The diagram label is the Ranger, so the room
    learns eight names once. Where one mission has the same Ranger twice the second gets its own
    name (`DigActs`, `ZoomRescue`/`ZoomSquirrel`/`ZoomNap`, `SniffVote`), because `markNode`
    lights every box whose label matches — two boxes called "Zoom" would both light.
  - **"Color = job" is on the schematic**: `Topology.Node.as("sniff")` sets the Ranger and
    `render.js` draws a badge in the box corner in `var(--r-sniff)` (`app.css`). A badge, not a
    tinted stroke, because Doc is WHITE and vanishes on the light canvas. The schematic test fails
    any box labelled as a Ranger without a colour.
  - **Rivet is always drawn as `code`**, never as an agent — wherever he appears. And **a non-AI
    agent is invisible to the listener in a sequence** (see the non-AI note above), so Rivet's box
    never lights there; his effect is on the Scope tab. Oddly, whether he is reported depends on how he
    is nested: invisible as a direct sub-agent of a sequence or of Mission 13's top-level
    VotingPlanner, but reported when that planner is nested one level down (Mission 18). Do not
    build anything on either behaviour — read his effect from the scope.
  - **Gear is real tools, and the model picks them.** `SniffGear`/`ZoomGear` are `@Tool` classes
    passed with `.tools(...)`; `@P(name = "place", ...)` is needed because the build does not keep
    parameter names (without it the model is offered `arg0`). `StreamingListener` emits
    `tool-call`/`tool-result` from `beforeAgentToolExecution`/`afterAgentToolExecution`, `app.js`
    lights the gear box whose label starts with the tool name, and `ChatCallLog` prints a
    tool-only reply as `tool call: sniff({...})` instead of an empty arrow. Gear returns canned
    Barkville FACTS — a tool is where facts come from — and its shed is the source of truth
    (`ZoomGear.fetch` rounds UP to the next ladder it actually has).
  - **The acts are the categories** (`team`, `workflow`, `planner`, `minds`, then `composite`,
    `production`). The catalogue stays in mission-number order (so `_NN_` holds), and the rail and
    gallery group by category — which is how missions 1 and 8 sit together in Act 1, as the spec's
    running order asks, without renumbering anything.
  - **Live-model lessons carried over from earlier casts, all still true:** a persona prompt
    ("You are the St Bernard…") makes a small model role-play and lose judgement, so a persona
    goes only where the voice IS the output and stays short; a prompt that ends in a marker puts
    the decision FIRST; "who guards it" lets a human in where "which dog guards it" does not; mock
    triggers key on an INSTRUCTION, never on a dog's name alone (the same Ranger speaks in a dozen
    missions); and a verdict-giving agent is told that "not proven" is not a verdict.
  - **Pins that must grow need an appender.** An agent's output OVERWRITES its key, so the spec's
    `clues` list (Mission 12) is one pin per clue — which is also what lets a clue be the
    precondition of somebody else's contribution.
  - **Mission 13 is LangChain4j's `VotingPlanner`, not a parallel workflow with a hand count** —
    which is what it was for a while. The planner calls EVERY sub-agent at once, collects each
    output as a vote, and returns `strategy.aggregate(votes)` as the result. `VotingStrategy` is a
    one-method interface, so the spec's safety rule is a lambda: `VotingPattern.VETO` (one NOT SAFE
    wins), passed as `new VotingPlanner(VETO)`. The result also shows what the library's own
    `VotingStrategy.majority()` would have said on the same votes — the spec's "majority for the
    mascot, veto for safety" made literal, with both strategies real. One trap: `majority()` counts
    EQUAL votes, and "SAFE — the ducks are on it" is a different vote from "SAFE, 12 cm", so votes
    are read down to their verdict word before `majority()` sees them. Mission 18 nests the same
    planner and strategy as one step of its sequence. `theVetoOverrulesTheMajorityOnIce` asserts
    both use `VotingPlanner` and neither uses `parallelBuilder`.
  - **Mission 14 is LangChain4j's `DebatePlanner`, not a loop dressed as one** — it was a
    `loopBuilder` of Howl, Marmalade and a Rivet minute-taker for a while, which showed a loop and
    called it a debate. How the real planner works, read from its bytecode in `1.20.0-beta30`:
    **every sub-agent but the LAST is a debater and the last is the judge** (so `subAgents(howl,
    marmalade, fifi)` is the whole casting); each round it calls ALL debaters together — they run
    concurrently, so within a round nobody hears the other — then writes the previous round's
    statements into the scope as `debateContext` ("Howl: …\nMarmalade: …"), which is what the next
    round answers; after each round it asks the `ConvergenceStrategy` (`unanimous()` = word for
    word identical, so prose never converges; `unanimousLastWord()` = both end on the same word),
    and on convergence or `maxRounds` it calls the judge once with that context. Consequences:
    `debateContext` holds ONLY the last round, so Fifi rules on closing statements and the result
    rebuilds the full transcript from `scope.agentInvocations()`; round 1's context is EMPTY, so
    the debaters are told an empty last round means "open your case" (live, Marmalade otherwise
    opened with "Howl has yet to offer an argument"); and `debateContext` is the library's key,
    so `Keys.DebateContext` is the one `TypedKey` that overrides `name()` — with the library's
    constant, `DebatePlanner.DEBATE_CONTEXT_KEY`. `theCouncilArguesThreeRoundsBeforeFifiRules`
    asserts the planner is used and both debaters read that key.
  - **Missions 11, 12 and 15 were rebuilt because they were too simple to show their pattern.**
    Two peers alternating looked like a loop; three clue-finders reading only the crime plus a
    judge looked like a fan-out and a join; and BDI's beliefs never changed during the run, so it
    was a priority list. Each now makes the ONE behaviour that distinguishes it visible in the
    Run events, and each was read out of the library's bytecode first — worth doing again on a
    version bump, because these planners are small and their semantics are the whole demo:
    - **P2P (corn maze): three peers, one pin with two listeners.** `P2PPlanner` runs every agent
      whose input pins all exist, and after EACH agent finishes, re-arms every agent that reads
      the key it wrote — then calls everything activatable at once. So Sniff (reads Zoom's
      `Clearing` and Dig's `Burrows`) writes `Scent`, which wakes Zoom AND Dig together, and
      each of their reports wakes Sniff again. Seed `Clearing`/`Burrows` or nobody moves
      ("stable after 0 invocations"). It is genuinely chatty: when two reports land a moment
      apart, Sniff fires once for each — a live run took 13 turns, hence the cap of 20 and the
      caveat saying so. The test asserts Sniff first, then {Zoom, Dig} as a wave, and that the
      predicate (FOUND: in any pin) stopped it — never the exact interleaving, which is the
      planner's. The prompts are explicit if/else scripts on what the OTHER pups said: on
      gemma, "once your run takes you to the middle" let Zoom find the goat on his first run,
      and the peers never needed each other.
    - **Blackboard (sausage heist): preconditions, not a fan-out.** `BlackboardPlanner` picks,
      each step, ONE agent whose inputs are all on the board and that has not fired since they
      last changed, using a `ConflictResolutionStrategy` when several qualify. So the inputs ARE
      the preconditions: Sniff and Rivet need the crime; Dig needs Sniff's `ScentClue`; Doc needs
      Rivet's `CameraClue`; Zao needs `TunnelClue` + `CrumbClue` and writing `Culprit` is the goal.
      They are registered **backwards** (Zao, Doc, Dig, Rivet, Sniff) and `declarationOrder()`
      only breaks ties among the eligible, so the run goes Rivet → Doc → Sniff → Dig → Zao —
      interleaving two chains nobody wrote — and the result prints that order beside the
      registration order. The diagram has NO agent→agent edge (the test pins it): every arrow
      goes into or out of the board, which sits in the middle column.
    - **BDI (squirrel!): multi-step intentions, preemption, resumption.** A `Desire` is
      priority + achievable + satisfied + a PLAN (its agent types, in order). `BDIPlanner`
      commits to the highest achievable unsatisfied desire, and after EVERY step re-checks: if
      a higher desire has become achievable it preempts, remembering the cursor
      (`desireProgress`), and later resumes there; a plan that runs out without satisfying its
      desire is an `IllegalStateException`, not a retry. The demo needs a belief that changes
      mid-run, so Zoom's squirrel plan step 1 (`ZoomUpTheBank`) writes `Lookout` — what he
      sees — and the rescue is achievable only once that says "stranded". The run is squirrel
      1/2 → rescue 1/2 → rescue 2/2 → squirrel **2/2** → nap; with "Officer Jo already has the
      kid, safe" on the radio it is squirrel 1/2 → 2/2 → nap. Beliefs are read in plain Java.
    - **On gemma, "pin your clue on the Pup Board" is an invitation to role-play** — headings,
      emoji pins, "Clue Status: PLACED" — and the facts the next Ranger needs fall out. Clue
      prompts are "Report it in two short plain sentences, no headings", and they carry the
      facts (cat's prints, number 9, the 07:02 snack): the twist is only fair if the board
      makes it, and a small model will otherwise convict "the Rat".
  - **A voter's criterion must be narrow enough to disagree.** Mission 13's whole lesson is a 2–1
    SAFE majority overruled by a veto. When Sniff judged "what you can sniff and see", he saw the
    dark patch too and voted NOT SAFE, so majority and veto agreed and the lesson vanished. He now
    judges ONLY who is already out on the ice and whether it holds them.
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
      ranked multi-step plans and a belief that changes mid-run; blackboard's contributors each
      need a different clue on the board, so the order emerges. See the Missions 11/12/15 note.
      **`p2p` was the fourth, and it hid for longer because its exit predicate looked fine.**
      It was `hasState(Agreement.class)`, and `Agreement` was the *second peer's own output
      key* — so it was true the instant that peer had run, on any model, and the run always
      stopped after exactly one exchange. A two-step sequence with a planner bolted on top, and
      its own test asserted the two invocations and called that correct. **Check a predicate
      for the failure in both directions: one that can never be true never terminates, and one
      that can never be false terminates immediately and silently.** It now reads the *content*
      of either peer's key (`Parsing.agreed`), so the run is propose → counter → sign, and the
      test asserts three turns precisely because two would mean the old bug is back.
      Two things `P2PPlanner` pinned down on the way:
      - **It activates an agent only once every input it declares is present.** With nothing
        seeded for the first peer to read, neither can start and the run ends "stable after 0
        invocations" — no agents, no error, no result. `P2pPattern` seeds an empty `Counter`
        for that reason, in plain Java, and that seeding is also what makes the peers symmetric.
      - **It is reactive, and an agent re-fires when an input changes.** So two peers writing
        one *shared* key trigger each other **and themselves**, race, and run to the cap with
        the value oscillating — which is what happened when this was first rewritten that way.
        Distinct keys, each peer reading the other's, give the ping-pong a direction without
        giving either peer authority.
    - *Output nobody can check.* "Write a vivid tale" makes a failed run look like a good one.
  - **2. The audience must not need the domain explained.** This is the rule the *second* rewrite
    was for. A version of this catalogue set in a professional boarding kennel satisfied rule 1
    perfectly and still failed on stage: bloat, 21-day rabies clearances, run sizes and discharge
    notes all have to be *taught* before the pattern can be discussed, and a sentence of setup
    per demo is fifteen sentences across the talk — during which the room is learning kennels,
    not patterns. So every constraint a demo turns on is now one the room already holds: a cooked
    bone is dangerous and a croissant is not, a dog who suddenly starts snapping needs a vet and not a
    training tip, a Dachshund stuck under a fence needs rescuing and not a training tip, a puppy goes to the garden before he gets a training session, the chair cannot
    be pushed while the human is in the kitchen, the dog who does not fit through the dog flap did
    not carry the cake out through it, and with the pack leader declining to rule, neither of two
    dogs outranks the other about the sofa. **The test for a new scenario: would a dev in row 20 know the right answer before
    you finished reading the input aloud?** If not, it is the wrong scenario however good the
    pattern fit is.
  - **3. Something in the input must be visibly wrong, dangerous or funny — and the run must be
    seen dealing with it.** This is the newest rule and the one the catalogue was weakest on. The
    demos that land are the ones with an "oh no" the room spots before the first agent runs: the
    conker, the 85% chocolate, the 2-1 split, the Beagle sending it to the rescue dog, the ladder stopping
    at the book. The ones that died on stage all produced *admin* — "plan the meals for the days
    the owners are away" is a perfectly good pattern fit and a paragraph nobody watches. **This is
    not fixed by better prose.** A pass that only made the sentences wittier was rejected in the
    same words as the version before it ("pedestrian, not that fun to see"); what changed it was
    putting the consequence into the default input. So: write the input so the room can grade the
    run, and prefer a scenario that ends in a verdict, a split, a refusal or a route over one that
    ends in a document. The note-writing spine (`sequential`, `parallel`, both composites) is the
    standing offender, because the note is load-bearing for the reuse argument and a note is a
    document — those four have to earn their interest from the *input* and the timings, since the
    output is fixed.
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
    context object and only one mission out of twenty-one asks anybody anything. It emits `human-ask`
    **before** waiting — do it the other way round and the run blocks on a question nobody has
    been shown.
  The diagram gives the person the `human` role rather than `agent`, and
  `everySchematicShowsWhatItsMissionActuallyDoes` asserts it: drawn like the boxes either side, the
  picture would say the model decided, which is the one thing this pattern denies.
  In Mission 7 the person is **Officer Jo**, the one human in the cast. She is asked whether Dig
  may tunnel under the Mayor's prize roses, and her answer — yes, no or yes-but — is the
  `approved` pin `DigActs` reads. On stage, say no once: Dig does not dig and suggests another way.
  A trap this already paid for, and still worth knowing: the mock's reply reads what the person
  said from the AFTER-label slice of the prompt, and the test asserts on the outcome section
  (`**So Dig…**`), not the whole result — the result echoes Jo's own words, so a naive
  `contains("no")` passes on those even when Dig ignored her.
- **`customPlanner` (Mission 16, Zao's Nap Schedule) is your own `Planner`, on one slide.**
  `NapSchedule` implements `dev.langchain4j.agentic.planner.Planner` — `init(InitPlanningContext)`
  hands it the sub-agents, `nextAction(PlanningContext)` returns `call(agent)` or `done(result)`,
  and `context.agenticScope()` lets it READ and WRITE the board (typed `writeState(Key.class, v)`).
  The rule is plain Java: **hungry → feed; energy above 70 and not last on a mission → go;
  otherwise → nap**, nobody twice in a row. Feeding and napping change the board and call nobody,
  so `nextAction` loops internally until somebody is sent or it is bedtime (`BEDTIME` steps) — the
  two guard rails are the empty queue and that cap, and they are yours to write. It writes the
  current mission into `Call` before calling a Ranger, because Mission 6's Rangers read `Call`,
  and pins `Energy`, `LastOnMission`, `MissionQueue` and `Schedule` so the Scope tab shows the
  planner thinking. The result is the day's log, with FEED / GO / NAP in words — no emoji.
- **`megaMutt` (17) is the spec's "Mega Mutt" made literal**: Mission 2's kitten rescue with
  Rivet (8) dropped in between Sniff and Zoom, and Mission 3's loop nested at the end to polish the
  Gazette story. It exists to show that **the builders nest** — a loop is an agent, so it sits in a
  sequence like any Ranger. Its lesson is the glue: Sniff pins a sentence and Rivet needs a number,
  so `TapeMeasure` (plain Java) reads the metres; the rescue pins a health report and the loop
  needs a brief, so `GazetteBrief` (plain Java) writes one. And **the wiring chooses Zoom's output
  key** (`RescueStatus`, so Doc can read it) — the key is the contract between two agents, and a
  composite is where contracts get written.
- **`lakeParty` (18)**: a parallel mapper sends Sniff to four spots of ice, `IceReport` (plain
  Java) writes the findings back INTO `Mission` — because that is the key Mission 13's voters
  declared, and reusing an agent means accepting its key — then the same vote with the same veto
  (`VotingPattern.count`, public so it can be reused), and Howl announces it. The original words go
  first in the rewritten mission, or Rivet's ruler loses the measured thickness.
- **`demos/_NN_<id>/*`** — one public interface per agent (`@Agent` + `@UserMessage`/`@K`), so
  LangChain4j can build JDK proxies. Prompts are worded so `MockChatModel` returns parseable output.
- **`ModelFactory`** — resolves the shared `ChatModel` (Ollama or mock). Eager (observes `StartupEvent`)
  so the endpoint discovery and probe run at boot; `activeModel()` reports what is actually live, and
  `currentModel()` re-probes when the last attempt fell back. `PatternResource` calls `currentModel()`
  per run rather than injecting a `ChatModel` once — that is what makes recovery-without-restart work.
  It also resolves `tiers()` (see the `production` note) and `currentStreamingModel()`, both off the
  back of the same probe, so neither can be a real model while the ordinary one is not.
- **Streaming is one toggle on demo 1, and it is deliberately not a demo of its own.** A
  streaming *card* would demonstrate this app's SSE plumbing more than it demonstrates
  LangChain4j; a toggle on the simplest demo shows the API difference and nothing else. Four
  things make it work, and three of them are one-way doors:
  - **The return type is what makes an agent streaming**, not the builder. Hence
    `StreamingNoteRetriever`, a second interface with the *same prompt word for word* and
    `TokenStream card(...)` instead of `String card(...)`. `streamingChatModel(...)` on a method
    returning String changes nothing at all.
  - **Only the LAST agent of an `UntypedAgent` system can stream to a screen.**
    `PlannerBasedInvocationHandler` sets `allowStreamingOutput` from
    `UntypedAgent.class.isAssignableFrom(type) || TokenStream.class.isAssignableFrom(outputType)`,
    and `propagateStreaming()` is that *and* `planner.terminated()`. Put a step after the
    streaming agent and `AgentExecutor` wraps the stream in a `StreamingResponse` and drains it
    internally — the right behaviour, and the reason no other demo in the catalogue can offer
    this.
  - **`PatternDef.streams` is a secondary-constructor field**, false for twenty demos, so the
    one fact costs those files nothing. `PatternInfo` carries it to the page, which shows the
    toggle only where it is honoured — a control that silently does nothing reads as broken, not
    absent. `PatternResource` also checks `def.streams()` before building a streaming model.
  - **Tokens are drawn as plain text, not markdown**, and skipped by the event log. A
    half-arrived answer is usually mid-construct (an unclosed `**`), so re-rendering per chunk
    flickers between two layouts; `run-result` swaps in the rendered version at the end. Forty
    token lines in the Run events pane would bury the six events that describe the run's shape.
  `MockStreamingChatModel` delegates to `MockChatModel` for the answer and hands it over in
  word-ish chunks with an 18 ms pause — without the pause every token lands in the same
  millisecond and the demo shows a block of text appearing at once, which is exactly what
  streaming is supposed to look different from.
- **`MockChatModel`** — deterministic, no-network `ChatModel`, and the thing `mvn test` runs
  against. It matches **the last user message** (never the accumulated conversation — that would
  pin multi-turn planners to their first choice) against an **ordered rule table**, and returns
  canned, PARSEABLE answers. Each rule's comment says what it stands in front of. What it does
  that a canned table usually does not:
  - **Tool calls.** When the request carries tool specifications it plans the calls from the
    prompt (`toolPlan`), asks for ONE per turn as an `AiMessage` with a `ToolExecutionRequest`,
    counts the `ToolExecutionResultMessage`s since the last user message, and only answers once
    every planned call has a result — with the results appended to the text its rule reads. So
    Mission 1's `tool-call` events are real round trips through the framework's tool loop, offline.
    `MockStreamingChatModel` hands a tool-call turn over whole (`onCompleteResponse`), and streams
    only text.
  - **Item awareness** for the mappers (ducklings, ice spots) — eight identical lines would run a
    mapper perfectly and demonstrate nothing — and a **reactive supervisor plan** that reads the
    fair's problems out of the request and sends one Ranger per problem (`CALL_ARG` is the
    responders' `Call` key spelled out; rename that key and the plan breaks).
  - **Fifi alternates 2/4 then 4/4**, so both loops (3 and 17) visibly iterate once and exit; and
    Howl's first draft is loud and incomplete, his second fixed, so the score is earned.
  Hazards it already handles, each of which produced a wrong demo with no error at some point:
  - **Whitespace is collapsed before matching**, because the prompts are text blocks and a phrase
    that wraps is `"a\nb"` to `String.contains`.
  - **Triggers are INSTRUCTIONS, never quoted content**: a refining loop feeds its own output
    back in, and the merge/judge/verdict prompts quote every earlier answer — so the rule for the
    quoting agent goes FIRST (Zao's merge, Fifi, Zao's culprit), and no canned answer contains
    another rule's trigger.
  - **Never a dog's name alone**: the same Ranger speaks in a dozen missions, so "you are sniff"
    would claim them all. Triggers are the job ("find what this mission is looking for").
  - **Read only what was asked**: `after(p, "the call:")` and friends take the text after the
    LAST label, never the agent's own instructions — the classifier's prompt defines every
    category's words, and matching against it would route every call the same way.
  - **Two "at least"s**: Zoom's prompt says "at least" twice, and reading the last one found no
    number and fetched the 5 m ladder — the test that pins the 7.5 m one is what caught it.
  **A deterministic stand-in proves the wiring, never that a real model will follow a prompt** —
  check a mission against Ollama after touching its prompts.
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
- **`ChatCallLog`** — a `ChatModelListener` that logs the prompt sent and the answer returned, with
  elapsed time and token count, under the logger name **`chat`**. It is what makes the Server log tab
  worth projecting: without it the tab carries only the framework talking about itself ("activating
  agent X"), which is the shape of a run and none of its content. Three decisions worth keeping:
  - **INFO, not DEBUG.** The model's own `logRequests`/`logResponses` do this at DEBUG, which meant
    the most interesting half of the demo was invisible unless somebody remembered to raise a log
    level before going on stage. `ModelFactory` no longer sets those flags — this replaces them, so
    there is exactly one source of prompt logging and no duplicate lines.
  - **It works for the mock too.** `ChatModel.chat()`'s default implementation fires `listeners()`
    and then calls `doChat()` — so `MockChatModel` overrides **`doChat`**, not `chat`, and takes a
    listener list in its constructor. Override `chat` there and offline runs log nothing. Its no-arg
    constructor stays silent, which is what tests use.
  - **One line per call.** `trim()` flattens newlines to `⏎` and caps at 700 chars; a prompt is a
    thirty-line text block, and thirty log records per call is a wall nobody reads.
  The tab renders `chat` lines in brighter ink with a left rule, so the prompts stand out from the
  framework lines they are interleaved with — the interleaving is the point, the weighting is so it
  can be read from the back of a room.
- **The dark panes must not inherit `--ink`.** "Run events" and "Server log" are dark in *both*
  themes. `.pane.term` therefore sets `color:var(--term-ink)` explicitly: it previously inherited
  `body`'s `--ink`, which in light theme is `#1c1917` on a `#1b1917` background — every log message
  was invisible, and only the spans that set their own colour (time, level, logger) survived. For the
  same reason the agent name in Run events uses `--c-agent`, not `--accent2` (a dark teal in light
  theme, 3.2:1 there). Any new colour used inside those panes belongs in the theme-independent token
  block next to `--term-dim`.
- **`Topology` / `RunEvent`** — plain records describing the graph and the streamed events.
  `RunEvent.ScopeValue` (type + size + rendered value) is what makes the Scope tab a variables
  table rather than a wall of strings. `StreamingListener.describe` names types the way a reader
  expects — `List(3)`, not `ImmutableCollections$ListN` — and skips `__`-prefixed planner
  bookkeeping. Worth noticing on stage: `Score` shows as `String`, which is exactly why
  `demos._03_loop.RuffDraftCritic` returns one.
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
  title → run controls → **one stage showing ONE of two views**, switched by a Diagram / Data
  control at the right end of the run bar (or the **V** key, outside a text field). It used to be
  diagram above a bottom dock, and on a projector the diagram ended up too small to read — so the
  diagram now gets the whole stage when it is the thing being presented. The **Data** view is
  two stacked docks with their own tabs: **Result** / **Scope state** on top (what the run
  produced) and **Run events** / **Server log** below (how it got there, and the real prompts from
  `ChatCallLog`, with a level filter). The grip between them sets the bottom half's share as a
  **fraction**, not pixels, so a resize or a browser zoom keeps the proportion; it is persisted
  (`split`) with the chosen view (`view`). The Scope state tab is a debugger-style variables
  table (name / type / value, rows an agent just wrote highlighted, long values clamped until
  clicked, expansion surviving the next update). `showPane` switches only the half a pane
  belongs to (`GROUPS`), and `paneVisible` is "its tab is active AND the data view is showing".
  A run starts with the top half on Scope state (it fills as agents write) and ends on Result
  — unless the viewer picked a top tab during that run (`tabPinned`; picking the server log does
  not pin). **The view itself is never switched for the viewer**: on the diagram they are usually
  pointing at the timings the run left behind, so a finished result, or a WARN/ERROR in the log,
  puts a dot on the Data button instead.
  **The diagram zooms** (wheel or trackpad pinch toward the pointer, drag to pan once zoomed,
  double-click or `0` to fit, `+`/`−` keys and a corner −/%/+ bar). It is done on the **viewBox**,
  so it stays vector-sharp, and the fitted view is kept apart in `svg.dataset.base` (set by
  `drawGraph`) because zooming rewrites the live one. `fitEdgeLabels` measures against that BASE
  view, on purpose: measured against the zoomed viewBox it would shrink the labels straight back
  while everything else grew. A new diagram starts fitted; a run does not redraw, so you can zoom
  into the part about to be discussed and THEN press Run. Panning is clamped so at least half
  the drawing stays on the canvas.
- **`[hidden]{display:none !important}` is declared once in `app.css`, and it has to be.** The
  `hidden` attribute is only `[hidden]{display:none}` in the browser's own stylesheet, so any
  author rule that sets `display` on the same element silently beats it. The runtime badge
  (`.ran{display:inline-flex}`) sat in the controls as an empty pill before the first run for
  exactly that reason. A dozen elements on this page are toggled with `el.hidden` — the badge, the
  builds-on line, the human-in-the-loop panel, the unread dots, the dock panes and the two views
  (`.dataview` is `display:flex`, the newest case of exactly this) — so
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
  the canine character lives in the palette (**a Bouvier's coat**: cool salt-and-pepper neutrals,
  a steel blue-grey accent — the "blue" a grey Bouvier is called — and wheaten as the warm
  secondary, for the brindle stripe and the beard. It was fawn/rust on warm cream until
  2026-09-22, which is Malinois colouring, from when the catalogue had the breed wrong. Two
  things worth keeping if you re-cut it again: **the accent cannot be achromatic**, because
  `--accent` marks identity and selection and grey-on-grey marks nothing; and **the one warm
  note earns its place on a cool page** — a working agent still pulses wheaten, and on cool
  structure the warm box is the one the eye goes to, which is what that state is for. The paw
  mark and the canvas trail are `data:` URIs, so they bake the accent in and cannot read
  `var()`: both need editing by hand, and the trail needs a **separate dark rule** because a
  dark paw at 3% on a dark panel is absent, not faint), a
  drawn paw mark shared by the header and favicon, a near-subliminal paw texture on the empty
  canvas, and the pulse on a working agent. It must NOT live in emoji decoration, pun button labels
  ("Fetch"/"Heel") or twee empty states — those read as kitsch on a projector and undercut the
  talk. Labels stay plain; the agent names already carry the theme. Two glyphs remain, both
  functional rather than decorative: ☰ for the rail toggle and ⚠ on the caveat.
  **This rule is about the chrome, not about the writing.** The `story` beats are the one place
  the humour belongs — they are what the speaker says out loud, the talk is three hours long, and
  the room needs the laughs. The register is dry and observational (Zao "would like it known that
  this proves nothing"; Marmalade "has prepared"), and every punchline earns its place twice:
  `p2p`'s "nobody is in charge — not even Zao, who is outside, eating corn" **is** why it is not a
  supervisor, `bdi`'s "Squirrel. Kid. Squirrel." is the preempt-and-resume, `customPlanner`'s "Zoom is
  hungry" is the first rule the planner fires. A joke you have to stop and explain
  costs more time than it buys, so it is the wrong joke. Beats are capped at
  140 chars by `everyMissionHasItsBeat` — a beat is a sentence, not a paragraph.
  **Wordplay is allowed in the writing and in the agent names, on one condition: the pun has to
  be the accurate name too.** This used to read "never wordplay", which was the wrong rule for a
  Java audience — "Pawer Rangers" itself is the model: the pun is the name. The spec settles the
  agent names: **the Ranger names say the job** (Sniff finds, Zoom runs, Dig digs), so they stay
  plain on the diagram, and the jokes live in the beats and the canned answers. Beat lines that
  hold: `async`'s "nobody blocks the main thread waiting for a Beagle", `blackboard`'s crumb in
  Zao's beard (the spec's running gag), `goap`'s "the planner does not care".
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
  The rail collapses (header ☰) and the data view's split is drag-resizable by its grip (arrow
  keys too, double-click to reset); both persist in `localStorage` under `dashboard.layout`, so a reload
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
- **A node can carry a second line (`withSub`) and can be drawn as a stack (`asStack`).** Both
  exist because a diagram of the cast is not a diagram of the mechanism:
  - `goap` was pixel-for-pixel a sequence — same boxes, same left-to-right arrows. Its boxes now
    declare the key each one **needs** (`needs 'indoor'`) and its edges what they **write**, so
    the order reads as derived from the keys rather than typed by hand. That is the pattern's
    entire claim and it was invisible.
  - `parallelMapper` was a single box, so it said "one call" — the opposite of what a mapper
    does. The agent is now drawn as a stack, `once per item`.
  - `supervisor` was a symmetric star saying "talks to all four equally", which is a fan-out.
    The Beagle is now `1 · always first` with a two-way edge (the supervisor reads her answer),
    and the three desks are `2 · if she says so` behind one arrow.
  - `blackboard` was four identical satellites round a box, which said nothing about where the
    problem comes from, why the order is free, or how the run ever stops. Sub-lines were the
    first attempt at that and were not enough — **no sub-line survives being attached to a box
    the layout has already put in the wrong place.** `star` spaces satellites at top / right /
    bottom / left in declaration order, so `TrainerLead` — which can only act once all three
    notes exist, and is the step that *ends the run* — sat at the far **left**, where the eye
    starts, reading as a fourth peer. Plus eight arrows radiating from one box, no way in and
    no way out. It is `stages` now, with the board in the **middle** column and the Rangers
    either side of it: the column a Ranger stands in is the earliest it CAN act (Sniff and Rivet
    need only the crime; Dig and Doc need a clue; Zao needs two), every arrow goes into or out
    of the board, and each box's sub-line says what it needs (`needs: the scent`). The problem
    and the goal state are both on the page.
  - `goap`'s goal box says `registered: park first` while the boxes run indoor → garden → park.
    That one line is the pattern's whole claim; without it the order looks typed, and the reader
    has to be *told* it was derived — not having to be told is what the picture is for.
  - Only the **return** half of a two-way pair is labelled. Both halves bow through the same gap,
    so the supervisor's "invoke" and "names who it needs" landed on top of each other; of the two
    it is the answer that carries the mechanism. Same convention as the blackboard's write/read.
  - `bdi` carried its priorities inside the agent names (`GardenLeave (p30)`); they are a second
    line now, which also stopped the names truncating.
  The sub-line sits *inside* the box with the name shifted up, so every box stays one size and
  the layout maths is untouched. `everyTopologyShowsWhatItsPatternActuallyDoes` asserts these
  three claims, because each of them is a distinction that would quietly disappear in a tidy-up.
- **A circle has no before and after, so a directional pattern must not be drawn in one.** The
  `star` and `mesh` layouts space nodes evenly round a circle in declaration order. That drew the
  debate's judge to the *left* of its advocates — a verdict arriving before the argument — and
  the supervisor as a wheel with four equal spokes, which is a picture of the fan-out that demo
  spends its time denying. Both are `stages`, and a test pins them there.
  **Blackboard was the last holdout and it has gone the same way**, which retires the exception
  this note used to carve out for it ("the board is genuinely the centre and the contributors
  genuinely have no order"). Neither half is true any more — Mission 12's contributors have
  preconditions — and a pattern only needs **one** node with a position to lose the right to a
  circle. `star` and `mesh` are now used by nothing; keep them for the pattern that
  is genuinely orderless end to end, and reach for columns first. When *any* part of a pattern
  has a direction, give the whole thing columns and let the shared column carry the symmetry.
- **Every diagram needs its way out drawn, not only its way round.** Three of them didn't:
  - the **loop** had the return arc and no exit, so it was two agents circling for ever and the
    exit condition — the whole of what you have to get right — was the one thing not on the page;
  - **p2p** was two boxes passing a proposal back and forth with no end at all, which is the
    pattern's *caveat* rather than its behaviour (the exit predicate is the box on the right).
    Two further things were wrong with it, and both drew a hierarchy the pattern exists to
    deny: the question arrived at **one** peer, and **one** peer reached the exit. Both peers
    read the question and either can sign, so it is two arrows in and two arrows out now, and
    the test pins both counts;
  - the **escalation ladder** stacked its three rungs in one column, which is pixel-for-pixel demo
    6's branch diagram — one input arriving at one of three desks, the exact reading a cost ladder
    exists to correct. One column per rung now, cost rising left to right.
- **An arrow between two agents beats any caption underneath it.** This is the strongest rule on
  this list and the one that cost the most: `goap` was drawn `goal → hoover → children →
  cyclists`, nose to tail, with `needs 'Hoover'` written under the boxes. That is
  pixel-for-pixel the sequential demo, and a reader takes the arrows and ignores the small
  print — so the picture said *somebody typed this order* while the text said *the planner
  derived it*, and the picture won. Sub-lines cannot argue a diagram out of its own shape.
  The fix is structural, not textual: **no edge may join two GOAP agents.** They sit in one
  column in *registration* order (which is backwards), a `planner` node fans out to them, and
  the positions the search derived ride on those arrows — `runs 3rd`, `runs 2nd`, `runs 1st`,
  reading down. The mismatch between the order they are listed in and the order they run in is
  the whole pattern, and it is now the first thing you see.
  `everyTopologyShowsWhatItsPatternActuallyDoes` asserts both halves: no agent→agent edge, and
  three arrows labelled `runs …`. The `planner` **role** exists for this — framework machinery
  drawn as a dotted italic box (`render.js` class list, `.node.planner` in `app.css`), so the
  thing deciding the order does not read as a fourth agent.
- **In a `stages` diagram an edge that skips a column arcs over the top** (`span` in `drawGraph`,
  nested by how far it jumps so the short hop stays lowest). Node fills are opaque, so a
  skip-ahead edge drawn flat does not look crowded — it silently *disappears* behind whatever
  stands between its ends. The ladder's three ways out are all skip-ahead edges, and flat they
  said only the last rung can answer.
  **The row layouts need this too, and for a long time did not.** `chain`/`dag`/`loop` place
  nodes in declaration order along one row, so an edge skipping a node is just as hidden — and
  `bdi` paid for it when it was a row: the edge from the first desire to the third, straight
  behind the second, was invisible. `span` now measures the declaration-index gap for those
  layouts instead of returning 0. (`bdi` is `stages` now — one row per desire's plan, a
  `planner` node in front, and the belief revision drawn as the edge coming back from the
  chase's first step.)
- **A label is trimmed at 22 characters and a sub-line at 26, silently.** `fit()` does it with no
  error, so an over-long one is simply wrong on the projector and nowhere else.
  `everyTopologyShowsWhatItsPatternActuallyDoes` caps labels at 22 and subs at **24** — not 26,
  because at 10.5px in a 150px box 26 characters touch both walls.
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

## Adding a mission (the common task)

1. Make a package `demos/_NN_<id>/` — the position it takes in `PatternCatalog.build()`, then
   the pattern id in lowercase. The leading `_` is required: a package segment cannot start with
   a digit. Inserting rather than appending means renumbering the packages after it.
2. Write the mission the spec's way first: one sentence, the Rangers involved, the Pup Board pins,
   when it stops. Then put one file per agent in the package (one `@Agent` interface each, named
   for the Ranger's job here, built with `.name("<Ranger>")`), a `Keys.java` for the pins it
   introduces, an `XxxPattern`, and a `package-info.java`. Reuse a Ranger from the mission that
   introduced him rather than writing a new one. Give every Ranger box `.as("<ranger>")` so it
   wears his colour, draw Rivet and gear as `code`, and pick a category from the four acts. Then
   add one line to `PatternCatalog.build()`.
3. If running under the mock, add a rule to `MockChatModel`'s table — and mind where you put it:
   the table is ordered, and a rule keyed on a word that appears in quoted content will hijack
   another agent's prompt.
4. Add a claim of its own to `PatternCatalogTest` — the thing the speaker says on stage, asserted —
   and extend `everySchematicShowsWhatItsMissionActuallyDoes` with what its diagram must show.
   Then run it against Ollama: the mock proves the wiring, not the prompts.
The frontend needs no change — it renders whatever `/api/patterns` returns.

## Foreign agent config detected

An OpenAI Codex config exists at `~/.codex` (AGENTS.md + skills). Reply `/import` to scan and list what's
importable, then `/import --yes` to apply user-level items.
