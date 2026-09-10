# LangChain4j Agentic Patterns — Zao Dashboard

A Quarkus web app that visualizes and **live-runs** all 13 LangChain4j agentic patterns plus
two composite systems. Each pattern is streamed over Server-Sent Events and animated on an SVG
topology graph, with a live scope-state panel and an event console.

The setting is a working Ardennes boarding kennel and rescue, where the Belgian shepherd **Zao**
is the resident dog — and the setting is load-bearing rather than decorative. Every demo problem
is picked so that **removing the pattern visibly degrades the answer**: the kennel has hard
constraints (capacity, vaccination rules, medication times), competing interests (a full kennel
versus welfare rules, two families wanting the same rescue dog), and artefacts that can be wrong
in ways an audience can check for itself. A bloat call belongs at the emergency desk; a booking
with an expired rabies booster must be declined; a discharge note either names every dose or it
does not.

Open <http://localhost:8080> after starting.

## Default: local Ollama (stage)

The default model is a standard langchain4j `OpenAiChatModel` pointed at a **local Ollama**
OpenAI-compatible endpoint — no external network, no real API key required.

```bash
ollama pull llama3.1
ollama serve            # serves the OpenAI-compatible API on http://localhost:11434/v1
mvn quarkus:dev         # requires Maven 3.9+ for dev mode
```

Then open <http://localhost:8080>, pick a pattern on the left, and hit **Run**.

Configuration (see `src/main/resources/application.properties`, override via env vars):

| Property | Env override | Default |
|---|---|---|
| `dashboard.model` | — | `openai` |
| `dashboard.openai.base-url` | `OPENAI_BASE_URL` | `http://localhost:11434/v1` |
| `dashboard.openai.api-key`  | `OPENAI_API_KEY`  | `ollama` (Ollama ignores it) |
| `dashboard.openai.model-name` | `OPENAI_MODEL`  | `llama3.1` |

## Real OpenAI

Same code path — just override the env vars:

```bash
OPENAI_BASE_URL=https://api.openai.com/v1 OPENAI_API_KEY=sk-... OPENAI_MODEL=gpt-4o-mini mvn quarkus:dev
```

## Offline fallback (no Ollama / no key)

A deterministic `MockChatModel` (no network, parseable canned responses) lets the whole app
run with nothing installed — handy for CI or a laptop with no model server:

```bash
mvn quarkus:dev -Ddashboard.model=mock
```

Or against a packaged build:

```bash
mvn -DskipTests package
java -jar target/quarkus-app/quarkus-run.jar -Ddashboard.model=mock
```

> Note: the fast-jar's `Class-Path` cannot be decoded from a directory whose path contains
> emoji/spaces. If you hit `Error decoding percent encoded characters`, copy `target/quarkus-app`
> to a plain path first (e.g. `cp -r target/quarkus-app /tmp/app && java -jar /tmp/app/quarkus-run.jar`).

## API

- `GET /api/patterns` — JSON metadata + static topology for all 13 patterns and both composites.
- `GET /api/patterns/{id}/run?input=...` — `text/event-stream` of `RunEvent`s
  (`run-start`, `agent-before`, `agent-after`, `agent-error`, `run-result`, `run-done`).

## The catalogue

| Pattern | The problem it is shown on |
|---|---|
| `single` | A hurried drop-off note becomes a structured boarding record |
| `sequential` | …and then the run sheet the kennel hand carries — a different reader, so a second agent |
| `loop` | A jargon-filled discharge note refined until four named rules hold |
| `parallel` | Capacity and paperwork checked at once; **any FAIL declines the booking** |
| `parallelMapper` | The morning round: one inspection per occupied run, gathered into a watch-list |
| `conditional` | The out-of-hours line: emergency / behaviour / booking, where mis-routing is fatal |
| `supervisor` | "Sort out his week" — you cannot enumerate which specialists that needs |
| `goap` | A boarding quote: audit → allocate → price, **registered backwards on purpose** |
| `p2p` | An overbooked bank holiday: foreman vs. welfare officer, neither outranks the other |
| `blackboard` | Why has the dog stopped eating? Medical, behaviour and feeding notes on one board |
| `voting` | Is this rescue dog safe with a toddler? Three rubrics over a self-contradicting dossier |
| `debate` | Two homes, one dog: the counter-case has to be stated before the panel rules |
| `bdi` | The morning shift, ordered by desire **priority** rather than declaration order |
| `nightHandover` | *Composite:* routing + parallel + merge + refinement loop → the night handover sheet |
| `placementCouncil` | *Composite:* mapper + debate + vote, and the glue agents between them |

`PatternCatalogTest.theDemoProblemsActuallyDemonstrateTheirPattern` asserts the claims above, so
a prompt change that turns a pattern back into decoration fails the build rather than the talk.
