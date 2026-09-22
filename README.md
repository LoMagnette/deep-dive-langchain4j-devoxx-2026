# Devoxx Belgium — "Agentic Systems in Java with LangChain4j" (3h deep dive)

Talk workspace. Two halves, and only one of them is code:

| | |
|---|---|
| **[`pattern-dashboard/`](pattern-dashboard/)** | **The live demo.** A Quarkus app that visualizes and *runs* 19 agentic patterns plus two composite systems, all set in the life of Zao, a Bouvier des Flandres. This is the thing that goes on the projector, and the thing you build and edit. See [its README](pattern-dashboard/README.md). |
| This file | The talk's spine: thesis, through-line, section order, speaker notes. |

## The one-sentence thesis

**Autonomy is a dial, not a feature: start as far left (deterministic) as the problem allows, and
hand the LLM the wheel only where you genuinely cannot enumerate the path — because the pattern you
don't adopt is the one you don't have to debug.**

## The vibe

Told as **"From Puppy to Pack" 🐾** — raising intelligence you don't fully control, from a puppy on
a leash (you decide every step) to a pack that coordinates with nobody writing the steps. Warm and
funny on the left of the dial, awe-and-nature-documentary on the right.

The humour lives in the **story beats** — what gets said out loud — and nowhere else. It stays out
of the app's chrome: no emoji decoration, no pun button labels. The dog is in the craft (the coat
colours in the palette, the drawn paw mark, the pulse on a working agent), not in the jokes.

## The through-line diagram (repeat at every transition)

```
 DETERMINISTIC ◄──────────────────────────────────────────────────────► AUTONOMOUS

  nonAiAgent   single  sequential  loop  parallel  conditional  │  GOAP  P2P  blackboard  │  supervisor
               parallelMapper  humanApproval                    │  voting  debate  BDI    │
               ├────────────── workflows ─────────────────────┤ │  customPlanner          │

     "you decide the path"              "a planner decides the turns"        "the model decides
                                                                                  the path"
```

Off the dial entirely, and deliberately so:

- **Composites** (`sitterNote`, `secondDogCouncil`) — several patterns wired into one system. The
  builders *nest*: each composite is itself an `UntypedAgent` another builder takes as a sub-agent.
- **Running it for real** (`modelRouting`, `async`, `resilience`) — not positions on the dial but
  *modifiers*, one call each, that bolt onto any pattern above.

Note the shape of the walk: §1–§5 go left to right through the workflows, §6 jumps to the far right
(the supervisor — the pivot), and §7 comes back to the middle ground. That is why the dashboard's
rail shows `supervisor` *before* the pattern zoo: the rail order is the talk order.

## Section plan

| § | What it covers | Demos in the dashboard |
|---|---|---|
| 1 | One agent doing one job — `@Agent`, AI-service-with-benefits, the `AgenticScope` | `single` |
| 2 | Sequential — the workhorse; stop being the wire | `sequential` |
| 3 | Loop — generator + critic, exit conditions, `maxIterations` | `loop` |
| 4 | Parallel and parallel-mapper — fan-out, join, the two timing numbers | `parallel`, `parallelMapper` |
| 5 | Conditional routing + typed keys (keeping shared state honest) | `conditional` |
| 6 | **Supervisor — hand the wheel to the LLM. The pivot of the talk.** | `supervisor` |
| 7 | Custom planner, GOAP, and the pattern zoo — the middle ground | `goap`, `p2p`, `blackboard`, `voting`, `debate`, `bdi`, `customPlanner` |
| 8 | Non-AI agents, human-in-the-loop, error recovery | `nonAiAgent`, `humanApproval`, `resilience` |
| 8½ | Observability — `ChatModelListener` → OpenTelemetry → Langfuse/Phoenix, cost and latency | the **Server log** tab (`ChatCallLog`), `modelRouting`, `async` |
| 9 | The capstone that composes everything | `sitterNote`, `secondDogCouncil` |
| 10 | Take-home: the "workflow or agent?" flowchart and heuristics | — |

> **Speaker notes are not in this repo.** Earlier drafts of this README linked a set of `NN-*.md`
> planning documents (master plan, narrative thread, one per section). They have never been
> committed here — the section table above is the surviving outline. Either add them, or treat this
> file as the spine.

## Every example is real code you can open on stage

The demo app is not slideware: all 21 entries run live, on a real model, from the same
`AgenticServices` builder calls the room is being taught. Deliberately **standard LangChain4j**,
not `quarkus-langchain4j` — the builders are the subject, so there is no CDI magic between the
slide and the call.

- Every pattern: `pattern-dashboard/src/main/java/dev/devoxx/dashboard/demos/_NN_<id>/`, one
  package per demo, numbered by its place in the running order and then named after the pattern id
  — so the packages read top to bottom in talk order, and the deep link on a slide (`#/loop`) still
  names the package to open (`demos._03_loop`). The leading `_` is Java's, not a style choice: a
  package segment cannot start with a digit.
- Every `XxxPattern` opens with `run(model, input, listener)` — the wiring and nothing else, first
  in the file, because that is what gets projected.
- The one piece of framework code written here rather than imported: `EscalationPlanner`, a
  hand-written `dev.langchain4j.agentic.planner.Planner` in about forty lines (`demos/_16_customplanner/`).
- Upstream, for the foils and the prose: `langchain4j-agentic-patterns/` (GOAP, BDI, blackboard,
  debate, voting, P2P) and the module's own tutorial, `docs/docs/tutorials/agents.md`.

Two claims the talk makes are asserted by the build rather than by the speaker:
`PatternCatalogTest.theDemoProblemsActuallyDemonstrateTheirPattern` (a prompt tweak that turns a
pattern back into decoration goes red) and `everyTopologyShowsWhatItsPatternActuallyDoes` (a diagram
that stops showing the mechanism goes red).

