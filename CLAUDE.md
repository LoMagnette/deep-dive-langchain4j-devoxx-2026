# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

Workspace for a Devoxx Belgium 3-hour deep-dive talk, "Agentic Systems in Java with LangChain4j."
Two distinct halves:

- **Root** (`README.md`) — talk planning: the through-line is "autonomy is a dial," told as "From Puppy
  to Pack." The `NN-*.md` planning docs referenced in the root README are the speaker's notes.
- **`pattern-dashboard/`** — the live demo: a Quarkus web app that visualizes and **runs** all 13
  LangChain4j agentic patterns, themed around a Belgian shepherd named **Zao**. This is the code you
  will actually build and edit.

## Commands (run inside `pattern-dashboard/`)

```bash
mvn quarkus:dev                       # dev mode + live reload; needs Maven 3.9+. Open http://localhost:8080
mvn quarkus:dev -Ddashboard.model=mock  # no Ollama / no API key — deterministic offline run
mvn -DskipTests package               # build fast-jar to target/quarkus-app/
mvn test                              # smoke-runs all 13 patterns on the mock model (%test profile)
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

Backend is a handful of small classes in `src/main/java/dev/devoxx/dashboard/`; the frontend is one
static file.

- **`PatternCatalog`** — the heart. A `@ApplicationScoped` registry of all 13 patterns built in `build()`.
  Each pattern is a `PatternDef` = display metadata (name, category, `useful`, `caveat`) + a static
  `Topology.Graph` (for the SVG) + a `Runner` lambda that wires and invokes the agents live. **This is
  where you add or change a pattern** — every wiring here is deliberately written to double as readable
  demo code for the talk. Categories: `workflow` (single, sequential, loop, parallel, parallelMapper,
  conditional), `pure-agent` (supervisor), `pattern-zoo` (goap, p2p, blackboard, voting, debate, bdi).
- **`Agents`** — all agent contracts as public nested interfaces (`@Agent` + `@UserMessage`/`@V`), so
  LangChain4j can build JDK proxies. Prompts are worded so `MockChatModel` returns parseable output.
- **`ModelFactory`** — resolves the shared `ChatModel` (Ollama or mock). Eager (observes `StartupEvent`)
  so the endpoint discovery and probe run at boot; `activeModel()` reports what is actually live, and
  `currentModel()` re-probes when the last attempt fell back. `PatternResource` calls `currentModel()`
  per run rather than injecting a `ChatModel` once — that is what makes recovery-without-restart work.
- **`MockChatModel`** — deterministic, no-network `ChatModel`. It pattern-matches **the last user
  message** (never the accumulated conversation — that would pin multi-turn planners to their first
  choice) and returns canned, PARSEABLE answers (a score alternating 0.60/0.95 so loops visibly iterate
  then exit; "POSITIVE" for sentiment so voting converges; a line ending "AGREE" so debate/consensus
  fires; a 3-step supervisor plan activity→meal→done). If you change an agent prompt in `Agents`, keep
  these heuristics in mind or the mock run will break — `mvn test` will tell you. Its care-category
  branch must stay in step with `PatternCatalog.CATEGORIES`, and its canned supervisor plan names
  `ActivityPlanner`/`MealPlanner` literally — renaming those two agents breaks the supervisor demo.
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
- **`src/main/resources/META-INF/resources/index.html`** — the entire single-page frontend: renders the
  SVG topology, consumes the SSE stream, animates agent activity, shows the live scope-state panel, and
  hosts the bottom dock with its two tabs ("Run events" from `/api/patterns/{id}/run`, "Server log" from
  `/api/logs`, with a level filter and a dot that flags a WARN/ERROR you haven't looked at yet).

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
