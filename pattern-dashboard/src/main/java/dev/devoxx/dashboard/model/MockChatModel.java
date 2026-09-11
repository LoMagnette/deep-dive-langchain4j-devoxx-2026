package dev.devoxx.dashboard.model;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
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
 * PARSEABLE answer so every pattern can run on stage without a real LLM — which is also what
 * {@code mvn test} runs against.
 *
 * <p>The rules are an ORDERED table on purpose. Prompts overlap a lot (three agents mention the
 * sitter note; the park step quotes the garden step), so the specific rule has to be listed
 * before the general one, and putting them all in one list makes that ordering visible instead of
 * hiding it in a ladder of ifs. Each rule's comment says what it is standing in front of.
 *
 * <p>Three hazards this table already pays for, each of which produced a wrong demo with no error
 * at all:
 * <ul>
 *   <li><b>Whitespace is collapsed before matching.</b> The prompts are text blocks, so
 *       "PASS or FAIL" is one phrase to a reader and {@code "PASS or\nFAIL"} to
 *       {@code String.contains} — a rule that looks obviously right silently never fires.</li>
 *   <li><b>Rules matching quoted content go BELOW rules matching an instruction.</b> A
 *       refinement loop feeds the note it just wrote back into the next prompt, so a rule keyed
 *       on a word that appears in the note hijacks the loop's second pass and the composite
 *       returns the wrong stage's answer.</li>
 *   <li><b>Trigger words must not be ordinary English.</b> The score rule used to fire on the
 *       word "number", which quietly claimed every agent whose rules mention "the vet's
 *       telephone number" — so they answered "0.60" instead of writing a note.</li>
 * </ul>
 *
 * <p>The replies are deliberately good demo content rather than filler: run with
 * {@code -Ddashboard.model=mock} and the picnic-blanket mapper really does clear the cheddar and
 * condemn the grapes, the walk really is vetoed by the hot pavement, and the second-dog vote
 * really does split two to one.
 */
public class MockChatModel implements ChatModel {

    private final AtomicInteger scoreCounter = new AtomicInteger();
    private final AtomicInteger lineCounter = new AtomicInteger();
    /** Which step of the supervisor's canned plan we're on (1 = routine, 2 = training, 3+ = done). */
    private final AtomicInteger plannerStep = new AtomicInteger();

    /** Filler for prompts no rule claims — themed, so an unmatched prompt still looks alive. */
    private static final String[] HOUSE_LINES = {
            "Zao settles in the hall where he can see both doors, one ear up.",
            "Noted, and stuck on the fridge with the others.",
            "Nothing else to change this week — keep everything as boring as possible.",
            "Zao takes this as his cue to bring you the lead, just in case.",
            "Written in the notebook by the back door where you will both see it."
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

                // --- 1. The loop critic. Keyed on "0.0 to 1.0" and NOT on the word "number":
                // three agents' rules mention "the vet's telephone number", and they must write
                // notes, not scores. Alternates 0.60 then 0.95 so a loop visibly runs a second
                // pass and then reliably crosses the 0.8 bar instead of spinning to maxIterations.
                new Rule(p -> p.contains("0.0 to 1.0") || has(p, "score", "rate"),
                        p -> String.format(Locale.US, "%.2f",
                                scoreCounter.getAndIncrement() % 2 == 0 ? 0.60 : 0.95)),

                // --- 2. The second-dog vote. ONE WORD, and deliberately NOT the same word for
                // all three: the money is fine, the flat and Zao are not. That is a real 2-1
                // majority rather than three agents agreeing because they were asked the same
                // thing, which is the only reason to run a vote at all.
                new Rule(p -> p.contains("what a second dog costs"), p -> "YES"),
                new Rule(p -> p.contains("yes or later"), p -> "LATER"),

                // --- 3. The worry router. Must stay in step with Parsing.CATEGORIES, or the
                // router picks a branch that does not exist. Returns the first destination named
                // in the prompt, which for the shipped worry is also the RIGHT one: emergency.
                new Rule(p -> p.contains("classify this worry"), p -> {
                    for (String who : new String[] {"emergency", "training", "everyday"}) {
                        if (has(p, who)) {
                            return who;
                        }
                    }
                    return "emergency";
                }),

                // --- 4. "Walk him now?" Both checks say "PASS or FAIL", so they are separated by
                // what each one is told to judge — and the weather one FAILS, which is the point
                // of the demo: the join vetoes a walk the room already knew was a bad idea.
                new Rule(p -> p.contains("weather and the ground"),
                        p -> "FAIL — 31 degrees and the pavement has been in the sun all "
                                + "afternoon. Press the back of your hand to it for five seconds; "
                                + "if you cannot hold it there, neither can he."),
                new Rule(p -> p.contains("the dog himself for a walk"),
                        p -> "PASS — four years old, sound, and an hour past his meal."),

                // --- 5. The picnic blanket, one item at a time. Item-aware, so the gathered
                // verdicts differ per item — five identical lines would run the pattern perfectly
                // and demonstrate nothing.
                new Rule(p -> p.contains("picnic blanket"), MockChatModel::foodVerdict),

                // --- 6. The sitter note, narrowest first. All three of these prompts talk about
                // notes and cards, and the checklist's prompt quotes the words "sitter card".
                new Rule(p -> p.contains("times of day in order"),
                        p -> """
                                07:30  two scoops, in the tub by the back door
                                08:00  out for a walk, lead on the whole time
                                13:00  quick garden visit
                                18:00  two scoops
                                19:00  last walk of the day
                                Never: the dried liver treats. Never: off the lead in the park."""),
                new Rule(p -> p.contains("sitter card with exactly"),
                        p -> """
                                Dog: Zao, Belgian shepherd
                                Meals: two scoops morning and evening, food in the tub by the back door
                                Walks: not given
                                Watch out for: no dried liver treats; never off the lead in the park
                                Vet: 061 22 33 44"""),

                // --- 7. The refinement loop's rewrite. Satisfies all four rules, so the room can
                // hold it against the note it started from and see what the loop fixed.
                new Rule(p -> p.contains("never met the dog"),
                        p -> """
                                Zao eats twice a day: two scoops at 07:30 and two at 18:00. Food \
                                is in the tub by the back door. No dried liver treats — they \
                                upset him.

                                Walk him at 08:00 and again at 19:00. His lead and the poo bags \
                                are on the hook by the back door. Keep him on the lead in the \
                                park; he will not come back yet.

                                He may cry the first night. He settles.

                                Vet: 061 22 33 44."""),

                // --- 8. The weekend-away composite, narrowest first. The tightener's prompt is
                // the loop's second pass, so it quotes the note it just wrote — see the class
                // note about rules that match quoted content.
                new Rule(p -> p.contains("tighten this note"),
                        p -> """
                                ZAO — Friday to Sunday.

                                07:30  two scoops. 18:00  two scoops. Food: tub by the back door.
                                Walks 08:00 and 19:00. Lead and poo bags: hook by the back door. \
                                Lead stays on — he pulls, plant your feet and wait.
                                Fireworks both nights: curtains shut, radio on, stay in with him. \
                                Do not take him out after dark.

                                Vet 061 22 33 44. Ring us any time."""),
                new Rule(p -> p.contains("goes on the fridge for the dog sitter"),
                        p -> """
                                Fireworks are the thing to plan for: shut the curtains, put the \
                                radio on and keep him in after dark both nights.
                                Meals 07:30 and 18:00, two scoops. Walks 08:00 and 19:00, lead on \
                                throughout. Vet 061 22 33 44."""),
                new Rule(p -> p.contains("meals for the days"),
                        p -> "Two scoops at 07:30 and two at 18:00, from the tub by the back "
                                + "door. Nothing off the table, and no dried liver treats."),
                new Rule(p -> p.contains("walks for the days"),
                        p -> "08:00 for half an hour and 19:00 for twenty minutes, lead on the "
                                + "whole time. Avoid the park after dark while the fireworks are "
                                + "going."),

                // --- 9. The three people a worry can reach. Listed AFTER the composite's rules
                // because the merger's prompt quotes whichever of these answered.
                new Rule(p -> p.contains("emergency vet"),
                        p -> "Ring the practice now and tell them his weight and how much he ate — "
                                + "dark chocolate is the worst kind. Take the wrapper with you so "
                                + "they can read the cocoa percentage. Do not wait to see whether "
                                + "he is sick, and do not try to make him sick yourself."),
                new Rule(p -> p.contains("the dog trainer"),
                        p -> "This week: stop the walk dead every time the lead goes tight, and "
                                + "only move off when it slackens. Stop: yanking him back, which "
                                + "teaches him that pulling is how walks feel."),
                new Rule(p -> p.contains("everyday dog questions"),
                        p -> "Keep it boring and keep it the same: same food, same times, same "
                                + "route. Most of what looks like a problem in week one is just "
                                + "a change of routine."),

                // --- 10. The supervisor's two specialists.
                new Rule(p -> p.contains("daily routine"),
                        p -> "Start now, not in month three: move his bed off your room and into "
                                + "the hall this month, so it is not something the baby did to "
                                + "him. Keep the 08:00 and 19:00 walks exactly as they are — they "
                                + "are the two things that will not change in March."),
                new Rule(p -> p.contains("needs to be taught"),
                        p -> "In this order: a settle on a mat while you are busy in the room; "
                                + "waiting at doorways instead of barging through; and off the "
                                + "furniture on a word. Three months is enough for all three if "
                                + "you start with the mat."),

                // --- 11. Recall in three steps. The park rule is FIRST because the park prompt
                // quotes "garden step already done", and the garden prompt quotes the indoor step.
                new Rule(p -> p.contains("park step"),
                        p -> "At the park, on a fifteen-metre line, when there are dogs in the "
                                + "distance but not near him. The line is there so he can never "
                                + "learn that ignoring you works. Drop it when he has come back "
                                + "ten times out of ten with a dog in sight."),
                new Rule(p -> p.contains("garden step"),
                        p -> "Same word, same reward, now in the garden with the smells and the "
                                + "birds. If he ignores you, do not repeat it — walk to him, take "
                                + "his collar, and make the next one easier."),
                new Rule(p -> p.contains("indoor step for"),
                        p -> "In the hall, two metres away, nothing else going on. Say his name "
                                + "once, then the word, and pay him the moment he turns. Five "
                                + "goes, twice a day. It is working when he turns on the word "
                                + "before he has thought about it."),

                // --- 12. The household argument. The floor rule is FIRST because its prompt
                // quotes the other half's proposal.
                new Rule(p -> p.contains("wants the dog in his own bed"),
                        p -> "I can live with that, but not the whole bed and not every night. "
                                + "House rule: his own bed in our room, and he is invited up in "
                                + "the morning once we are awake — never during the night, and "
                                + "never when he is wet. AGREED."),
                new Rule(p -> p.contains("wants the dog on the bed"),
                        p -> "He has slept up there since he was a puppy and he settles better "
                                + "for it, and so do I. What I will not give up is the mornings — "
                                + "if he has to be off it at night, fine, but he comes up when "
                                + "the alarm goes."),

                // --- 13. The barking board. The trainer's rule is FIRST because its prompt
                // quotes all three contributors' headings.
                new Rule(p -> p.contains("most likely first"),
                        p -> """
                                1. The bed under the front window — most likely. He now has a \
                                job: watching the street all day. Try moving the bed to the back \
                                room and see if it stops within a week.
                                2. The new shift. His day changed shape and nobody told him. Try \
                                a fixed 07:00 walk whatever time you leave.
                                3. Not enough exercise before he is left. Try forty minutes off \
                                the lead before you go, not ten on it."""),
                new Rule(p -> p.contains("exercise angle"),
                        p -> "A four-year-old shepherd needs more than a lead walk round the "
                                + "block, and a bored shepherd invents work. Next: forty minutes "
                                + "of real exercise before he is left, and see what changes."),
                new Rule(p -> p.contains("new working hours"),
                        p -> "The new shift is the change nobody has accounted for — he is left "
                                + "at a different hour, for longer, with no warning cue. Next: "
                                + "keep one thing fixed, the morning walk, whatever your shift."),
                new Rule(p -> p.contains("see and hear from indoors")
                                || p.contains("what he can see and hear"),
                        p -> "His bed was moved under the front window, so he now watches the "
                                + "street, the post and next door's cat all day. Next: move the "
                                + "bed out of sight of the window before you try anything else."),

                // --- 14. The puppy's first hour, three desires.
                new Rule(p -> p.contains("first tiny training session"),
                        p -> "One thing only: his name. Say it once, pay him when he looks, five "
                                + "goes, then stop while he still wants more. Two minutes is a "
                                + "long session for an eight-week-old puppy."),
                new Rule(p -> p.contains("first meal in the new house"),
                        p -> "The amount on the breeder's sheet, not more, in a quiet corner "
                                + "where nobody walks past. Put it down, walk away, and leave him "
                                + "alone with it — do not stroke him or take the bowl to check."),
                new Rule(p -> p.contains("out to the garden first"),
                        p -> "Straight out of the car and onto the grass, before he comes "
                                + "indoors at all. Stand still and say nothing until he goes, "
                                + "then tell him he is wonderful the second he finishes."),

                // --- 14b. The escalation ladder. Each tier decides from the QUESTION which
                // kind it is, so the ladder stops at a different rung depending on what is
                // asked — which is the whole demo. A kibble question stops at the book, a
                // pulling question at the trainer, a limp goes all the way. Three canned
                // ladders, all deterministic, so the early exit can be shown offline by just
                // typing a different question.
                new Rule(p -> p.contains("puppy book on the shelf"),
                        p -> kind(p) == Kind.BASICS
                                ? "Feed a four-year-old shepherd of that size twice a day, a "
                                        + "measured amount from the bag's chart, and weigh him "
                                        + "monthly rather than guessing by eye. ANSWERED"
                                : "There is nothing in here about that — the book only covers "
                                        + "food, kit, grooming and routine. ESCALATE"),
                new Rule(p -> p.contains("trainer, reached by telephone"),
                        p -> kind(p) == Kind.BEHAVIOUR
                                ? "Stop the walk dead the moment the lead goes tight, and only "
                                        + "move off when it slackens — he learns that pulling "
                                        + "makes the walk stop. Ten minutes of that daily beats "
                                        + "an hour of being dragged. ANSWERED"
                                : "That is not a training problem and I would be guessing. If he "
                                        + "is sore or unwell it needs the vet, not me. ESCALATE"),
                new Rule(p -> p.contains("you are the vet"),
                        p -> "Keep him still and off stairs, and do not give him any human "
                                + "painkiller — several are toxic to dogs. A sudden refusal to "
                                + "weight-bear needs examining today, so ring for an appointment "
                                + "this morning. ANSWERED"),

                // --- 15. The council. The chair and the glue are listed before the two
                // advocates, because all three prompts talk about a motion.
                new Rule(p -> p.contains("chair the household council"),
                        p -> "The motion is carried, but not yet. The fact that decided it: Zao "
                                + "stiffens and growls at dogs that come at him, and a flat with "
                                + "no garden gives him nowhere to get away from one. Condition: "
                                + "not before he can meet a strange dog calmly on neutral ground."),
                new Rule(p -> p.contains("restate this ruling"),
                        p -> "A two-bedroom flat with no garden, both owners out eight to six, "
                                + "and a second dog brought in later once Zao can meet other "
                                + "dogs calmly."),
                new Rule(p -> p.contains("write the motion"),
                        p -> "Motion: get a second dog, but not this year — an older, calm "
                                + "female, and only after Zao can meet a strange dog on neutral "
                                + "ground without stiffening."),
                new Rule(p -> p.contains("one angle only"),
                        p -> "On this angle it points one way: the flat is small, the days are "
                                + "long and the dog they have does not enjoy other dogs. What is "
                                + "unknown: whether that is every dog, or just the ones that run "
                                + "straight at him."),
                // The two council advocates answer DIFFERENTLY, so unanimous() does not converge
                // and the debate runs its full two rounds before the chair rules — the opposite
                // of the holiday debate below, which converges in one. Both are worth seeing.
                new Rule(p -> p.contains("argue for this motion"),
                        p -> "A second dog would give him company for the nine hours nobody is "
                                + "home, which is the real problem here. The objection is fair: "
                                + "he does not like strange dogs — which is why the motion says "
                                + "an older calm female, and says later, not now."),
                new Rule(p -> p.contains("argue against this motion"),
                        p -> "Two dogs in a flat with no garden and nobody home for nine hours "
                                + "is two bored dogs instead of one. The point in favour is real "
                                + "— he is lonely — but the answer to a lonely dog is a dog "
                                + "walker, not another dog."),

                // --- 16. The holiday debate. Both advocates get the IDENTICAL line, so
                // ConvergenceStrategy.unanimous() (all responses equal) fires after round one.
                new Rule(p -> p.contains("comes or stays"),
                        p -> "He stays, with the sitter. The fact that decided it: a house with "
                                + "no shade in Tuscany in August is dangerous for a black "
                                + "double-coated shepherd, and the twelve-hour drive is on top of "
                                + "that. Condition: the sitter stays in our house, not hers, and "
                                + "does two overnight trial stays before August."),
                new Rule(p -> has(p, "argue"),
                        p -> "The twelve hours in the car and a house with no shade are the whole "
                                + "argument, and August in Tuscany is not survivable for a black "
                                + "double-coated dog. Two weeks with a sitter he knows costs him "
                                + "a fortnight of missing you; the alternative could cost more."));
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
        return HOUSE_LINES[Math.floorMod(lineCounter.getAndIncrement(), HOUSE_LINES.length)];
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
            return "{\"agentName\":\"RoutinePlanner\",\"arguments\":{\"request\":\"" + req + "\"}}";
        }
        if (step == 2) {
            return "{\"agentName\":\"TrainingPlanner\",\"arguments\":{\"request\":\"" + req
                    + "\"}}";
        }
        return "{\"agentName\":\"done\",\"arguments\":{\"response\":\"Two things to start now: "
                + "move his bed out of your room this month, and teach a settle on a mat. Keep "
                + "the walks exactly as they are.\"}}";
    }

    /** Which rung of the escalation ladder a question belongs on. */
    private enum Kind { BASICS, BEHAVIOUR, MEDICAL }

    private static final String[] MEDICAL_WORDS = {
            "limp", "blood", "bleeding", "vomit", "sick", "swollen", "collapse", "breathing",
            "not eating", "won't eat", "lump", "sore", "hurt", "injur", "poison", "ate a"
    };
    private static final String[] BEHAVIOUR_WORDS = {
            "pull", "bark", "bite", "biting", "growl", "jump", "recall", "come back", "lead",
            "aggress", "scared", "afraid", "anxious", "chew", "destroy", "toilet", "training"
    };

    /**
     * Reads ONLY the question, never the tier's own instructions. This one is worth the comment
     * because the obvious version is wrong in a way tests caught and eyes would not: every tier's
     * prompt explains what is past it ("anything about pain, injury or illness"), so a match
     * against the whole prompt finds "injur" every single time, classifies every question as
     * medical, and the ladder walks to the top no matter what is asked — a planner that looks
     * exactly like a sequence.
     *
     * <p>The question is the last thing in every tier's template, hence {@code lastIndexOf}. A
     * missing marker means the wording in the escalation agents changed: fall back to MEDICAL, so the
     * ladder visibly runs to the top rather than silently answering everything from the book.
     */
    private static Kind kind(String prompt) {
        String lower = prompt.toLowerCase(Locale.ROOT);
        int at = lower.lastIndexOf("question:");
        if (at < 0) {
            return Kind.MEDICAL;
        }
        String q = lower.substring(at + "question:".length());
        for (String w : MEDICAL_WORDS) {
            if (q.contains(w)) {
                return Kind.MEDICAL;
            }
        }
        for (String w : BEHAVIOUR_WORDS) {
            if (q.contains(w)) {
                return Kind.BEHAVIOUR;
            }
        }
        return Kind.BASICS;
    }

    /** What the room already knows, in a table: the dangerous ones and the harmless ones. */
    private static final String[][] FOODS = {
            {"grape,raisin,sultana", "Dangerous — grapes and raisins can shut a dog's kidneys "
                    + "down and there is no known safe amount. Ring the vet now."},
            {"chocolate,cocoa", "Dangerous — and dark is the worst kind. Ring the vet now with "
                    + "his weight and how much he ate; keep the wrapper."},
            {"onion,garlic,leek,shallot", "Dangerous — onions damage red blood cells, and raw is "
                    + "worse. Ring the vet, even if he seems fine today."},
            {"xylitol,sweetener,sugar-free", "Dangerous — xylitol drops a dog's blood sugar "
                    + "within minutes. Ring the vet now."},
            {"macadamia", "Dangerous — macadamias cause weakness and tremors. Ring the vet."},
            {"cheese,cheddar", "Fine — a slice of cheese is fat and salt, nothing worse. Nothing "
                    + "to do."},
            {"bread,crust,toast", "Fine — plain baked bread does nothing. Nothing to do (raw "
                    + "dough would be a different answer)."},
            {"carrot,apple,banana", "Fine — nothing to do. Take the apple core off him though."}
    };

    /**
     * One verdict for one thing off the blanket. Reads ONLY the item, never the instruction: the
     * prompt itself uses the words "dangerous" and "ring the vet", so matching the whole text
     * would give every item the same answer and hide the entire point of a scatter/gather.
     */
    private static String foodVerdict(String prompt) {
        String item = prompt.toLowerCase(Locale.ROOT);
        int at = item.indexOf("he ate:");
        item = at < 0 ? item : item.substring(at + "he ate:".length());
        for (String[] food : FOODS) {
            for (String name : food[0].split(",")) {
                if (item.contains(name)) {
                    return food[1];
                }
            }
        }
        return "Probably nothing, but watch him for a few hours and ring the vet if he is sick "
                + "more than once.";
    }

    /** Substring between two markers, or a themed fallback if the markers aren't found. */
    private static String between(String text, String start, String end) {
        int i = text.indexOf(start);
        if (i < 0) {
            return "get the dog ready for what is coming";
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
     * matching "rate" as a substring fires on "celeb-RATE-s" in a note being edited.
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
