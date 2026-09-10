# LangChain4j Agentic Patterns — Zao Dashboard

A Quarkus web app that visualizes and **live-runs** all 13 LangChain4j agentic patterns plus two
composite systems. Each pattern is streamed over Server-Sent Events and animated on an SVG
topology graph, with a live scope-state panel and an event console.

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
| `single` | A rambling "thanks for having Zao!!" message becomes a structured sitter card |
| `sequential` | …then the timed checklist for the fridge door — a different reader, so a second agent |
| `loop` | A useless note ("just feed him twice, he knows the routine") refined until four rules hold |
| `parallel` | Walk him now? Weather and dog checked at once; **either can veto** |
| `parallelMapper` | Five things off the picnic blanket, one verdict each — and you know all five answers |
| `conditional` | He ate a bar of dark chocolate: vet, trainer, or everyday care? |
| `supervisor` | "A baby is due in three months" — you cannot enumerate what that needs |
| `goap` | Recall: indoors → garden → park, **registered backwards on purpose** |
| `p2p` | Should the dog sleep on the bed? Neither half of the household outranks the other |
| `blackboard` | He has started barking all day: exercise, what changed, or what he can see? |
| `voting` | A second dog? Three criteria over one household — money says yes, the other two say later |
| `debate` | Two weeks in Tuscany: take him, or leave him with a sitter? |
| `bdi` | The puppy's first hour, ordered by desire **priority** rather than declaration order |
| `sitterNote` | *Composite:* routing + parallel + merge + refinement loop → the note on the fridge |
| `secondDogCouncil` | *Composite:* mapper + debate + vote, and the glue agents between them |

`PatternCatalogTest.theDemoProblemsActuallyDemonstrateTheirPattern` asserts the claims above, so
a prompt change that turns a pattern back into decoration fails the build rather than the talk.
