# LangChain4j Agentic Patterns — Zao Dashboard

A Quarkus web app that visualizes and **live-runs** 19 LangChain4j agentic patterns plus two
composite systems. Each run is streamed over Server-Sent Events and animated on an SVG topology
graph, with a live scope-state panel, an event console, and the server log — including the real
prompts and completions.

Built on **standard LangChain4j**, not `quarkus-langchain4j`: every agent is wired by hand through
`AgenticServices` builders, because the builders are what the talk is about. There is deliberately
no helper that shortens them.

The setting is **Zao**, a Belgian shepherd, and the household he runs. Every demo problem has to
pass two tests at once, and both are load-bearing:

1. **The pattern must be load-bearing** — take it away and the answer visibly degrades. The vote
   can genuinely split, the critic has named rules to check, the planner has an order to discover.
2. **The audience must not need the domain explained** — the constraint each demo turns on is one
   everybody already holds. Grapes are dangerous and cheddar is not. Hot pavement burns paws. A
   puppy goes to the garden before he gets a training session. Recall works in the garden before
   it works at the park. Neither half of a couple outranks the other about the bed.

The second test is the one that is easy to fail: a scenario that needs a sentence of setup needs
it fifteen times over, and then the room spends the talk learning the domain instead of the
patterns.

Every step is timed, and the page shows it three ways — under each node on the diagram, against
each line in the event pane, and as a badge beside the Run button carrying the whole run against
how long the agents were busy inside it. That second pair is the point:

```
sequential       whole run 387 ms   agents busy 328 ms
parallel         whole run 162 ms   agents busy 304 ms
parallelMapper   whole run 165 ms   agents busy 783 ms
```

The patterns argue for themselves without a slide.

## Running it

```bash
mvn quarkus:dev                         # dev mode + live reload (needs Maven 3.9+)
mvn quarkus:dev -Ddashboard.model=mock  # deterministic offline run — no Ollama, no API key
mvn -DskipTests package                 # fast-jar into target/quarkus-app/
mvn test                                # smoke-runs all 21 demos against the mock model
```

Then open <http://localhost:8080>, pick a pattern, and hit **Run**.

### Default: a local Ollama

The default is `dashboard.model=auto` against a **local Ollama** through the native
`langchain4j-ollama` client — no `/v1` suffix, no API key.

```bash
ollama serve
ollama pull "$OLLAMA_MODEL"   # or whatever dashboard.ollama.model-name is set to
mvn quarkus:dev
```

Two things `ModelFactory` does that are worth knowing before the app surprises you:

- **The base URL is discovered, not hardcoded.** It probes `http://localhost:11434` (Ollama on this
  machine) and then `http://host.docker.internal:11434` (app in a container, Ollama on the host).
  Hardcoding either breaks the other *invisibly* — a host that does not resolve fails exactly like
  a stopped Ollama. Set `OLLAMA_BASE_URL` to pin one endpoint and skip discovery.
- **The probe is a cheap `GET /api/tags`, never a real `chat()`.** It proves the server is up *and*
  that the wanted model is in the list, so a typo or an un-pulled model is reported at boot by
  name. A real call would run inference and can take 10–20 s on a thinking model, timing out while
  Ollama is perfectly healthy.

On failure `auto` falls back to `MockChatModel` with a loud `WARN` and **re-probes** on the next run
(10 s cooldown), so starting Ollama recovers without restarting the app. Every `run-start` event
names the model that is actually live, so a fallback is visible in the Run events pane rather than
silent. `dashboard.model=ollama` disables the fallback and fails loudly instead; `mock` forces
offline.

### Configuration

All of it lives in `src/main/resources/application.properties`.

| Property | Env override | Default | What it does |
|---|---|---|---|
| `dashboard.model` | — | `auto` | `auto` \| `ollama` (no fallback) \| `mock` (forced offline) |
| `dashboard.ollama.base-url` | `OLLAMA_BASE_URL` | *(empty — discover)* | Pin one endpoint and skip the probe |
| `dashboard.ollama.model-name` | `OLLAMA_MODEL` | `gemma4:e4b-mlx` | Must be a model the server actually serves |
| `dashboard.ollama.timeout` | `OLLAMA_TIMEOUT` | `PT2M` | Per request; thinking models routinely pass 30 s on one step |
| `dashboard.ollama.cheap-model-name` | — | *(unset)* | Second, smaller tier for the `modelRouting` demo |
| `quarkus.http.port` | — | `8080` | |

`dashboard.ollama.cheap-model-name` is worth setting before the talk: with nothing configured,
`ModelTiers.distinct()` is false and the `modelRouting` demo says so out loud rather than implying
a saving that never happened. Point it at something genuinely small (`llama3.2:1b`,
`qwen2.5:0.5b`) and pull it first — it is probed exactly like the main model.

Running against real OpenAI, or any OpenAI-compatible endpoint, means re-enabling the
`langchain4j-open-ai` dependency in `pom.xml` and the OpenAI branch in `ModelFactory`; both are
commented out, and the commented block in `application.properties` is the config that goes with
them.

### Offline

`MockChatModel` is a deterministic, no-network `ChatModel` that pattern-matches the last user
message against an ordered rule table and returns canned, *parseable* answers. It is what `mvn test`
runs against, and it exists so the app survives a conference network:

```bash
mvn quarkus:dev -Ddashboard.model=mock
```

Or against a packaged build — note that the `-D` has to come **before** `-jar`, or the JVM treats it
as a program argument and ignores it:

```bash
mvn -DskipTests package
java -Ddashboard.model=mock -jar target/quarkus-app/quarkus-run.jar
```

The canned answers are written to be good demo content rather than filler: the picnic mapper really
does clear the cheddar and condemn the grapes, the refinement loop really does score 0.60 and then
0.95, and the second-dog vote really does split two to one.

> **What the mock does not prove.** It pins the *wiring*, never that a real model will follow a
> prompt. This has already bitten once: the supervisor's hand-off was special-cased in the mock, so
> every test passed while the live demo called one agent and stopped. Check any demo whose prompts
> you touch against a real model before the talk.

> **Packaged-jar gotcha:** the fast-jar's `Class-Path` fails to decode from a directory whose path
> contains emoji or spaces (`Error decoding percent encoded characters`). Copy it to a plain path
> first: `cp -r target/quarkus-app /tmp/app && java -jar /tmp/app/quarkus-run.jar`.

## The page

Two hash routes share the main column:

- `#/` — the **gallery**, one section per category, each card carrying the demo's story beat and a
  label-free thumbnail of its topology drawn by the same layout code as the live diagram.
- `#/<id>` — the **tester**: story → run controls → full-width SVG diagram → a dock with four tabs.
  Deep-linkable straight from a slide (`#/loop`, `#/secondDogCouncil`).

The four dock tabs are **Result** (rendered markdown), **Scope state** (a debugger-style
name/type/value table, with the rows an agent just wrote highlighted), **Run events**, and
**Server log** — where `ChatCallLog` puts the real prompts and completions at `INFO`, interleaved
with the framework's own lines. A dot flags a `WARN`/`ERROR`, or a finished result, on a tab you
have not looked at.

The `single` demo carries a **stream tokens** toggle. It is the only one that can: LangChain4j only
propagates a token stream from the *last* agent of a system, so a demo with a step after the
streaming agent would drain the stream internally. The page shows the toggle only where the
catalogue says it is honoured.

The rail collapses, the dock is drag-resizable, and the theme is an explicit light/dark toggle that
overrides the OS — on a projector the OS preference is rarely the right one. All of it persists in
`localStorage` under `dashboard.layout`, so a dev-mode restart mid-talk does not undo how the
room's view was set up.

## API

- `GET /api/patterns` — JSON metadata and static topology for all 21 demos.
- `GET /api/patterns/{id}/run?input=...&stream=false` — `text/event-stream` of `RunEvent`s:
  `run-start`, `agent-before`, `agent-after`, `agent-error`, `human-ask`, `human-answer`, `token`,
  `run-result`, `run-done`. `run-start` carries a `runId` and names the live model; the events that
  finish something carry `millis`. `stream=true` is honoured only where the catalogue allows it.
- `POST /api/patterns/runs/{runId}/answer?text=...` — the other half of a human-in-the-loop run.
  SSE is one-way, so the person's answer comes back as its own request.
- `GET /api/logs` — `text/event-stream` mirroring the server log into the browser.

## The catalogue

Twenty-one demos in one order, and the order is the argument: it runs left to right along the
autonomy dial, from a step where the model decides nothing to planners that decide every turn. The
rail, the gallery and the talk all use it.

### Workflows — *you decide the path*

| # | Pattern | The problem it is shown on |
|---|---|---|
| 1 | `single` | A rambling "thanks for having Zao!!" message becomes a structured sitter card |
| 2 | `sequential` | …then the timed checklist for the fridge door — a different reader, so a second agent |
| 3 | `loop` | A useless note ("just feed him twice, he knows the routine") refined until four named rules hold |
| 4 | `parallel` | Demo 1's card, fanned out: what he eats and when he goes out, planned at once and joined |
| 5 | `parallelMapper` | Five things off the picnic blanket, one verdict each — and you know all five answers |
| 6 | `conditional` | He ate a bar of dark chocolate: vet, trainer, or everyday care? |
| 7 | `humanApproval` | The same chocolate, except your sister is the one standing there — **a person approves the answer** before it reaches her |
| 8 | `nonAiAgent` | A record lookup and a guard, both plain Java, on either side of an LLM step — nobody should invent a microchip number |

### Pure agents — *the model decides the path*

| # | Pattern | The problem it is shown on |
|---|---|---|
| 9 | `supervisor` | A four-year-old dog has started snapping near his bed. A triage nurse takes the call and names who is needed; **that answer is what causes the second call** |

### Pattern zoo — *the middle ground: a planner decides the turns*

| # | Pattern | The problem it is shown on |
|---|---|---|
| 10 | `goap` | Recall: indoors → garden → park, **registered backwards on purpose** |
| 11 | `p2p` | Should the dog sleep on the bed? Neither half of the household outranks the other |
| 12 | `blackboard` | He has started barking all day: exercise, what changed, or what he can see? |
| 13 | `voting` | A second dog? Three criteria over one household — money says yes, the other two say later |
| 14 | `debate` | Two weeks in Tuscany: take him, or leave him with a sitter? |
| 15 | `bdi` | The puppy's first hour, ordered by desire **priority** rather than declaration order |
| 16 | `customPlanner` | A hand-written `Planner`: the book, then the trainer, then the vet — stop at the first rung that can answer |

### Putting it together — *several patterns wired into one system*

| # | Pattern | The problem it is shown on |
|---|---|---|
| 17 | `sitterNote` | *Composite:* routing + parallel + merge + refinement loop → the note on the fridge |
| 18 | `secondDogCouncil` | *Composite:* mapper + debate + vote, and the glue agents between them |

### Running it for real — *not where on the dial; what it takes to run it*

These three are **modifiers, not shapes**. Every demo above is a topology — a chain, a fan-out, a
loop, a star. These are one call each, and any of them can be bolted onto any demo above, which is
exactly why they sit outside the ordering rather than inside it.

| # | Pattern | The problem it is shown on |
|---|---|---|
| 19 | `modelRouting` | `chatModel(Function<AgenticScope, ChatModel>)` — chocolate goes to the strong model, kibble to the cheap one |
| 20 | `async` | `async(true)` — the out-of-hours line takes a minute; the rest of the note writes itself meanwhile |
| 21 | `resilience` | `optional(true)` for a missing **input**, `errorHandler(...)` with a retry counter for a failing **call** |

`PatternCatalogTest.theDemoProblemsActuallyDemonstrateTheirPattern` asserts the claims above, so a
prompt change that turns a pattern back into decoration fails the build rather than the talk.

`customPlanner` is the only one whose behaviour lives in this repo rather than in the library:
`EscalationPlanner` implements `dev.langchain4j.agentic.planner.Planner` in about forty lines. The
decision depends on *what came back from the previous rung*, which is the one thing none of the
built-in builders can express, and the only good reason to write a planner yourself. Change the
question and it stops at a different rung — "which food should I buy?" stops at the book, "he pulls
like a train on the lead" stops at the trainer, a limp goes all the way.

## How it is laid out

One package per demo, plus a few shared ones. Every package carries a `package-info.java` saying
what it is for — that is the shortest way in.

```
demos/<id>/      EVERYTHING for one demo and nothing else: its agent interfaces
                 (one per file), its Keys, its XxxPattern, its package-info
catalog/         PatternCatalog (the registry) · PatternDef · Topology
support/         Parsing · Errors — the shared pieces that are ours, not LangChain4j's
model/           ModelFactory · MockChatModel · MockStreamingChatModel · ChatCallLog
run/             RunEvent · StreamingListener · AskHuman · HumanQuestions · ModelTiers
web/             PatternResource · LogResource · LogStream — REST and SSE
```

The package is named after the pattern id, lowercased, so the deep link on a slide (`#/loop`) names
the package to open on stage (`demos.loop`).

Every `XxxPattern` is two methods in this order: **`run` then `define`**. `run(model, input,
listener)` is the wiring and nothing else — it is what gets opened in front of the room, so it comes
first and carries no topology or catalogue text. `define()` holds the diagram and the gallery copy,
and hands the wiring over as a method reference.

An agent lives in the demo that **introduces** it, and later demos import it from there —
`sitterNote` imports the loop's `FridgeChecklist` and the routing demo's `WorryRouter`;
`secondDogCouncil` imports the three assessors `voting` introduced. A composite reuses the parts
rather than re-implementing them, and its import list says so before a word of explanation.

Every scope key is a `TypedKey`, never a string literal, and each demo has a `Keys.java` for the
ones it introduces. A key is a record with no body — `public record Notes() implements
TypedKey<String> {}` — because `TypedKey.name()` already defaults to the record's simple name, so
the key is `"Notes"`, the `{{Notes}}` placeholders match it, and a parameter takes
`@K(Notes.class)` rather than `@V("Notes")` so neither end of the contract is a string.
`noDemoAddressesTheScopeWithAStringLiteral` reads the demo sources and fails on a relapse.

The frontend is four static files with no build step: `index.html`, `app.css`, `render.js` (pure
rendering — escaping, the markdown subset, topology layout and drawing) and `app.js` (routing, the
catalogue, SSE runs, the dock). Classic deferred scripts in that order, not ES modules. Adding a
pattern needs no frontend change at all: the page renders whatever `/api/patterns` returns.

### One run, end to end

`index.html` opens an `EventSource` → `PatternResource.run` looks up the `PatternDef`, builds a
`StreamingListener`, and calls `def.run(model, input, listener)` → the `Runner` wires agents through
`AgenticServices.*Builder()` with the listener attached and invokes → agent callbacks and scope
snapshots stream back as `RunEvent`s → the page animates the topology and updates the scope panel.

## Adding a pattern

1. Make a package `demos/<id>/`, named after the pattern id in lowercase.
2. Put one `@Agent` interface per file in it, a `Keys.java` for any scope keys it introduces, an
   `XxxPattern` with `run` then `define`, and a `package-info.java`.
3. Add one line to `PatternCatalog.build()`, in the place the rail order argues for.
4. Add a rule to `MockChatModel`'s table — and mind **where**: the table is ordered, and a rule keyed
   on a word that appears in quoted content will hijack another agent's prompt.
5. Extend `PatternCatalogTest.theDemoProblemsActuallyDemonstrateTheirPattern` with the claim the new
   pattern makes, and `everyTopologyShowsWhatItsPatternActuallyDoes` with what its diagram must show.

## Versions

Java 21 · Quarkus 3.39.2 · `langchain4j` 1.20.0 · `langchain4j-agentic(-patterns)` 1.20.0-beta30.

The `langchain4j-agentic` module is **experimental and subject to change** — the speaker flags this
on stage, and you should expect API churn when bumping the version.
