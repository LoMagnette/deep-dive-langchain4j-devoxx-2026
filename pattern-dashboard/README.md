# LangChain4j Agentic Patterns — Zao Dashboard

A Quarkus web app that visualizes and **live-runs** all 13 LangChain4j agentic patterns,
themed around a Belgian dog named **Zao**. Each pattern is streamed over Server-Sent Events
and animated on an SVG topology graph, with a live scope-state panel and an event console.

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

- `GET /api/patterns` — JSON metadata + static topology for all 13 patterns.
- `GET /api/patterns/{id}/run?input=...` — `text/event-stream` of `RunEvent`s
  (`run-start`, `agent-before`, `agent-after`, `agent-error`, `run-result`, `run-done`).

## The 13 patterns

Workflows: `single`, `sequential`, `loop`, `parallel`, `parallelMapper`, `conditional`.
Pure agents: `supervisor`.
Pattern zoo: `goap`, `p2p`, `blackboard`, `voting`, `debate`, `bdi`.
