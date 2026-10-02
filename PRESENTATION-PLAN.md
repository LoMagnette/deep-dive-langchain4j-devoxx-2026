# Presentation plan — Devoxx deep dive (180 min)

> Draft for review. Every section has the same layout: **Slides · Demo · Live-coded · The one
> deep look · Talking points · Transition · Cut if late.** Times are targets, not promises.
> Mark what you want to keep, move or drop.

## Ground rules for the whole talk

1. **Each demo gets one deep look, and it's a different one each time.** Scope tab, server log,
   timing badge, breaking it, the room voting, live coding… The full code → run → inspect → re-run
   cycle is for the three big moments only (typed keys, supervisor, custom planner). That's what
   keeps 20 demos from turning into one routine repeated 20 times.
2. **Vary the format, never the same one twice in a row:** live code · watch a run together ·
   audience predicts / votes · break it on purpose · slide with a war story.
3. **Code first, then run and watch.** Don't talk over the run. That was right for 45 min and
   wrong for 180 min: the diagram lighting up *is* content.
4. **The plain prompt from §1 is the baseline.** Every pattern after it has to answer "would one
   plain prompt do as well?" Point back to it whenever a pattern looks like overkill.
5. **The dial slide comes back at every transition.** Same picture, the cursor moves.

## Timing overview

| # | Section | Min | Clock | Format of the deep look |
|---|---|---|---|---|
| 0 | Opening: Zao, the dial, the promise | 5 | 0:00 | slides |
| 1 | LangChain4j basics: the baseline | 10 | 0:05 | **live code** |
| 2 | `single`: the first agent | 5 | 0:15 | read the interface, streaming toggle |
| 3 | `sequential` + the AgenticScope | 7 | 0:20 | **Scope tab** |
| 4 | `loop`: generator + critic | 6 | 0:27 | **Server log**, round by round |
| 5 | `parallel` + `parallelMapper` | 8 | 0:33 | **timing badge**, room marks the verdicts |
| 6 | `conditional` + typed keys | 12 | 0:41 | **break it live**, then fix it |
| 7 | `humanApproval`: the brake | 5 | 0:53 | **the room answers** |
| 8 | `nonAiAgent`: the far left | 5 | 0:58 | the node that never lights up |
| 9 | `supervisor`: the pivot | 14 | 1:03 | **the planner's real prompt**, three routes |
| — | **Break** (cliffhanger: "we just gave the LLM the wheel") | 20 | 1:17 | |
| 10 | Custom planner: every builder is a planner | 14 | 1:37 | **live code** |
| 11 | The zoo: GOAP, p2p, blackboard, voting, debate, BDI | 22 | 1:51 | predict · vote · war stories |
| 12 | Observability: listener, logs, monitor | 10 | 2:13 | the dashboard's own plumbing |
| 13 | Running it for real: routing, async, resilience | 13 | 2:23 | **break it** (resilience) |
| 14 | Composites: the builders nest | 10 | 2:36 | watch a whole system run |
| 15 | Take-home + Q&A | 10 | 2:46 | slides |
| | Slack | 4 | 2:56 | |

**Sanity check against yesterday:** the parts you already did in 45 min (demos 1–8 without
the mapper, scope, typed keys, supervisor, GOAP, listener) take about **75 min** here. That's
1.7× slower, and it comes from live-coding the typed keys, watching runs instead of talking over
them, and the supervisor failure stories. If that multiplier feels too high for how you speak, use
the "cut if late" lines in reverse: that's where to stretch.

**Halves:** about 77 min before the break and 79 after. Balanced.

## Practical setup

- **Two windows, one decision:** IDE and dashboard. Pick either full-screen switching (one hotkey,
  rehearsed) or a fixed split (IDE left 60%, dashboard right 40%, dock enlarged). Switching
  clumsily costs more than it saves.
- **Font size:** IDE at presentation size, dashboard zoomed to 125–150%. The dock resize is
  remembered, so set it up before the session starts.
- **Model:** real Ollama. `-Ddashboard.model=mock` running as a fallback on a second port, or
  ready to restart in 20 s. The `run-start` event names the live model, so a silent fallback
  can't fool you on stage.
- **The 15–20 s thinking pause:** always have a question for the room ready ("which desk will it
  pick?"). That turns dead air into a prediction.

---

## 0 · Opening (5 min)

**Slides**
- Title + one photo of Zao (real photo, not clip-art).
- "Who has shipped an LLM feature? Who has shipped an *agent*? Who has debugged one?" Hands up.
- **The dial**: deterministic ◄──► autonomous, with the four groups under it.
- The thesis in one sentence: *start as far left as the problem allows; the pattern you don't
  adopt is the one you don't have to debug.*
- The promise: 21 demos, all running live, all real `AgenticServices` code, nothing hidden
  behind a wrapper. Repo link / QR code.
- One slide flagging it: `langchain4j-agentic 1.20.0-beta30` is **experimental**. You'll see it
  break twice today, on purpose.

**Talking points**
- The story: a Bouvier des Flandres, a household, a weekend away, then everything that goes wrong
  in a dog's life. Every demo is one beat of that story.
- Rule for the demos: you'll know the right answer before the agent does. You can grade every run.

**Transition** → "Before any agents, the thing every agent is made of."

---

## 1 · LangChain4j basics: the baseline (10 min)

**Slides:** 1 slide — the four layers: `ChatModel` → `AiServices` → structured output → tools.
Then the IDE.

**Demo:** none from the dashboard.

**Live-coded** (a standalone file, *not* a catalogue demo, because inserting a `_00_` would
renumber all 21 packages). JBang script or a single class, same Ollama:
1. `model.chat("Zao ate a sock. Should I worry?")` → a wall of prose.
2. An interface with `@SystemMessage` / `@UserMessage` / `{{worry}}`, built with
   `AiServices.create(...)` → same call, now typed.
3. Return a `record Triage(String urgency, String action)` instead of `String` → structured output.
4. A `@Tool String vetPhone()` → the model decides to call Java. Keep it to 2 minutes.

**The one deep look:** the difference between steps 1 and 3. Prose vs a record you can `switch` on.

**Talking points**
- This is the whole of LangChain4j you need for today. An `@Agent` is an AI service with extras.
- Tools = "the model picks the next call." Remember that: it comes back in §9 as the supervisor.
- **This single prompt is the baseline.** Every pattern today has to beat it.
- Skip RAG, memory and embeddings on purpose. They're a different talk.

**Transition** → "Now make it an agent. One annotation, one builder."

**Cut if late:** drop step 4 (tools) and say it in one sentence at §9.

---

## 2 · `single`: the first agent (5 min) — `demos/_01_single`

**Slides:** none. The dashboard gallery once (10 s: "here's the map of the next 3 hours"),
then `#/single`.

**Demo:** default input: the sitter message, no punctuation, no walk time given.

**Live-coded:** nothing. Open `NoteRetriever` and `SinglePattern.run`.

**The one deep look:** the interface next to the one from §1. Almost identical: `@Agent` +
`@K` instead of `@V`. Then flip the **streaming toggle** and run again.

**Talking points**
- `agentBuilder(X.class).chatModel(model).name("X").outputKey(...)`: the most important line in
  the library, written out on purpose. No wrapper.
- `.name("X")` is load-bearing: the default name is the *method* name (`card`), not the interface.
- **Watch the Walks line.** The message never says when to walk him. Does the model write "not
  given" or invent 08:00? The room grades it.
- Streaming: the return type (`TokenStream`) makes an agent stream, not the builder. And only the
  **last** agent of a system can stream to a screen, which is why it's on this demo and no other.

**Transition** → "One agent is §1 with a name. Two agents need somewhere to meet."

**Cut if late:** skip the streaming toggle.

---

## 3 · `sequential` + the AgenticScope (7 min) — `demos/_02_sequential`

**Slides:** 1 slide — the AgenticScope as a whiteboard: agents never call each other, they read
and write keys.

**Demo:** same sitter message. `NoteRetriever` (unchanged) → `FridgeMagnet`.

**Live-coded:** nothing.

**The one deep look:** **the Scope tab**, for the first time. Name / type / value, and the row the
agent just wrote lights up. `Card` is the seam between the two agents.

**Talking points**
- Demo 1's agent, **unchanged**. Only the control around it changed. That's the argument of
  the whole first half.
- Why a second agent and not a longer prompt: it writes for a different reader (someone in your
  kitchen at 07:00).
- The scope is the contract between two agents that never see each other. Hold that thought: it
  becomes the typed-keys section.
- Rigid order; a bad hand-off derails the chain. The second agent trusts `Card` completely.

**Transition** → "It wrote a note. Is it a *good* note? Who decides?"

---

## 4 · `loop`: generator + critic (6 min) — `demos/_03_loop`

**Slides:** none, or one showing the four rules: every meal with time + amount, where the lead
is, the vet's number, short enough for the fridge.

**Demo:** default input: "just feed him twice like normal … ring me if anything's up!! xx". The
room sees all four problems before the run.

**Live-coded:** nothing. Show the `exitCondition` predicate:
`scope -> Parsing.score(scope.readState(...)) >= 0.8`.

**The one deep look:** **the Server log**: the critic's prompt and answer, round by round. `×n`
on the node shows it went round more than once.

**Talking points**
- `FridgeMagnet` unchanged, a third time. A critic and a loop drawn around it.
- **Score against named rules, not "quality out of 1.0".** "3/4" is something the room can check.
- War story: `Parsing.score` took the *first* number, so "4 of 4 rules hold: 1.0" read as 0.4. The
  loop ran to `maxIterations` on a perfect note, with no error. A demo that looked like a critic
  nobody could satisfy.
- Always cap `maxIterations`. A loop has two ways out; draw both.

**Transition** → "So far each step waited for the one before. Some don't need to."

**Cut if late:** drop the `Parsing.score` war story.

---

## 5 · `parallel` + `parallelMapper` (8 min) — `demos/_04_parallel`, `demos/_05_parallelmapper`

**Slides:** none.

**Demo 1 — parallel** (4 min): input is exactly what `single` printed. `ChowHound` and
`LeadDeveloper` fan out, `combine` joins.

**The one deep look:** **the timing badge.** Whole run ~160 ms vs agents busy ~300 ms (mock), or
the real-model equivalent. Point at it and say nothing for three seconds.

**Demo 2 — parallelMapper** (4 min): the beard. Cooked bone, croissant, conker, glove, puddle.

**The one deep look:** **the room marks the run.** Before hitting Run: "hands up, which ones are
dangerous?" Then check the verdicts against the room.

**Talking points**
- Parallel only for truly independent work, and the join is yours to write.
- The gap between the two numbers is the whole case for this pattern.
- The mapper's fan-out width is **data**, decided at run time. The diagram draws it as a stack.
- "He did the scatter, you do the gather."
- Cost: empty a whole beard into the box and it's 50 concurrent calls. Rate limits.

**Transition** → "Everything so far runs every agent. Sometimes you must run exactly one, and
the right one."

**Cut if late:** mapper in 2 min, no audience vote.

---

## 6 · `conditional` + typed keys (12 min) — `demos/_06_conditional`

The first of the three big moments. Full cycle: code → run → break → fix → run.

**Slides:** 2 slides — (a) `@V("Worry")` vs `@K(Worry.class)` side by side; (b) the list of
places that still take a string (`HumanInTheLoopBuilder.inputKey`, the mapper's item). "Typing is
only as good as the narrowest API you touch."

**Demo:** 85% dark chocolate, wagging. `WorryRouter` → one of `EverydayCare` / `DogTrainer` /
`EmergencyVet`.

**Live-coded** (the heart of the section, ~6 min):
1. Run it: goes to the vet. Everyone knew it would.
2. Switch the router's input to `@V("worry")` (lowercase, the natural thing to type). Compile:
   **fine**. Run: the router gets nothing, and the failure shows up **in a different place** from
   where you typed.
3. Fix it by going typed: `record Worry() implements TypedKey<String> {}`, `@K(Worry.class)`.
   Show that a typo is now a compile error.
4. Show `readState(Verdicts.class)` from the mapper: a `List<String>` with no cast.

**The one deep look:** the failure that doesn't happen where the mistake is.

**Talking points**
- Routing: only as good as the classifier. **Choose which way it falls**: the fallback is the vet,
  because that's the mistake you can live with.
- These three desks come back in demos 7, 9, 16 and 17. Learn their names now.
- A key is the contract between two agents. Nothing checks both spellings match. Real losses in
  this repo: `"note"` vs `"notes"`, and `findings` declared `String` while the scope held a `List`.
- Keys are **records**, not interfaces: the framework instantiates one to ask its name.
- `TypedKey.name()` defaults to the record's simple name, so `{{Worry}}` in the prompt is unchanged.
- A typed read returns `null` when absent. It does not fall back to `defaultValue()`.

**Transition** → "It routed to the vet. Now imagine the person acting on the answer does exactly
what it says."

**Cut if late:** drop step 4 (mapper `readState`) and the second slide.

> **Decision needed:** the tree is currently mixed: 9 demo files (in `_02`–`_06` and `_18`) use
> `@V`, the rest use `@K`. Decide whether you present demos 1–5 untyped (and this section is
> where they turn typed) or fully typed (and you break it on purpose here). Commit `31c3967` is
> the full untyped "before".

---

## 7 · `humanApproval`: the brake (5 min) — `demos/_07_humanapproval`

**Slides:** none.

**Demo:** swallowed a sock ("there are eleven"); your sister is the one standing there.

**The one deep look:** **the room answers.** Pick someone in the front row to approve or refuse
out loud; you type it. Then refuse once and show the instruction section changes.

**Talking points**
- Demo 6 exactly, plus one step. `HumanInTheLoop` is a **non-AI agent**: it reads a key and writes
  one back, and the sequence can't tell the answer came from a browser.
- The brake on the dial. It costs what brakes cost: a parked thread and a timeout.
- Ask too often: a form nobody fills in. Too rarely: a rubber stamp. Put it where the action is
  hard to undo.
- Testing a pattern that waits for a person: `AskHuman` is an interface, so in `mvn test` the human
  is a lambda.

**Transition** → "A human is an agent that isn't a model. So is any Java class."

---

## 8 · `nonAiAgent`: the far left (5 min) — `demos/_08_nonaiagent`

**Slides:** 1 slide with the three lines from beta30: `NonAiAgentInstance.setParent` vs
`AgentInvocationHandler:253`. The missing call.

**Demo:** `FlatFile` (Java) → the model writes prose → `Watchdog` (Java, `String::contains`)
checks the numbers survived.

**The one deep look:** **the diagram**: the middle box lights up, the two Java boxes **never do**.
Yet `Facts` appears in the Scope tab and the guard's finding is in the result.

**Talking points**
- Any object with one `@Agent` method goes into `subAgents(...)`. The question isn't "could a
  model do this?" but "is this judgement or a lookup?"
- Nobody should invent a microchip number, and a model asked for one has one.
- `name` goes on the annotation (there's no builder for a POJO), otherwise it's called `lookup`.
- **The listener bug:** a non-AI agent inherits no listener in beta30: no events, no timing. Real,
  and it's the honest face of "experimental". If this goes green on a version bump, they fixed it.
- This is the far-left end of the dial: the model decides nothing.

**Transition** → "That's the whole left side: you decide every path. Now the opposite: nobody
wrote the path down."

---

## 9 · `supervisor`: the pivot (14 min) — `demos/_09_supervisor`

The second big moment. The dial slide, cursor jumps to the far right.

**Slides**
- The dial, jumping right.
- "What's different from routing?" → *the second call exists because of what the first one said.*
- The three attempts (war stories, one slide): (1) trainer that refuses → one call on a live
  model; (2) leftover `supervisorContext` from an old scenario → one problem, stop; (3) the mock
  special-cased the hand-off → all tests green, live demo broken.

**Demo:** a sudden behaviour change. `TriageNurse` → `NEEDS: vet` → `EmergencyVet`.

**Live-coded:** nothing, but read `SupervisorPattern.run` in full: `subAgents(...)`,
`supervisorContext(...)`, `contextGenerationStrategy(CHAT_MEMORY)`,
`maxAgentsInvocations(4)`, `output(...)`.

**The one deep look:** **the Server log**: the prompt `supervisorBuilder()` actually sends, and the
JSON it returns naming the next agent and its arguments. Nobody in the room has seen this before.
Link back to §1: *it's tool calling, one level up.*

Then **re-run twice** with different inputs (the only demo that gets three runs):
- pulling and barking → the trainer
- eating grass → the nurse alone, stop

Before each: "which route?" to the room.

**Talking points**
- **One new agent only**, the nurse. Same three desks. The pivot is a change of *decider*, not
  of cast.
- The result is one answer with its route, not a set of opinions. That's what separates it from a
  fan-out.
- Don't build a hand-off on a refusal. A model follows the positive instruction every time. Give
  an agent whose *job* is to hand on.
- A deterministic mock proves the wiring, never that a real model follows a prompt.
- **The honest question:** look at our `supervisorContext`: "always call the nurse first, then
  whoever she names". If you can write that down… it's nearly a sequence. When is a supervisor
  really worth it?
- Bound the invocations. Non-deterministic: a weaker model sometimes stops after the nurse.

**Transition → break:** "We just gave the LLM the wheel. After the break: how to take some of
it back."

**Cut if late:** one re-run instead of two.

---

## ☕ Break (20 min)

Leave the dashboard on the gallery page. Optional: a slide with "try it yourself" + repo QR code.

---

## 10 · Custom planner: every builder is a planner (14 min) — `demos/_16_customplanner`

The third big moment. The dial slide, cursor back to the middle.

**Slides:** 1 slide — the `Planner` interface: `init(InitPlanningContext)`,
`firstAction` / `nextAction(PlanningContext)` → `call(...)` / `done()`. "Sequential, loop,
supervisor: all implementations of this."

**Live-coded:** `EscalationPlanner` (~40 lines). To stay within time: class shell and imports
pre-typed, **live-code `nextAction`**: read `previousAgentInvocation().output()`, stop on
`ANSWERED`, otherwise call the next rung. Rehearse with a timer; this is the riskiest block.

**Demo:** three runs, cost ladder `PuppyBook` → `TrainerOnCall` → `VetOnCall`:
- limp + €180 out-of-hours → all three rungs
- "which food should I buy?" → stops at the book
- "he pulls like a train on the lead" → stops at the trainer

**The one deep look:** the result line "asked 1 of 3 rungs", read from `agentInvocations()`.

**Talking points**
- Why nothing built-in can express it: sequential runs all three, conditional picks up front,
  loop re-runs the same, supervisor would hand an LLM your cost policy.
- The decision depends on **what came back**. That's what `previousAgentInvocation()` is for.
- Declaration order *is* the cost order. No prompt says "cheapest first".
- You own the loop now: termination, budgets and guard rails are yours.
- Mock war story: the matcher read the whole prompt, found "injur" in every tier's instructions,
  and the ladder always walked to the top. A planner that behaved exactly like a sequence.

**Transition** → "You just wrote a planner. The library ships six more. Let's visit the zoo."

**Cut if late:** type the whole thing from a pre-written clipboard instead of live.

---

## 11 · The zoo (22 min) — `demos/_10` … `_15`

Don't give all six the same time. Two get the deep treatment, the rest are short.

| Demo | Min | The one deep look | Format |
|---|---|---|---|
| **GOAP** `_10_goap` | 6 | registered **backwards** (cyclists, children, hoover); runs hoover → children → cyclists. Diagram: `runs 1st/2nd/3rd` on the planner's arrows | **room predicts the order** before the run |
| **p2p** `_11_p2p` | 4 | propose → counter → sign, 3 turns (the bed) | **war story**: the exit predicate that was always true |
| **blackboard** `_12_blackboard` | 4 | three note-takers in one column, the lead can only go last | read the `@K` keys: *any* could go first |
| **voting** `_13_voting` | 4 | a real 2-1 split: money says YES, the other two LATER | **the room votes first** |
| **debate** `_14_debate` | 3 | Tuscany in August: converges in one round | contrast with the council later (§14) |
| **BDI** `_15_bdi` | 1–2 | priorities, not order: garden > food > training | slide only, flashback: "get the order wrong and you mop" |

**Slides:** one per demo max, and only for p2p (predicate) and BDI.

**Talking points**
- **GOAP:** order derived from declared inputs/outputs. "He comes back indoors. Reliably.
  Indoors." A missing link = goal unreachable, and the failure is *silence*.
- **p2p:** check a predicate in both directions. One that can never be true never ends. One that
  can never be false ends immediately and *silently*. `hasState(Agreement)` was a peer's own output.
  Also: reactive planner, shared key → peers trigger themselves and race.
- **blackboard:** one run shows one order, and so does a sequence. "Any could go first" has to be
  read from the interfaces.
- **voting:** diversity of *criteria* buys robustness, not the same prompt three times. One-word
  answers, because a strategy can only tally answers that can be equal.
- **debate:** the value is forcing the case against the winner to be said out loud. The most
  persuasive agent may beat the most correct one.

**Transition** → "Six planners, and each one hides a failure mode. How do you *see* what they did?"

**Cut if late:** BDI → one sentence; debate → skip, mention at the council.

---

## 12 · Observability (10 min)

Yesterday this was a 2-min detour. Here it's a section.

**Slides:** 1 slide — the three levels: `AgentListener` (agent events) · `ChatModelListener`
(every prompt/answer) · `AgentMonitor` + `HtmlReportGenerator` (built-in report). Optional: an
arrow to OpenTelemetry → Langfuse/Phoenix.

**Demo / code** (the dashboard is the demo: it's built on these):
1. `StreamingListener` implements `AgentListener`, with `inheritedBySubagents() = true`: every
   node animation you've seen for 2 hours is this. Show `beforeAgentInvocation` / `after`.
2. `ChatCallLog` implements `ChatModelListener`: the Server log tab. INFO not DEBUG, because
   the interesting half shouldn't depend on a log level.
3. `AgentMonitor` + `HtmlReportGenerator.generateReport(...)` in `HumanApprovalPattern`: open
   the generated `human-in-the-loop.html`.

**The one deep look:** the generated HTML report, shown for the first time.

**Talking points**
- Timing keyed by `agentId()`, not name: a loop repeats names, ids don't.
- A workflow step is itself reported as an agent (`Sequential`, `Parallel`…), which is how the
  timing badge is the library's own measurement.
- Parallel steps call the listener from several threads: collect thread-safely.
- Reminder of §8: plain-Java agents are invisible to all of this in beta30.
- `Errors`: every failure is "Failed to invoke agent method". Flatten the cause chain or a dead
  Ollama looks like a parse error.

**Transition** → "You can see it. Now make it survive production."

> **Decision needed:** is there a real OTel/Langfuse demo? Nothing in the repo does it today. If
> not, keep it as one slide and don't promise it in the abstract.

---

## 13 · Running it for real (13 min) — `demos/_19` … `_21`

**Slides:** 1 slide — "not where on the dial, what it takes to run it": four one-liners:
`chatModel(Function<AgenticScope,ChatModel>)`, `async(true)`, `optional(true)`,
`errorHandler(...)`.

| Demo | Min | The one deep look |
|---|---|---|
| **modelRouting** `_19` | 4 | wasp sting → strong model; "which food?" → cheap one. Honest line when both tiers are the same model |
| **async** `_20` | 4 | timing badge again, but in a **sequence**: whole run < agents busy. Scope shows `<pending>` |
| **resilience** `_21` | 5 | **break it live**: delete the tablet line from the input → the medication step vanishes, no error. Flaky model recovered by `errorHandler` |

**Talking points**
- **modelRouting:** the model is resolved when the agent is *invoked*. The classifier is now a
  cost decision; cheap-when-it-should-be-strong is the silent expensive mistake. Bias upward.
  Nothing records which model answered: write it down for the bill.
- **async:** the join is the line that **reads** the key. Waiting moves, it doesn't disappear. A
  failure surfaces at the read, far from its cause.
- **resilience:** `optional(true)` is about a missing **input**, not a failing agent. Deciding
  whether you hold a value is plain Java, not a model's job. `RETRY` without a counter = an
  infinite loop.

**Transition** → "Every piece is on the table. Let's build a whole system."

**Cut if late:** modelRouting → slide only.

---

## 14 · Composites: the builders nest (10 min) — `demos/_17`, `_18`

**Slides:** none; the `stages` diagrams do the work.

**Demo 1 — sitterNote** (5 min): Friday to Sunday, fireworks both nights. Router → parallel
planners → merge → refinement loop with the **same four rules** as §4.

**The one deep look:** the import list of `SitterNotePattern`: it *is* the slide. Reused from demos
3, 4 and 6 before a word of explanation.

**Demo 2 — secondDogCouncil** (5 min): mapper → briefer → debate → the voting demo's three
assessors ratify.

**The one deep look:** this debate runs its **full two rounds** (advocates disagree) while §11's
converged in one. Same pattern, two behaviours.

**Talking points**
- Each composite is itself an agent another builder takes as a sub-agent.
- Deterministic scaffolding with LLM judgement at exactly three points. That's the dial, visible.
- **Most of the work is glue:** `CouncilBriefer`, `CouncilNote`. Each pattern expects its input
  under its own key.
- The `worry` / `stay` double-seeding: reusing an agent means accepting the key it already declared.
- Composites fail at the seams: one off-format answer breaks a step that looks unrelated.

**Transition** → "So which one do *you* reach for on Monday?"

**Cut if late:** council → 2 min, just the result.

---

## 15 · Take-home + Q&A (10 min)

**Slides**
- **The flowchart:** Can you write the steps down? → workflow. Can you write the *policy* down?
  → custom planner. Only the goal? → GOAP / supervisor. Need a person? → human-in-the-loop.
- Heuristics, five lines max: start left · name your keys · score against rules · bound every
  loop · assume the mock is lying.
- The dial one last time: "the pattern you don't adopt is the one you don't have to debug."
- Repo + QR code + thanks.

**Q&A:** the remaining time. If the room is quiet, have one seed question: "what would you
*not* use an agent for?"

---

## Open decisions (for you)

- [ ] **Typed vs untyped** for demos 1–5 (see §6). The tree is mixed right now.
- [ ] **The basics file** (§1) doesn't exist yet. JBang or a class in the project?
- [ ] **OTel/Langfuse**: demo or slide only (§12)?
- [ ] **`human-in-the-loop.html`** is untracked and written to the working directory at every
  run: add to `.gitignore`, or commit a sample for §12?
- [ ] **Screen layout**: switching vs split (rehearse either way).
- [ ] **Q&A slots mid-talk**: add 3 min after §9 and after §11 if the room is chatty; take them
  out of the zoo.
- [ ] **Rehearse with a timer**: §6 live code, §9 real-model runs, §10 live code. Those three
  decide whether the second half fits.
