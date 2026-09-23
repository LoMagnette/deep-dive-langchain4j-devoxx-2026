# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

Workspace for a Devoxx Belgium 3-hour deep-dive talk, "Agentic Systems in Java with LangChain4j."
Two distinct halves:

- **Root** (`README.md`) — talk planning: the through-line is "autonomy is a dial," told as "From Puppy
  to Pack." The `NN-*.md` planning docs referenced in the root README are the speaker's notes.
- **`pattern-dashboard/`** — the live demo: a Quarkus web app that visualizes and **runs** all 19
  LangChain4j agentic patterns, set in the life of **Zao**, a Bouvier des Flandres, and the household
  he runs. This is the code you will actually build and edit.

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
demos/_NN_<id>/  EVERYTHING for one demo, and nothing else:
                   its agent contracts, one interface per file
                   its XxxPattern — topology + Runner
                   package-info.java — what this demo is for
                 NN is its place in the talk, so the tree reads in running order
  _01_single/ _02_sequential/ _03_loop/ _04_parallel/ _05_parallelmapper/
  _06_conditional/ _07_humanapproval/ _08_nonaiagent/
  _09_supervisor/
  _10_goap/ _11_p2p/ _12_blackboard/ _13_voting/ _14_debate/ _15_bdi/
  _16_customplanner/
  _17_sitternote/ _18_seconddogcouncil/
  _19_modelrouting/ _20_async/ _21_resilience/
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
    `TriageNurse`. Nine tests go red at once if you drop it, which is how this was established.
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
  `#/secondDogCouncil` at `demos._18_seconddogcouncil`. Two things about that shape:
  - **The leading `_` is not decoration — a package segment cannot start with a digit.** `01_single`
    is a compile error ("illegal underscore"); `_` is one of the three characters Java allows a
    segment to begin with, so it is the price of having the number first.
  - **`NN` is the index into `PatternCatalog.build()`, and nothing reads it at run time.** The id
    in `PatternDef` is still `loop`, the route is still `#/loop`, and no code derives a package
    from an id — so a number that has drifted out of step with the catalogue is invisible to the
    build and wrong only to a reader. **Reordering the rail now means renaming packages**, on top
    of the `buildsOn` renumbering that moving `nonAiAgent` already cost once. That is the standing
    price of this scheme; pay it deliberately or not at all.
- **An agent lives in the demo that introduces it**, and later demos import it from there. That is
  deliberate, and worth pointing at on stage: `sitternote` imports the loop's `RuffDraftCritic`
  and the routing demo's `WorryRouter`; `seconddogcouncil` imports the three assessors `voting`
  introduced. A composite reuses the parts rather than re-implementing them, and its import list
  says so before a word of explanation. Two shared default inputs work the same way —
  `SinglePattern.SITTER_MESSAGE` (also used by `sequential`) and `VotingPattern.HOUSEHOLD` (also
  used by the council).
- **`PatternCatalog` is the registry and nothing else**: twenty-one `XxxPattern.define()` calls in
  the talk's running order, grouped by comments for the five rail categories. Adding a demo is a
  new package plus one line here.
- **An agent does not have to be a model, and `nonAiAgent` is the general case.**
  `AgentUtil.agentToExecutor` falls through to `nonAiAgentToExecutor` for anything that is not
  already an agent, so **any plain object with one `@Agent` method goes straight into
  `subAgents(...)`** — `@K` parameters bound from the scope, return value written to the output
  key, the sequence unable to tell. `HumanInTheLoop` (demo 7) is the library's own instance of
  this; `demos/_08_nonaiagent/` is your own class, on both ends of an LLM step. **It is demo 8, the
  last of the workflows** — not in `production` with the other late additions — because "the
  model decides nothing at all" is a genuine position on the autonomy dial, and the far-left one.
  It was in `production` first, and moving it cost a renumbering of every `buildsOn` from
  `supervisor` onwards; that renumbering is the price of the rail order meaning something. Three
  things it pinned down:
  - **A non-AI agent is INVISIBLE to the listener in `1.20.0-beta30`.**
    `NonAiAgentInstance.setParent` sets the parent and never calls
    `registerInheritedParentListener` — which `AgentInvocationHandler:253` and
    `PlannerBasedInvocationHandler:343` both do. The field, the method and
    `composeWithInherited` are all there; the one call is missing. So a plain-Java step emits no
    `agent-before`/`agent-after`, is never timed, and **its node never lights on the diagram**.
    The demo makes that the lesson rather than hiding it, and
    `theJavaStepsAreIndistinguishableFromTheModelStepAndActuallyDoTheWork` pins the current
    behaviour: **if that assertion goes red on a version bump the library fixed it — delete the
    assertion and rewrite the demo's caveat, which will have become wrong.** (This is also why
    `humanApproval` works: `StreamingListener.askHuman` emits `human-ask`/`human-answer` by hand,
    so that demo never depended on the inheritance that is missing here.)
  - **`name` goes on the annotation, not a builder.** There is no builder for a POJO, and the
    default is the *method* name — `FlatFile` would be called `lookup` everywhere. Same trap
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
    not optional's problem however optional it is. So `resilience` skips `MedicationNote` because
    most dogs are on nothing and nothing writes `meds`, and the seeding of that key is **plain
    Java in `run`**: deciding whether you hold a value is not a job for a model. Delete the
    tablets from the input on stage and the step vanishes with no error.
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
  the fix was made and never pinned. They now have `theDebateConvergesOnAgreementAndNotOtherwise`,
  `theTwoPeersSettleOnThePredicateRatherThanRunningOutOfRounds` and
  `anyBlackboardContributorCouldGoFirstAndOnlyTheLeadCanGoLast`. The last of those asserts from
  the **interfaces**, not from a run, and that is the general lesson: a run shows one order, and
  one order is exactly what a sequence shows too — so the claim "any of them could go first" has
  to be read off the declared `@K` keys, which is what actually makes it true.
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
- **The demos build on each other, and that is the narration.** Each `PatternDef` carries a
  `story` (its beat: a weekend away, the beard, the chocolate, a baby coming, the second dog) and a
  `buildsOn` naming what it inherits. Read in catalogue order the twenty-one beats are one passage;
  read down the `buildsOn` lines they are one system being assembled. The tester shows both above
  the explanation, the gallery cards show the beat so the grid reads as the story, and `←`/`→`
  walk the catalogue in order.
  **Three spines carry the reuse:**
  - **The sitter note** — `single` introduces `NoteRetriever`; `sequential` reuses it and adds
    `FridgeMagnet`; `loop` reuses *that* agent unchanged and draws a critic and a loop around
    it; `sitterNote` uses the same two a third time. Nothing about the agent changes between
    demos 2, 3 and 17 — only the control around it, which is the entire argument.
  - **The three desks** — `conditional` introduces `EverydayCare`/`DogTrainer`/`EmergencyVet`, and
    then four demos put a different control flow around the same cast: routing picks one,
    `humanApproval` adds a person before the answer is acted on, `supervisor` picks several and
    decides when to stop, `customPlanner` tries them cheapest-first. **`supervisor` adds exactly
    one agent of its own** — the `TriageNurse`, and nothing else — so the §6 pivot is a change of
    *decider* over a cast the room already knows, not a new cast. `theDemosReuseWhatTheEarlierOnesBuilt`
    asserts that count exactly: one, and it must be her. (It said "no agent of its own" for a
    while, which was true of the version before the nurse and of nothing since; the demo's own
    `buildsOn` line said it too, on screen, while the diagram beside it drew her.)
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
    training tip, a fridge note needs the vet's
    number on it, a puppy goes to the garden before he gets a training session, you can call him
    off a hoover before you can call him off a cyclist, and neither half of a couple outranks the other about
    the bed. **The test for a new scenario: would a dev in row 20 know the right answer before
    you finished reading the input aloud?** If not, it is the wrong scenario however good the
    pattern fit is.
  - **3. Something in the input must be visibly wrong, dangerous or funny — and the run must be
    seen dealing with it.** This is the newest rule and the one the catalogue was weakest on. The
    demos that land are the ones with an "oh no" the room spots before the first agent runs: the
    conker, the 85% chocolate, the 2-1 split, the nurse sending it to the vet, the ladder stopping
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
    context object and only one demo out of twenty-one asks anybody anything. It emits `human-ask`
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
  behaviour lives in this repo rather than in the library. `demos/_16_customplanner/` holds all of
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
    declaring `@K(Findings.class) String` fails at runtime with a bare `argument type mismatch`.
  - **Parallel steps invoke the listener from several threads.** Anything collecting those events
    must be thread-safe — a plain `ArrayList` in a test silently drops them and reads as a flaky
    "that agent never ran". The SSE path is fine (Mutiny's emitter serialises), and is verified.
  - **A refinement loop feeds its own output back into the next prompt.** In the mock that means
    a rule matching a word which appears in the *note* hijacks the loop's second pass, and the
    composite returns the wrong stage's answer with no error at all. See the rule ordering note
    in `MockChatModel`.
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
  offline vote is a genuine 2-1 majority; a **catch-all** `argue` rule that hands both holiday
  advocates the same words so `ConvergenceStrategy.unanimous()` fires, against the council's two
  named rules that differ so it does not; a 2-step supervisor plan nurse→specialist→done; and an
  item-aware table so the mapper really does clear the croissant and condemn the cooked bone. Its
  worry-routing rule must stay in step with `Parsing.CATEGORIES`, and its canned supervisor plan
  names `TriageNurse` literally and reads `NEEDS: vet`/`trainer`/`everyday` out of the nurse's
  answer to pick the second call — renaming her, or changing that marker, breaks the demo.
  **Two traps this table has now sprung twice.** A rule whose trigger no prompt contains any more
  is worse than no rule: it reads as live behaviour and its comment describes a demo that no
  longer exists. Three such orphans survived two rewrites (the `parallel` demo's weather/pavement
  veto, and a `tighten this note` rule left over from the `NoteTightener` the capstone stopped
  using) — delete the rules a prompt change strands. And the holiday debate converges **via the
  catch-all**, not via the `comes or stays` rule, which is the *judge's*: insert a rule between
  them that tells the two advocates apart and that debate silently starts running two rounds like
  the council's. `theDebateConvergesOnAgreementAndNotOtherwise` is what catches it.
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
  title → run controls → full-width SVG diagram → bottom dock. The dock has four tabs: **Result**
  (rendered markdown), **Scope state** (a debugger-style variables table: name / type / value, with
  the rows an agent just wrote highlighted, and long values clamped until clicked — expansion
  survives the next update so a row doesn't collapse mid-run), **Run events**
  (`/api/patterns/{id}/run`) and
  **Server log** (`/api/logs`, with a level filter — the real prompts and answers land here from
  `ChatCallLog`, interleaved with the framework's own lines). A dot flags a WARN/ERROR — or a finished result —
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
  the room needs the laughs. The register is dry and observational (the sitter said yes *before*
  reading the message; the dog is not sorry), and every punchline earns its place twice: `goap`'s
  "He comes back indoors. Reliably. Indoors." **is** the precondition chain, `p2p`'s names why it
  is not a supervisor, `customPlanner`'s is the cost ladder. A joke you have to stop and explain
  costs more time than it buys, so it is the wrong joke. Beats are capped at
  140 chars by `everyDemoHasItsBeatInTheNarration` — a beat is a sentence, not a paragraph.
  **Wordplay is allowed in the writing and in the agent names, on one condition: the pun has to
  be the accurate name too.** This used to read "never wordplay", which was the wrong rule for a
  Java audience — `LeadDeveloper` plans the walks where the whole question is the lead, and a room
  of developers gets both halves before the next sentence. The condition is what keeps it from
  turning into kitsch, and it is doing real work: `BeardOverflow` judges snacks, `RuffDraftCritic`
  critiques a draft, `Watchdog` is a plain-Java guard, `FlatFile` is a lookup in a flat, `GardenLeave`
  takes the puppy to the garden, `HelloWorld` teaches him his name first, `FinalBoarding` rules on
  whether he flies. Names appear on the diagram, so a pun that costs the reader the mechanism is
  the wrong pun and the plain name wins — which is why `EmergencyVet`, `DogTrainer`, `EverydayCare`,
  `TriageNurse` are still plain: they are the cast five demos share,
  and the routing only reads because their names say exactly what they are. Same test as the beats,
  applied to a noun. Beat puns that hold: `parallel`'s "two threads, nothing shared, no locks",
  `parallelMapper`'s "he did the scatter, you do the gather", `async`'s "nobody blocks the main
  thread on hold music", `bdi`'s "get the order wrong and you mop".
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
- **A node can carry a second line (`withSub`) and can be drawn as a stack (`asStack`).** Both
  exist because a diagram of the cast is not a diagram of the mechanism:
  - `goap` was pixel-for-pixel a sequence — same boxes, same left-to-right arrows. Its boxes now
    declare the key each one **needs** (`needs 'indoor'`) and its edges what they **write**, so
    the order reads as derived from the keys rather than typed by hand. That is the pattern's
    entire claim and it was invisible.
  - `parallelMapper` was a single box, so it said "one call" — the opposite of what a mapper
    does. The agent is now drawn as a stack, `once per item`.
  - `supervisor` was a symmetric star saying "talks to all four equally", which is a fan-out.
    The nurse is now `1 · always first` with a two-way edge (the supervisor reads her answer),
    and the three desks are `2 · if she says so` behind one arrow.
  - `blackboard` was four identical satellites round a box, which said nothing about where the
    problem comes from, why the order is free, or how the run ever stops. Sub-lines were the
    first attempt at that and were not enough — **no sub-line survives being attached to a box
    the layout has already put in the wrong place.** `star` spaces satellites at top / right /
    bottom / left in declaration order, so `TrainerLead` — which can only act once all three
    notes exist, and is the step that *ends the run* — sat at the far **left**, where the eye
    starts, reading as a fourth peer. Plus eight arrows radiating from one box, no way in and
    no way out. It is `stages` now: the three note-takers share **one column**, which is how a
    picture says "no order"; the lead has its own after them, reached by an edge that arcs over
    them (`all three notes` — what "reads the whole board" looks like drawn rather than said);
    and the problem and the goal state are both on the page. The board keeps its own dashed
    box, because the shared state really is this pattern.
    Their three sub-lines are deliberately **identical** (`needs only the problem`): three boxes
    that say the same thing are three agents with nothing to tell them apart, which is the
    claim. The fourth reads differently because it is different.
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
  genuinely have no order"). Half of that is still true — the three note-takers have no order —
  but a pattern only needs **one** node with a position to lose the right to a circle, and the
  lead is that node. `star` and `mesh` are now used by nothing; keep them for the pattern that
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
  `bdi` paid for it: the edge from the first desire to the third, straight behind the second,
  is the one that makes it a DAG of preconditions rather than a chain, and it was invisible.
  `span` now measures the declaration-index gap for those layouts instead of returning 0, and a
  test asserts `bdi` still has an edge that skips a node.
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

## Adding a pattern (the common task)

1. Make a package `demos/_NN_<id>/` — the position it takes in `PatternCatalog.build()`, then
   the pattern id in lowercase. The leading `_` is required: a package segment cannot start with
   a digit. Inserting rather than appending means renumbering the packages after it.
2. Put one file per agent in it (one `@Agent` interface each), a `Keys.java` for any scope keys
   it introduces, an `XxxPattern`, and a `package-info.java` saying what the demo shows. Then add
   one line to `PatternCatalog.build()`.
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
