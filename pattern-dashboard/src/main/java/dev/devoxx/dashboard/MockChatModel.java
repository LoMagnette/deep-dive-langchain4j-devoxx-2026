package dev.devoxx.dashboard;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

/**
 * Deterministic, no-API-key chat model. It inspects the last user prompt and returns a short,
 * PARSEABLE answer so every agentic pattern can run on stage without a real LLM — which is also
 * what {@code mvn test} runs against.
 *
 * <p>The rules are an ORDERED table on purpose. Kennel prompts overlap a lot (three different
 * agents mention "handover sheet"; two mention "vaccination status"), so the specific rule has to
 * be listed before the general one, and putting them in one list makes that ordering visible
 * instead of hiding it in a ladder of ifs. Each rule's comment says what it is standing in front
 * of.
 *
 * <p>The replies are deliberately good demo content rather than filler: run the dashboard with
 * {@code -Ddashboard.model=mock} and the discharge loop really does produce a compliant note, the
 * booking really is declined for an expired rabies booster, and the morning round really does
 * flag a different thing per run. If you change a prompt in {@link Agents}, check the rule that
 * matched it — {@code mvn test} will tell you if nothing does.
 */
public class MockChatModel implements ChatModel {

    private final AtomicInteger scoreCounter = new AtomicInteger();
    private final AtomicInteger lineCounter = new AtomicInteger();
    /** Which step of the supervisor's canned plan we're on (1 = rota, 2 = feed, 3+ = done). */
    private final AtomicInteger plannerStep = new AtomicInteger();

    /** Filler for prompts no rule claims — themed, so an unmatched prompt still looks alive. */
    private static final String[] KENNEL_LINES = {
            "Noted on the board: run 2 checked, water topped up, no change since the last round.",
            "Zao does the evening walk-round with the handler, nose to every door as usual.",
            "Kennel is quiet; the Ardennes rain has all nine dogs curled up and dry.",
            "Logged in the day book and initialled by the handler coming off shift.",
            "Passed to the morning shift with nothing outstanding.",
            "Zao settles by the office door once the last run is bolted for the night."
    };

    @Override
    public ChatResponse chat(ChatRequest request) {
        String text = respond(lastUserText(request));
        return ChatResponse.builder().aiMessage(AiMessage.from(text)).build();
    }

    /**
     * The LAST user message only — plus any system prompt, which carries the agent's role.
     *
     * <p>Concatenating the whole conversation instead would be a bug with teeth: the supervisor's
     * planner is a multi-turn conversation, so round 1's text (including the
     * {@code last received response is: ''} marker that means "nothing has run yet") would stay
     * visible forever, pinning the planner to its first choice and never letting it reach "done".
     */
    private String lastUserText(ChatRequest request) {
        StringBuilder sb = new StringBuilder();
        String lastUser = "";
        for (ChatMessage m : request.messages()) {
            if (m instanceof UserMessage um) {
                lastUser = um.singleText();
            } else if (m instanceof SystemMessage sm) {
                sb.append(' ').append(sm.text());
            }
        }
        return sb.append(' ').append(lastUser).toString();
    }

    /** One canned behaviour: when the lowercased prompt matches, reply from the raw prompt. */
    private record Rule(Predicate<String> when, Function<String, String> reply) {
    }

    private List<Rule> rules() {
        return List.of(
                // --- 0. Supervisor planner. FIRST because its prompt also contains "one of ...
                // agents", which would otherwise trip the routing rule and emit an unparseable
                // word instead of the JSON the planner protocol requires.
                new Rule(p -> p.contains("planner expert") || p.contains("agent invocation"),
                        this::supervisorPlan),

                // --- 1. Anything asking for a number from 0.0 to 1.0 (both loop critics).
                // Alternates 0.60 then 0.95 so a loop visibly runs a second pass and then
                // reliably crosses the 0.8 bar rather than spinning to maxIterations.
                new Rule(p -> p.contains("0.0") || has(p, "score", "rate", "number"),
                        p -> String.format(Locale.US, "%.2f",
                                scoreCounter.getAndIncrement() % 2 == 0 ? 0.60 : 0.95)),

                // --- 2. The placement vote. ONE WORD, identical for all three assessors, so
                // VotingStrategy.majority() has something it can actually tally.
                new Rule(p -> p.contains("place or hold"), p -> "PLACE"),

                // --- 3. The night line's router. Must stay in step with Parsing.CATEGORIES, or
                // the router picks a branch that does not exist. Returns the first desk named in
                // the prompt, which for the shipped call is also the RIGHT desk: emergency.
                new Rule(p -> p.contains("classify this call"), p -> {
                    for (String desk : new String[] {"emergency", "behaviour", "booking"}) {
                        if (has(p, desk)) {
                            return desk;
                        }
                    }
                    return "emergency";
                }),

                // --- 4. Admissions. Both checks say "PASS or FAIL", so the vaccination one is
                // separated by its own word — and it FAILS, which is the point of the demo: the
                // booking is declined for a reason the room can check.
                new Rule(p -> p.contains("pass or fail") && has(p, "vaccination"),
                        p -> "FAIL — the rabies booster expired on 12 June, so it is neither "
                                + "valid nor 21 days clear of arrival. Tablets twice a day are "
                                + "fine for staff to give."),
                new Rule(p -> p.contains("pass or fail"),
                        p -> "PASS — run 4 (large) is free for the whole of 12–19 October, and a "
                                + "40kg shepherd fits a large run."),

                // --- 5. The morning round, one run at a time. Item-aware, so the mapper's
                // gathered watch-list differs per run instead of repeating one line three times.
                new Rule(p -> p.contains("watch-list"), MockChatModel::inspectRun),

                // --- 6. The two intake artefacts. The run sheet is listed FIRST because its
                // prompt also contains the words "boarding record".
                new Rule(p -> p.contains("run sheet"),
                        p -> """
                                Handle alone — do not take him past the runs on the left.

                                07:30  half an antibiotic tablet, given with breakfast
                                08:00  yard, on the lead, on his own
                                17:30  supper, then half an antibiotic tablet with it
                                19:00  last yard visit
                                Collection Tuesday after five."""),
                new Rule(p -> p.contains("boarding record"),
                        p -> """
                                Dog: Nero, German shepherd, 40kg
                                Stay: until Tuesday, collection after 17:00
                                Medication: half an antibiotic tablet morning and night, with food
                                Feeding: not given
                                Flags: reactive to other males — never walk past the left-hand runs"""),

                // --- 7. The discharge loop. The rewrite satisfies all four rules, so the room
                // can hold it against the draft it started from and see what the loop fixed.
                new Rule(p -> p.contains("go-home instructions"),
                        p -> """
                                Give Nero half a white antibiotic tablet with his breakfast and \
                                half with his supper, every day until they run out.

                                Give one painkiller tablet at 08:00 and one at 20:00, only if he \
                                seems sore.

                                Keep him on the lead for a week. No food after 8pm tonight. Look \
                                at his stitches each morning: they should be dry and closed.

                                Call the kennel on 061 22 33 44 if anything worries you."""),

                // --- 8. The week's plans (supervisor sub-agents, reused by the composite).
                // The handover rules come FIRST: that prompt quotes both planners' subject lines.
                new Rule(p -> has(p, "tighten") && p.contains("handover sheet"),
                        p -> """
                                NERO — RUN 2. Do this first: he has not eaten since yesterday, so \
                                telephone the vet on 061 22 33 44 before his 20:00 dose.

                                20:00  half an antibiotic tablet, with food if he will take it
                                22:00  last yard visit, alone, lead on
                                Overnight: check him every two hours and write down what he does.
                                Do not walk him past the left-hand runs."""),
                new Rule(p -> p.contains("handover sheet"),
                        p -> """
                                Nero, run 2 — not eating, retching. Ring the vet before the 20:00 \
                                dose. Antibiotic half tablet at 20:00 with food. Yard alone at \
                                22:00. Two-hourly checks overnight. Never past the left-hand runs."""),
                new Rule(p -> p.contains("exercise and handling"),
                        p -> "Yard alone at 08:00 and 17:00, ten minutes each, lead on. No "
                                + "corridor passes while the other males are out. One quiet "
                                + "grooming session mid-week to keep him handled."),
                new Rule(p -> p.contains("feeding and medication"),
                        p -> "Half an antibiotic tablet with breakfast at 07:30 and with supper "
                                + "at 17:30 — this one goes WITH food. If he refuses two meals in "
                                + "a row, stop and telephone the vet before the next dose."),

                // --- 9. The night line's desks. These are listed AFTER the handover rules
                // and not before, which is not cosmetic: the refinement loop feeds the sheet it
                // just wrote back into the next prompt, so a desk rule matching a word that
                // appears in the SHEET hijacks the second pass of the loop and the composite
                // quietly returns the desk's answer instead of the handover. Keep rules that
                // match on quoted content below the rules that match on an instruction.
                new Rule(p -> p.contains("on-call vet"),
                        p -> "Do not let him eat or drink and do not walk him. A tight, swollen "
                                + "belly with unproductive retching is a suspected bloat: this is "
                                + "a drive-to-the-clinic-now case. Telephone the clinic while you "
                                + "load him and take his paperwork."),
                new Rule(p -> p.contains("behaviour desk"),
                        p -> "Tonight: move him to run 7, away from the barker, and leave the "
                                + "corridor light on low. For the record: third night of "
                                + "disturbed sleep, settles when the neighbour is quiet."),
                new Rule(p -> p.contains("booking desk"),
                        p -> "Those dates are open and a medium run is free. Bring the "
                                + "vaccination card showing rabies and kennel cough at least 21 "
                                + "days old, and any medication in its original box."),

                // --- 10. The quote chain. Ordered narrowest first: the pricer's prompt quotes
                // the allocated run, and the allocator's prompt quotes the vaccination status.
                new Rule(p -> has(p, "cost"),
                        p -> "7 nights × 28 euro (large run) = 196 euro, plus 7 × 5 euro for "
                                + "giving medication = 231 euro total."),
                new Rule(p -> has(p, "allocate"),
                        p -> "Run 3 (large) allocated for the seven nights from 12 October."),
                new Rule(p -> has(p, "vaccination"),
                        p -> "Valid — rabies and kennel cough given 2 September, forty days "
                                + "before arrival."),

                // --- 11. The two peers. The welfare rule is FIRST because its prompt quotes the
                // foreman ("if the foreman's proposal is acceptable"), so the foreman's own rule
                // would otherwise claim it and the two peers would say the same thing. Its reply
                // ends with AGREED, which is what writes 'consensus' and lets P2P exit.
                new Rule(p -> p.contains("welfare officer"),
                        p -> "The spaniels may share, they live together. The barn stalls are "
                                + "not runs and the three males cannot be doubled at all, so two "
                                + "bookings move to the Thursday. Eleven runs, twelve dogs, "
                                + "four-hourly checks on the shared pair. AGREED."),
                new Rule(p -> has(p, "foreman"),
                        p -> "Take all fourteen. I will not turn away a booking I have already "
                                + "confirmed — three of them are regulars and it is the bank "
                                + "holiday. Double up the two spaniels that live together and "
                                + "put the quietest three in the barn stalls."),

                // --- 12. The case board. Three genuinely different KINDS of note, so the board
                // is worth reading; the lead's rule is listed before them because its prompt
                // quotes all three subject lines.
                new Rule(p -> p.contains("lead vet"),
                        p -> """
                                1. Food refusal after the Monday diet change — most likely. \
                                Next check: offer the original food alongside the new one and \
                                weigh what he eats.
                                2. Kennel stress from the barking neighbour. Next check: move him \
                                to run 7 for two nights and see if he eats.
                                3. Early dental or throat pain, despite the normal temperature. \
                                Next check: look in his mouth under sedation if he still refuses \
                                by Thursday."""),
                new Rule(p -> p.contains("medical angle"),
                        p -> "Temperature normal and he is bright, which argues against sepsis "
                                + "or an obstruction. No wound, no vomiting. Next check: weigh "
                                + "him and look in his mouth — dental pain hides behind a normal "
                                + "temperature."),
                new Rule(p -> p.contains("behaviour angle"),
                        p -> "The dog in the next run barks most of the night, and this is his "
                                + "third kennel stay. Dogs that will not eat in a noisy row will "
                                + "often eat in a quiet one. Next check: move him to run 7 for "
                                + "two nights."),
                new Rule(p -> p.contains("feeding angle"),
                        p -> "He came off his usual food on Monday, which is exactly when he "
                                + "stopped eating. Fed at 07:30 and 17:30 by whoever is on the "
                                + "round. Next check: offer the old food beside the new."),

                // --- 13. The morning shift's three desires. Report first: its prompt quotes
                // both round headings.
                new Rule(p -> p.contains("shift report"),
                        p -> "Nine dogs in. Nero (run 2) left his supper and was panting at "
                                + "03:00 — flagged, not medicated, vet telephoned. Luna (run 5) "
                                + "passed no stool overnight — watching. The other seven are on "
                                + "routine and had their 08:00 medication. Next shift: Nero's "
                                + "vet call-back at 11:00, and check Luna again after her walk."),
                new Rule(p -> p.contains("occupied run"),
                        p -> """
                                Run 2 — Nero: left his supper, panting at 03:00. Needs the vet \
                                before anything else this shift.
                                Run 5 — Luna: no stool overnight. Watch, and check again after \
                                her walk.
                                Runs 1, 3, 4, 6, 7, 8, 9: bright, ate up, nothing to report."""),
                new Rule(p -> p.contains("medication round"),
                        p -> "Nero (run 2): held back — flagged unwell, vet called first. Luna "
                                + "(run 5): nothing due. Runs 3, 4, 6 and 8: 08:00 tablets given "
                                + "with breakfast and swallowed. Run 9: ear drops, both ears."),

                // --- 14. The placement council. Narrowest first — the advocates' prompts and
                // the briefer's prompt all contain the word "motion".
                new Rule(p -> p.contains("placement council"),
                        p -> "Motion: place Bruno with Home Two, on condition that a behaviourist "
                                + "visits in the first fortnight to work on the food guarding."),
                new Rule(p -> p.contains("placement panel"),
                        p -> "Bruno goes to Home Two. The fact that decided it: he guards his "
                                + "bowl and has never met a cat, and Home One has two cats and an "
                                + "empty house nine to six. Condition: a behaviourist visit "
                                + "within the first fortnight, and feeding behind a closed door."),
                new Rule(p -> p.contains("restate"),
                        p -> "Ruling: Bruno to Home Two, with a behaviourist visit in the first "
                                + "fortnight and feeding behind a closed door."),
                new Rule(p -> p.contains("one angle"),
                        p -> "On this angle the dossier is clear that Bruno guards his food and "
                                + "has never lived with a cat. What is missing: nobody has "
                                + "recorded how he is with a child at mealtimes."),
                // Both advocates get the identical line, so ConvergenceStrategy.unanimous()
                // (all responses equal) fires after the first round instead of burning both.
                new Rule(p -> has(p, "argue"),
                        p -> "The garden matters less than who is in the house. A dog that guards "
                                + "his bowl needs someone present to manage mealtimes, and a "
                                + "household that has raised two mastiffs already knows what "
                                + "that takes. The absent objection is the cats, and they are "
                                + "not a risk that can be trained away in a fortnight."));
    }

    private String respond(String prompt) {
        // Whitespace is collapsed before matching because the prompts are text blocks: "PASS or
        // FAIL" is one phrase to a reader and "PASS or\nFAIL" to String.contains, so a rule that
        // looks obviously correct silently never fires.
        String p = prompt.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        for (Rule r : rules()) {
            if (r.when().test(p)) {
                return r.reply().apply(prompt);
            }
        }
        return KENNEL_LINES[Math.floorMod(lineCounter.getAndIncrement(), KENNEL_LINES.length)];
    }

    /**
     * The langchain4j PlannerAgent protocol: pick the next agent, or agentName "done" with a
     * "response" argument to finish. We walk both sub-agents and then finish, so the demo shows
     * a supervisor delegating twice rather than looping on one agent.
     */
    private String supervisorPlan(String prompt) {
        boolean firstRound = prompt.toLowerCase(Locale.ROOT)
                .contains("last received response is: ''");
        // Round 1 resets the counter, so every run replays the same three-step plan.
        int step;
        if (firstRound) {
            plannerStep.set(1);
            step = 1;
        } else {
            step = plannerStep.incrementAndGet();
        }
        String req = jsonEscape(between(prompt, "The user request is: '", "'."));
        // Both names are supervisor sub-agents; each takes a single @V("request") argument.
        if (step == 1) {
            return "{\"agentName\":\"RotaPlanner\",\"arguments\":{\"request\":\"" + req + "\"}}";
        }
        if (step == 2) {
            return "{\"agentName\":\"FeedPlanner\",\"arguments\":{\"request\":\"" + req + "\"}}";
        }
        return "{\"agentName\":\"done\",\"arguments\":{\"response\":\"Nero's week is planned: "
                + "solo yard slots away from the other males, and his antibiotic with both "
                + "meals — stop and call the vet if he refuses two in a row.\"}}";
    }

    private static final Pattern RUN_NUMBER = Pattern.compile("run\\s+(\\d+)");
    private static final String[] URGENT = {
            "ate nothing", "left his supper", "left her supper", "no stool", "retch",
            "swollen", "panting", "not eating", "blood"
    };

    /**
     * One line for the shift's watch-list, from one run's notes. It reads the run number and
     * looks for the words the prompt calls urgent, so the mapper's gathered output actually
     * differs per item — three identical lines would hide the whole point of a scatter/gather.
     */
    private static String inspectRun(String prompt) {
        // Only the notes, never the instruction: the prompt itself lists the urgent signs
        // ("treat not eating, ... as urgent"), so matching the whole text flags every run and
        // the gathered watch-list goes back to being three identical lines.
        String full = prompt.toLowerCase(Locale.ROOT);
        int notesAt = full.indexOf("overnight notes:");
        String p = notesAt < 0 ? full : full.substring(notesAt + "overnight notes:".length());
        Matcher m = RUN_NUMBER.matcher(p);
        String run = m.find() ? "Run " + m.group(1) : "This run";
        for (String u : URGENT) {
            if (p.contains(u)) {
                return run + " — urgent: not right overnight. Take his temperature, offer food by "
                        + "hand and telephone the vet before the next medication round.";
            }
        }
        return run + " — no action. Ate up, settled, nothing to pass on.";
    }

    /** Substring between two markers, or a themed fallback if the markers aren't found. */
    private static String between(String text, String start, String end) {
        int i = text.indexOf(start);
        if (i < 0) {
            return "sort out the week for a boarded dog";
        }
        i += start.length();
        int j = text.indexOf(end, i);
        return (j < 0 ? text.substring(i) : text.substring(i, j)).trim();
    }

    /** Minimal JSON string escaping so an extracted request can't break the returned JSON. */
    private static String jsonEscape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", " ").replace("\r", " ").replace("\t", " ");
    }

    /**
     * True if any word appears as a whole word in the (lowercased) text. Word boundaries matter:
     * matching "rate" as a substring fires on "celeb-RATE-s" in a sheet being edited, and
     * matching "allocate" as one fires on "allocated run" in the pricer's prompt.
     */
    private static boolean has(String text, String... words) {
        for (String w : words) {
            if (Pattern.compile("\\b" + Pattern.quote(w) + "\\b").matcher(text).find()) {
                return true;
            }
        }
        return false;
    }
}
