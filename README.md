# Devoxx Belgium — "Agentic Systems in Java with LangChain4j" (3h deep dive)

Talk planning workspace. Everything here is grounded in the actual `langchain4j-agentic` module in this repo (file paths + test references are real and citeable on stage).

## Read in this order

| File | What it is |
|---|---|
| **[00-master-plan.md](00-master-plan.md)** | **Start here.** The spine: thesis, the autonomy-dial through-line, audience, full 180-min time budget, the naive→crack→pattern rhythm, delivery mechanics. |
| **[00b-narrative-thread.md](00b-narrative-thread.md)** | **Read second.** The story bible — "From Puppy to Pack" 🐾: the thread that carries 3 hours, the cast, per-section beats, and energy mechanics. |
| [01-foundations.md](01-foundations.md) | §1 One agent doing one job — `@Agent`, AI-service-with-benefits, the AgenticScope. |
| [02-workflows-sequential.md](02-workflows-sequential.md) | §2 Sequential — the workhorse; stop being the wire. |
| [03-workflows-loop.md](03-workflows-loop.md) | §3 Loop — generator+critic, exit conditions, `maxIterations`. |
| [04-workflows-parallel.md](04-workflows-parallel.md) | §4 Parallel & parallel-mapper — fan-out + combine, `async`. |
| [05-workflows-conditional-typedkeys.md](05-workflows-conditional-typedkeys.md) | §5 Conditional routing + typed keys (keeping shared state honest). |
| [06-supervisor.md](06-supervisor.md) | §6 Supervisor — hand the wheel to the LLM; the pivot of the talk. |
| [07-planner-goap-patterns.md](07-planner-goap-patterns.md) | §7 Custom Planner + GOAP + the pattern zoo — the middle ground. |
| [08-production-hitl-observability.md](08-production-hitl-observability.md) | §8 Non-AI agents, human-in-the-loop, AgentMonitor, error recovery. |
| [08b-observability-production.md](08b-observability-production.md) | §8½ Observability past the AgentMonitor — `ChatModelListener` → OpenTelemetry → Langfuse/Phoenix, cost/latency metrics, the evals bridge. |
| [09-capstone-car-rental.md](09-capstone-car-rental.md) | §9 The real use case that composes everything. |
| [10-decision-guide.md](10-decision-guide.md) | Take-home: the "workflow or agent?" flowchart + heuristics. |
| [11-api-cheatsheet.md](11-api-cheatsheet.md) | Take-home: every factory, annotation, and interface on one page. |

## The one-sentence thesis
**Autonomy is a dial, not a feature: start as far left (deterministic) as the problem allows, and hand the LLM the wheel only where you genuinely cannot enumerate the path — because the pattern you don't adopt is the one you don't have to debug.**

## The vibe
Told as **"From Puppy to Pack" 🐾** — raising intelligence you don't fully control, from a puppy on a leash (you decide every step) to a wild pack that coordinates with no one writing the steps. Warm and funny on the left of the dial, awe-and-nature-documentary on the right. Details in [`00b-narrative-thread.md`](00b-narrative-thread.md).

## The through-line diagram (repeat at every transition)
```
 DETERMINISTIC ◄───────────────────────────────────────────► AUTONOMOUS
  single  sequential  loop  parallel  conditional | GOAP  planner  supervisor
  @Agent  ───────────── workflows ──────────────  | ──── pure agents ────
       "you decide the path"                        "the model decides the path"
```

## Every example is real code in this repo
- Running toy thread: story writing (`Agents.java`, `WorkflowAgentsIT`, `DeclarativeAgentIT`, `TypedAgentsIT`).
- Supervisor foil: banking transfer (`SupervisorAgentIT`).
- GOAP: horoscope writer (`langchain4j-agentic-patterns/.../goap/horoscope/`).
- Capstone: roadside assistant (`.../carrentalassistant/`).
- Patterns: `langchain4j-agentic-patterns/` (GOAP, BDI, blackboard, debate, voting, P2P).
- Canonical prose reference: `docs/docs/tutorials/agents.md` (the module's own 3,000-line tutorial).

## Notes for the speaker
- Flag once, early: the `langchain4j-agentic` module is **experimental / subject to change**.
- The **AgentMonitor HTML report** is your best visual — show it in §2/§3, again in §6, and as the §9 finale.
- Every section = one git checkpoint you can `git checkout`; keep a known-good tag per section so a live-code slip never strands you.
- Two models on hand (`BASE_MODEL` cheap, `PLANNER_MODEL` strong) — it's also a teaching point (dynamic model selection).
- If time runs short: compress §7's pattern zoo to name-drops; **protect §6 (supervisor) and §7's GOAP** — they're the "aha."
