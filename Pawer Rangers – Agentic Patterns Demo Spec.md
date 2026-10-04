# Pawer Rangers – Agentic Patterns Demo Spec

Oct 2, 2026 · @Loïc Magnette

## Overview

The demo teaches 16 agentic patterns in langchain4j through one team of dogs, the Pawer Rangers, led by Zao the Bouvier des Flandres. Each pattern is one mission in the town of Barkville.

- **One cast, many patterns.** The audience meets the Rangers once, then only has to learn the pattern each time.
- **Color = job.** Every Ranger has one color and one obvious job, and the name says it (Sniff finds, Zoom runs, Dig digs).
- **Mission first, code second.** Each pattern opens with the mission in one sentence, then shows the wiring.
- **Rallying cry:** "Paws up, Rangers!" at the start of each mission.

## The cast

Eight Rangers: seven AI agents and one plain Java robot. Zao leads and appears in most missions.

| Color | Ranger | Breed | Job | Tools (gear) | Agent type | Personality |
| --- | --- | --- | --- | --- | --- | --- |
| Black | **Zao** | Bouvier des Flandres | Leader: decides who goes where, names the culprit | Mission board | AI (supervisor, router, planner) | Calm, strong, a Flemish cattle herder. Always comes back with something stuck in his beard |
| Blue | **Sniff** | Beagle | Finds things | `sniff(place)`, `followTrail(scent)` | AI | Nose down, never gives up |
| Yellow | **Zoom** | Greyhound | Runs fast, fetches and delivers | `fetch(item)`, `deliver(item, place)` | AI | Easily distracted by squirrels |
| Green | **Dig** | Dachshund | Tunnels, tight spots | `dig(spot)`, `crawl(tunnel)` | AI | Wants to dig everywhere, including the Mayor's roses |
| White | **Doc** | St. Bernard | Medic, decides what's safe | `checkHealth(animal)`, `isSafe(thing)` | AI | Careful, says no often |
| Purple | **Howl** | Husky | Writes and argues | none | AI | Loud and dramatic |
| Pink | **Fifi** | Poodle | Critic and judge | none | AI | Nothing is ever perfect |
| Silver | **Rivet** | Robot dog | Math and lookups | Java methods only | Non-AI (plain Java) | No brain, never wrong |

**Supporting cast**

- **Officer Jo**: the human who runs Pup HQ. She approves risky actions (human in the loop).
- **Marmalade the cat**: the villain. Argues against the dogs in the debate and is the prime suspect in every crime.
- **The Mayor of Barkville**: loses things, owns a prize rose garden, sends most of the missions.

## Shared world

Two metaphors carry the langchain4j concepts through the whole talk.

**The Pup Board = the AgenticScope.** A big board at Pup HQ where every Ranger pins what they find and reads what others pinned. Each agent's output key is a pin on the board, and the next agent reads it as input.

| Pin (scope key) | Written by | Example value |
| --- | --- | --- |
| `mission` | The Mayor / Officer Jo | "Kitten stuck in the oak tree on Main Street" |
| `location` | Sniff | "Oak tree, 6 m up, north branch" |
| `ladderLength` | Rivet | 7.5 |
| `healthReport` | Doc | "Scared but fine" |
| `article` / `draft` | Howl | Newspaper story or poster text |
| `score` | Fifi | 0.0 to 1.0 |
| `approved` | Officer Jo | true / false |
| `clues` | Sniff, Dig, Rivet | List of clues (blackboard mission) |
| `energy` | Each Ranger | 0 to 100 (custom planner mission) |

**The Mega Mutt = composition.** When Rangers combine, they form a bigger Ranger. In langchain4j, a workflow (sequence, loop, parallel, conditional) is itself an agent, so it can be plugged into a bigger workflow. Use it on slides whenever one pattern is nested in another, for example the Loop mission inside the Sequential mission.

## Missions 1 to 8

Each mission lists the Rangers involved, the flow, what goes on the Pup Board, when it stops, and the one thing to show on stage. Building blocks name the langchain4j-agentic concept to use; check exact method names against the version in your demo.

### 1. Single Agent: The Mayor's Lost Hat

- **Rangers:** Sniff alone.
- **Flow:** The Mayor asks "find my hat". Sniff decides which tools to call (`sniff`, `followTrail`) and in what order.
- **Pup Board:** in `mission` → out `location`.
- **Stops when:** Sniff returns where the hat is.
- **Building block:** a single `@Agent` interface with tools.
- **On stage:** show the tool calls in the log. The LLM chooses the tools, not your code.

### 2. Sequential: Kitten in a Tree

- **Rangers:** Sniff → Zoom → Doc → Howl.
- **Flow:** Sniff finds the kitten, Zoom brings the ladder, Doc checks the kitten, Howl writes the Barkville Gazette story.
- **Pup Board:** `mission` → `location` → `rescueStatus` → `healthReport` → `article`.
- **Stops when:** the last Ranger finishes.
- **Building block:** sequence workflow.
- **On stage:** each Ranger only reads the pin left by the one before.

### 3. Loop: The Town Fair Poster

- **Rangers:** Howl (writer) and Fifi (critic).
- **Flow:** Howl writes the poster, Fifi scores it from 0 to 1 and says what's wrong, Howl rewrites.
- **Pup Board:** `draft`, `score`, `feedback`.
- **Stops when:** `score >= 0.8`, or Howl runs out of treats (max 5 iterations).
- **Building block:** loop workflow with exit condition and max iterations.
- **On stage:** print each draft and score. The treat counter is the max-iterations safety net.

### 4. Parallel: Storm Warning

- **Rangers:** Zoom (bridge), Sniff (forest), Dig (tunnels), then Zao merges.
- **Flow:** the three inspections run at the same time; their reports are merged into one safety report.
- **Pup Board:** `bridgeReport`, `forestReport`, `tunnelReport` → `safetyReport`.
- **Stops when:** all three reports are in.
- **Building block:** parallel workflow with an executor, followed by a merge step.
- **On stage:** show the timing: three checks in the time of one.

### 5. Parallel Mapper: Eight Lost Ducklings

- **Rangers:** the same Sniff search agent, sent 8 times.
- **Flow:** a list of 8 ducklings, each with a last-seen place. One search agent runs once per duckling, all in parallel.
- **Pup Board:** in `ducklings` (list) → out `foundDucklings` (list).
- **Stops when:** every duckling has a result.
- **Building block:** parallel mapper (same agent applied to each item of a list).
- **On stage:** contrast with Mission 4. There: different agents, one input. Here: one agent, many inputs.

### 6. Conditional Routing: The Emergency Phone

- **Rangers:** Zao classifies the call, then one of Sniff, Dig, Doc or Zoom.
- **Flow:** something lost → Sniff. Someone stuck underground → Dig. Someone hurt → Doc. Far away and urgent → Zoom.
- **Pup Board:** `call` → `category` → the chosen Ranger's result.
- **Stops when:** the chosen Ranger finishes.
- **Building block:** a classifier agent, then a conditional workflow.
- **On stage:** send four different calls live and let the audience guess which pup answers.

### 7. Human in the Loop: Digging the Mayor's Roses

- **Rangers:** Dig, Officer Jo (human).
- **Flow:** a hedgehog is trapped under the Mayor's prize roses. Dig plans a tunnel, then asks Officer Jo before digging.
- **Pup Board:** `digPlan` → `approved` → `rescueStatus`.
- **Stops when:** Jo says yes (Dig digs) or no (Dig suggests another way).
- **Building block:** human-in-the-loop agent inside a sequence.
- **On stage:** type the answer in the console live. Say no once to show the fallback.

### 8. Non-AI Agent: Rivet's Ladder Math

- **Rangers:** Rivet.
- **Flow:** Rivet computes the ladder length from the tree height (plain Java, no LLM), then Zoom fetches the right ladder.
- **Pup Board:** in `treeHeight` → out `ladderLength`.
- **Stops when:** the method returns.
- **Building block:** a plain Java class with an `@Agent` method, used in a workflow next to AI agents.
- **On stage:** "Not every dog needs a brain." Drop Rivet into Mission 2 between Sniff and Zoom.

## Missions 9 to 16

From here the patterns are about who decides what happens next: an LLM, a plan, the pups themselves, or your own code.

### 9. Supervisor: Chaos at the Town Fair

- **Rangers:** Zao supervises; Sniff, Zoom, Dig and Doc are available.
- **Flow:** the Mayor reports "the fair is chaos: a lost child, a runaway sausage cart, a hole in the bouncy castle". Zao decides, step by step, who to send next based on each report.
- **Pup Board:** `mission` → each Ranger's report → `fairStatus`.
- **Stops when:** Zao decides the fair is under control.
- **Building block:** supervisor agent with sub-agents.
- **On stage:** run it twice. The order can change, because the LLM decides, not the code.

### 10. GOAP: Marmalade on the Water Tower

- **Rangers:** Zoom, Rivet, Dig, Doc.
- **Flow:** goal = `catSafe`. Each Ranger declares what it needs and what it produces. The planner works backward from the goal: Doc climbs (needs `ladderSecured`) ← Dig steadies the ladder (needs `ladder`) ← Zoom fetches it (needs `ladderLength`) ← Rivet computes it (needs `towerHeight`).
- **Pup Board:** `towerHeight` → `ladderLength` → `ladder` → `ladderSecured` → `catSafe`.
- **Stops when:** the goal pin exists.
- **Building block:** goal-oriented planner; agents chained by their input and output keys.
- **On stage:** register the agents in random order. The planner still finds the right chain.

### 11. Peer-to-Peer: The Corn Maze

- **Rangers:** Sniff and Zoom, no leader.
- **Flow:** the two search a giant corn maze for the Mayor's lost goat. They talk directly over their collars: "I cleared north, you take east." Any pup acts when the board has something new for it.
- **Pup Board:** `clearedAreas`, `goatSighting`.
- **Stops when:** the goat is found.
- **Building block:** peer-to-peer planner (agents trigger each other through shared state).
- **On stage:** point out that no one is in charge, not even Zao.

### 12. Blackboard: The Great Sausage Heist

- **Rangers:** Sniff, Dig, Rivet add clues; Zao concludes.
- **Flow:** the town's sausages are stolen. Each Ranger pins a clue when they can add one: Sniff a scent trail, Dig paw prints in a tunnel, Rivet camera timestamps. When enough clues are up, Zao names the culprit.
- **Pup Board:** `clues` (list) grows → `culprit`.
- **Stops when:** `culprit` is set.
- **Building block:** shared AgenticScope as the board plus a custom planner that picks whoever can add a clue.
- **On stage:** the twist: a sausage crumb is found in Zao's beard. He's innocent. It was Marmalade.

### 13. Voting / Ensemble: Is the Lake Ice Safe?

- **Rangers:** Sniff, Doc, Rivet vote independently.
- **Flow:** each Ranger judges "safe" or "not safe" without seeing the others.
- **Pup Board:** `vote1`, `vote2`, `vote3` → `verdict`.
- **Stops when:** all votes are counted.
- **Building block:** parallel workflow plus an aggregation step.
- **On stage:** two rules. Majority vote for fun choices (naming the new HQ mascot). One "no" is a veto for safety. The aggregation strategy is part of the design.

### 14. Debate: Dog Park vs. Cat Café

- **Rangers:** Howl (for the dog park), Marmalade (for the cat café), Fifi (moderator).
- **Flow:** at the town council, Howl and Marmalade argue for 3 rounds, each answering the other. Fifi reads the transcript and declares a winner with reasons.
- **Pup Board:** `transcript` grows → `verdict`.
- **Stops when:** 3 rounds are done.
- **Building block:** loop workflow over two debaters, then a judge agent.
- **On stage:** let the audience vote before Fifi announces her verdict.

### 15. BDI: Squirrel!

- **Rangers:** Zoom.
- **Flow:** Zoom's head has three parts.
  - **Beliefs:** the bridge is out; a kid is stranded on the other side.
  - **Desires:** rescue the kid, chase that squirrel, nap.
  - **Intention:** commit to the rescue route.

  A squirrel appears (belief update). Zoom re-evaluates but keeps the rescue intention, because it is committed and the kid is still stranded.
- **Pup Board:** `beliefs`, `desires`, `intention`.
- **Stops when:** the intention is achieved or dropped.
- **Building block:** custom planner that loops: update beliefs → pick a desire → commit to an intention → act.
- **On stage:** send a second belief update ("the kid is already safe") to show the intention being dropped.

### 16. Custom Planner: Zao's Nap Schedule

- **Rangers:** Zao's rule, applied to all Rangers.
- **Flow:** plain Java decides the next Ranger from the board: hungry → feed; energy above 70 → go on the next mission; otherwise → nap. Nobody does two missions in a row.
- **Pup Board:** `energy` per Ranger, `lastOnMission`, `missionQueue`.
- **Stops when:** the mission queue is empty or it's bedtime.
- **Building block:** your own Planner implementation.
- **On stage:** show the whole planner on one slide. It's just Java you control.

## Running order and presenter notes

Run the missions in four acts, from code-controlled to LLM-controlled to fully custom.

| Act | Missions | Idea to land |
| --- | --- | --- |
| 1. Meet the team | Cast intro, 1 Single Agent, 8 Non-AI Agent | An agent is a pup with a job; some don't need a brain |
| 2. Workflows (you decide the order) | 2 Sequential, 3 Loop, 4 Parallel, 5 Parallel Mapper, 6 Conditional, 7 Human in the Loop | Rangers combine into the Mega Mutt |
| 3. Planners (the system decides) | 9 Supervisor, 10 GOAP, 11 Peer-to-Peer, 12 Blackboard | Who picks the next pup: Zao, a plan, or the pups themselves |
| 4. Many minds and custom brains | 13 Voting, 14 Debate, 15 BDI, 16 Custom Planner | You can write the rules yourself |

**Presenter notes**

- Introduce the cast once with one slide per Ranger, then only use their color and face as a badge on each mission slide.
- Keep the same slide layout for every mission: mission sentence, Rangers involved, flow picture, code.
- Reuse missions to show composition: put Rivet (Mission 8) into the Kitten in a Tree sequence, and the Poster loop (Mission 3) inside a bigger sequence.
- Running gag: Zao's beard. Reveal something new stuck in it at the end of each act (a twig, a duckling, the sausage crumb).
- "Pawer Rangers" is a nod to Power Rangers. Keep logos, suits and catchphrases your own and let the pun do the work.

**Open questions**

- Which langchain4j version will the demo run on? Confirm the builder names for parallel mapper, GOAP and peer-to-peer before coding those missions.
