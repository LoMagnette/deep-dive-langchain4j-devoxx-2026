# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

Workspace for a Devoxx Belgium 3-hour deep-dive talk, "Agentic Systems in Java with LangChain4j."
Two distinct halves:

- **Root** (`README.md`) — talk planning: the through-line is "autonomy is a dial," told as "From Puppy
  to Pack." The `NN-*.md` planning docs referenced in the root README are the speaker's notes.
- **`pattern-dashboard/`** — the live demo: a Quarkus web app that visualizes and **runs** all 13
  LangChain4j agentic patterns, set in the life of **Zao**, a Belgian shepherd, and the household
  he runs. This is the code you will actually build and edit.

## Commands (run inside `pattern-dashboard/`)

```bash
mvn quarkus:dev                       # dev mode + live reload; needs Maven 3.9+. Open http://localhost:8080
mvn quarkus:dev -Ddashboard.model=mock  # no Ollama / no API key — deterministic offline run
mvn -DskipTests package               # build fast-jar to target/quarkus-app/
mvn test                              # smoke-runs all 13 patterns + both composites on the mock model
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

Backend is a handful of small classes in `src/main/java/dev/devoxx/dashboard/`; the frontend is four
static files (no build step).

- **The catalogue is one file per rail category**, so the source layout mirrors the talk's arc and
  "where does this pattern go?" has one answer:
  - `PatternCatalog` — registry only (~40 lines): what is in the catalogue and in what order.
  - `PatternDef` — the entry type, with nested `Runner` (the live wiring) and `PatternInfo` (the
    JSON the page gets: the same thing minus the runnable part).
  - `Wiring` — `agent()`, `str()`, `result()`: the lines every pattern repeats.
  - `Parsing` — `score()`, `category()`, `items()`: defensive readers for what a model *actually*
    returns. Every one exists because a real model broke a pattern with no error at all.
  - `WorkflowPatterns` (6) · `PureAgentPatterns` (1) · `ZooPatterns` (6) · `CompositePatterns` (2).
  **To add a pattern:** write it in the group matching its category and list it in that group's
  `all()`. `PatternCatalog` does not change. Every wiring is deliberately written to double as
  readable demo code, so keep the helpers statically imported — `agent(...)`, `score(s)` read the
  way they did when it was all one class.
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
- **`Agents`** — all agent contracts as public nested interfaces (`@Agent` + `@UserMessage`/`@V`), so
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
  `Agents.FridgeRuleCheck` returns one.
- **`src/main/resources/META-INF/resources/`** — the frontend, four files, no build step:
  `index.html` (90 lines of markup), `app.css`, `render.js` (pure rendering: HTML escaping, the
  markdown subset, topology layout/drawing — functions of their arguments, which is why the same
  `layout()` serves both the live diagram and the gallery thumbnails) and `app.js` (routing, the
  catalogue, SSE runs, the dock, layout chrome). Classic deferred scripts in that order, not ES
  modules — they share globals and load in sequence. The **theme bootstrap stays inline in
  `<head>`**: it has to set `data-theme` before first paint or dark users get a white flash, and an
  external file cannot guarantee that. Two hash routes share the main column: `#/` is the **gallery** (a card grid — one card per pattern with its
  category, its `useful` line, and a label-free thumbnail of its topology drawn by the same `layout()`
  the real diagram uses, so a fan-out is recognisable from a chain at a glance), and `#/<id>` is the
  **tester**. Cards are real `<a href="#/id">` anchors, so Back, keyboard and open-in-new-tab work
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
  `renderMarkdown` is ~40 lines with no dependency (a CDN is the one thing sure to fail on conference
  wifi). It escapes the text **before** introducing any tag, so model output can never inject markup;
  keep that order if you extend it. Known simplification: nested bullets flatten to one level.
  **Theming rule: the dog is in the craft, not in the jokes.** This is shown on a Devoxx stage, so
  the canine character lives in the palette (a Belgian shepherd's fawn/rust coat on warm paper), a
  drawn paw mark shared by the header and favicon, a near-subliminal paw texture on the empty
  canvas, and the pulse on a working agent. It must NOT live in emoji decoration, pun button labels
  ("Fetch"/"Heel") or twee empty states — those read as kitsch on a projector and undercut the
  talk. Labels stay plain; the names in `Agents` already carry the theme. Two glyphs remain, both
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

1. Add the agent interface(s) to `Agents`.
2. Add a `private PatternDef xxx()` method in `PatternCatalog` (topology + `Runner`) and register it in
   `build()`.
3. If running under the mock, make sure the new prompts hit a sensible `MockChatModel` branch.
The frontend needs no change — it renders whatever `/api/patterns` returns.

## Foreign agent config detected

An OpenAI Codex config exists at `~/.codex` (AGENTS.md + skills). Reply `/import` to scan and list what's
importable, then `/import --yes` to apply user-level items.
