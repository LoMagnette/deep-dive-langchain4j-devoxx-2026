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
import dev.langchain4j.model.chat.listener.ChatModelListener;
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
 * condemn the grapes, the refinement loop really does score 0.60 and then 0.95, and the
 * second-dog vote really does split two to one.
 *
 * <p>A rule whose trigger no prompt contains any more is worse than no rule: it reads as live
 * behaviour, and its comment describes a demo that no longer exists. When a demo's prompts
 * change, delete the rules they stranded — this table has carried orphans from two rewrites.
 */
public class MockChatModel implements ChatModel {

    private final AtomicInteger scoreCounter = new AtomicInteger();
    private final AtomicInteger lineCounter = new AtomicInteger();
    /** How far the supervisor's reactive plan has got. */
    private final AtomicInteger plannerStep = new AtomicInteger();

    /** Filler for prompts no rule claims — themed, so an unmatched prompt still looks alive. */
    private static final String[] HOUSE_LINES = {
            "Zao settles in the hall where he can see both doors, one ear up.",
            "Noted, and stuck on the fridge with the others.",
            "Nothing else to change this week — keep everything as boring as possible.",
            "Zao takes this as his cue to bring you the lead, just in case.",
            "Written in the notebook by the back door where you will both see it."
    };

    private final List<ChatModelListener> listeners;

    public MockChatModel() {
        this(List.of());
    }

    /**
     * With listeners, the offline demo logs its prompts and answers exactly as a real model
     * does — {@code ChatModel.chat()} fires them and then calls {@link #doChat}, so overriding
     * doChat rather than chat is what buys that for free. Tests construct the no-arg version:
     * every mock call would otherwise log, and the interesting failures would be buried.
     */
    public MockChatModel(List<ChatModelListener> listeners) {
        this.listeners = List.copyOf(listeners);
    }

    @Override
    public List<ChatModelListener> listeners() {
        return listeners;
    }

    @Override
    public ChatResponse doChat(ChatRequest request) {
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
                // Classified from the WORRY, not from the destinations listed in the prompt.
                // Matching the prompt returned whichever category was named first — always
                // "emergency", whatever was asked — so offline the router looked like it worked
                // (the shipped worry really is an emergency) while routing everything to the vet.
                // The model-routing demo is what exposed it: every question bought the strong
                // tier. Must stay in step with Parsing.CATEGORIES.
                new Rule(p -> p.contains("classify this worry"), p -> switch (kind(p)) {
                    case MEDICAL -> "emergency";
                    case BEHAVIOUR -> "training";
                    case BASICS -> "everyday";
                }),

                // --- 4. The picnic blanket, one item at a time. Item-aware, so the gathered
                // verdicts differ per item — five identical lines would run the pattern perfectly
                // and demonstrate nothing.
                new Rule(p -> p.contains("picnic blanket"), MockChatModel::foodVerdict),

                // --- 5. The sitter note, narrowest first. All three of these prompts talk about
                // notes and cards, and the checklist's prompt quotes the words "sitter card".
                new Rule(p -> p.contains("times of day in order"),
                        p -> """
                                07:30  two scoops, in the tub by the back door
                                08:00  out for a walk, lead on the whole time
                                13:00  quick garden visit
                                18:00  two scoops
                                19:00  last walk of the day
                                Never: the dried liver treats. Never off the lead in the park.
                                Lead and poo bags: hook by the back door. Vet: 061 22 33 44."""),
                new Rule(p -> p.contains("sitter card with exactly"),
                        p -> """
                                Dog: Zao, Belgian shepherd
                                Meals: two scoops morning and evening, food in the tub by the back door
                                Walks: not given
                                Watch out for: no dried liver treats; never off the lead in the park
                                Vet: 061 22 33 44"""),

                // --- 6. The refinement loop's rewrite. Satisfies all four rules, so the room can
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

                // --- 7. The weekend-away composite, narrowest first. Its refining loop reuses
                // demo 3's FridgeChecklist rather than an agent of its own, so it is claimed by
                // the checklist rule above — there is deliberately no rule of its own here.
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

                // --- 8. The three people a worry can reach. Listed AFTER the composite's rules
                // because the merger's prompt quotes whichever of these answered.
                // Each desk ends with the word the escalation ladder branches on. The vet is the
                // last rung, so it always answers; the other two escalate outside their subject.
                // She always names who is needed, so the hand-off never depends on a refusal.
                new Rule(p -> p.contains("out-of-hours line"), MockChatModel::nurse),

                new Rule(p -> p.contains("emergency vet"),
                        p -> vet(p) + "\nANSWERED"),
                new Rule(p -> p.contains("the dog trainer"), MockChatModel::trainer),
                new Rule(p -> p.contains("everyday dog questions"),
                        p -> everyday(p) + "\n"
                                + (kind(p) == Kind.BASICS ? "ANSWERED" : "ESCALATE")),

                // --- 9. The supervisor's two specialists.
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

                // --- 10. Recall in three steps. The park rule is FIRST because the park prompt
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

                // --- 11. The household argument. The floor rule is FIRST because its prompt
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

                // --- 12. The barking board. The trainer's rule is FIRST because its prompt
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

                // --- 13. The puppy's first hour, three desires.
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

                // Before the desks' own rules: this prompt quotes whichever desk answered.
                new Rule(p -> p.contains("honouring the person's decision"),
                        MockChatModel::finalNote),

                // --- 14. The council. The chair and the glue are listed before the two
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

                // --- 15. The holiday debate, and the rule that ENDS it: "comes or stays" is the
                // JUDGE's prompt (HolidayVerdict), not an advocate's. The advocates fall through
                // to the catch-all below, which hands both sides the same words — and THAT is
                // what makes ConvergenceStrategy.unanimous() (all responses equal) fire after
                // round one.
                //
                // So the catch-all is load-bearing, not a safety net: put a rule between these
                // two that tells the advocates apart and the holiday debate stops converging,
                // runs its full two rounds like the council's, and the page quietly loses the
                // contrast it exists to show. theDebateConvergesOnAgreementAndNotOtherwise
                // is what goes red if that happens.
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
                                + "a fortnight of missing you; the alternative could cost more."),

                // --- 16. Running it for real. Last in the table and safely so: each of these is
                // keyed on an instruction of its own, and no rule above quotes any of them.
                //
                // The out-of-hours DESK, not the out-of-hours LINE — the nurse owns that phrase
                // nine rules up, and the two prompts are one word apart.
                new Rule(p -> p.contains("cover arrangements"),
                        p -> "Mr Devos is on call from 19:00 to 08:00 both nights. Ring 061 22 "
                                + "33 44 as normal and the line diverts to him.\nThe out-of-hours "
                                + "surgery is in Marche, twenty minutes by car — ring before you "
                                + "set off, they do not always have someone on site."),
                // Deliberately DROPS the microchip and the policy number. The note reads
                // perfectly well without them, which is exactly the failure the guard is there
                // to catch — a canned answer that copied everything would make the non-AI step
                // look like ceremony.
                new Rule(p -> p.contains("from the record below"),
                        p -> """
                                Zao is a four-year-old Belgian shepherd, 32 kg.

                                Feed him 300 g twice a day, morning and evening. He is used to \
                                two walks, on the lead throughout.

                                If anything worries you, ring Dr Cluysen on 061 22 33 44 — the \
                                same number works out of hours.

                                Thank you for having him!"""),
                new Rule(p -> p.contains("medication paragraph"),
                        p -> "Half a tablet with his breakfast, every morning, for his hip.\n"
                                + "Push it into a folded slice of cheese and he takes it without "
                                + "noticing.\nIf he spits it out, wait ten minutes and try the "
                                + "other half.\nNever give two to catch up on a missed one."),
                // One agent, three kinds of question — because the point of the demo is that the
                // ANSWER is not what changes between tiers, so it had better be a real answer
                // whichever question is typed in.
                new Rule(p -> p.contains("desk a worried dog owner reaches"), p -> switch (kind(p)) {
                    case MEDICAL -> vet(p);
                    case BEHAVIOUR -> trainer(p);
                    case BASICS -> everyday(p);
                }));
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
    /**
     * The canned supervisor plan — and it is deliberately a <b>reactive</b> one.
     *
     * <p>It does not replay a fixed list. It calls the trainer first, then reads what came back:
     * if the trainer said ESCALATE, it calls the vet, and otherwise it stops. That is the same
     * decision a real planner makes, so the offline demo shows the thing the demo claims — the
     * second call happening <i>because of</i> the first — rather than a script that would look
     * identical if the first agent had said something else.
     *
     * <p>Try it: an input the trainer can answer ends after one call.
     */
    private String supervisorPlan(String prompt) {
        boolean firstRound = prompt.toLowerCase(Locale.ROOT)
                .contains("last received response is: ''");
        String req = jsonEscape(between(prompt, "The user request is: '", "'."));
        if (firstRound) {
            plannerStep.set(1);
            return "{\"agentName\":\"TriageNurse\",\"arguments\":{\"worry\":\"" + req + "\"}}";
        }
        // ONLY the last response, never the whole prompt: the supervisor context spells out
        // every phrase the nurse can use, so scanning the page would match them all.
        String last = between(prompt, "last received response is: '", "'").toLowerCase(Locale.ROOT);
        String needs = last.contains("needs: vet") ? "EmergencyVet"
                : last.contains("needs: trainer") ? "DogTrainer"
                : last.contains("needs: everyday") ? "EverydayCare"
                : null;
        if (needs != null && plannerStep.incrementAndGet() == 2) {
            return "{\"agentName\":\"" + needs + "\",\"arguments\":{\"worry\":\"" + req
                    + "\"}}";
        }
        return "{\"agentName\":\"done\",\"arguments\":{\"response\":\"The nurse named who it "
                + "needed and they have answered.\"}}";
    }

    /**
     * The instruction, after a person has had their say. Reads only what they said — and note
     * that the reply lambda is handed the RAW prompt, not the lowercased one the rule matched
     * on, so {@code indexOf("what they said:")} against raw text returns -1.
     */
    private static String finalNote(String prompt) {
        String all = prompt.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        int at = all.lastIndexOf("what they said:");
        String said = at < 0 ? "" : all.substring(at + "what they said:".length());
        boolean refused = said.contains("no ") || said.startsWith(" no")
                || said.contains("nothing") || said.contains("don't") || said.contains("refuse")
                || said.contains("wait");
        if (refused) {
            return "Do not act on it. Sit with him, keep him where you can see him, and ring us "
                    + "— we will decide and ring the vet ourselves if it comes to that.";
        }
        boolean changed = said.contains("but") || said.contains("also") || said.contains("add")
                || said.contains("instead");
        return "Ring the practice now, tell them his weight and how much he ate, and take the "
                + "wrapper with you."
                + (changed ? " And do exactly what they added: " + said.trim() : "")
                + " Do not wait to see whether he is sick.";
    }



    /** What the nurse makes of the call, and who she says it needs. */
    private static String nurse(String prompt) {
        String q = worry(prompt);
        if (q.contains("snap") || q.contains("growl") || q.contains("grumpy")) {
            return "A dog who has never done this before and now does is the one that worries "
                    + "me. In a four-year-old that is pain until somebody rules it out — teeth "
                    + "and ears first, then hips and back.\nNEEDS: vet";
        }
        if (has(q, "ear", "ears") || q.contains("limp") || q.contains("chocolate")
                || q.contains("blood") || q.contains("swollen")) {
            return "That is physical and it is not going to wait until Monday.\nNEEDS: vet";
        }
        if (q.contains("pull") || q.contains("bark") || q.contains("postman")
                || q.contains("lunging")) {
            return "Nothing here sounds like pain — he is well in himself and this is about what "
                    + "he has learned to do.\nNEEDS: trainer";
        }
        if (q.contains("food") || q.contains("switch") || q.contains("groom")) {
            return "Ordinary stuff, nothing urgent in it.\nNEEDS: everyday care";
        }
        return "He is bright, eating, and nothing about this needs anybody tonight. Ring us in "
                + "the morning if it has not settled.\nNEEDS: nobody";
    }

    /**
     * The three desks answer what they were actually ASKED, not one canned line each.
     *
     * <p>Mostly {@code contains}, because the keywords are stems and the worry says "snapping"
     * rather than "snap". The exception is "ear", which goes through {@link #has}: it is a
     * substring of "near". Stems need prefix matching, short words need boundaries.
     *
     * <p>That matters because the same three agents serve four demos now: routing sends one
     * worry to one of them, the supervisor puts a three-part message to all three, and the
     * escalation ladder walks them in order. A fixed reply per desk made the supervisor's
     * roll-call read "called 3 of 3" over three answers about the wrong problems.
     */
    private static String vet(String prompt) {
        String q = worry(prompt);
        if (q.contains("chocolate") || q.contains("ate a") || q.contains("poison")) {
            return "Ring the practice now and tell them his weight and how much he ate — dark "
                    + "chocolate is the worst kind. Take the wrapper so they can read the cocoa "
                    + "percentage. Do not wait to see whether he is sick.";
        }
        if (has(q, "ear", "ears")) {
            return "That is an infection until a vet says otherwise, and a smell means it has "
                    + "been going a while. Book today, do not poke anything down there, and stop "
                    + "him scratching it open — a buster collar tonight if you have one.";
        }
        if (q.contains("snap") || q.contains("growl") || q.contains("grumpy")) {
            return "The nurse is right to send him. A dog that snaps where he never used to is "
                    + "telling you something hurts, and at four the usual suspects are teeth and "
                    + "ears. Book a full examination — mouth, ears, hips, spine — and keep the "
                    + "children away from his bed entirely until he has been seen.";
        }
        if (q.contains("limp") || q.contains("sore")) {
            return "Keep him still and off stairs, and give him nothing from your own cupboard. "
                    + "A dog that will not weight-bear needs examining today.";
        }
        return "Nothing here needs me tonight, but ring the practice in the morning if it has "
                + "not settled.";
    }

    private static String trainer(String prompt) {
        String q = worry(prompt);
        // The one every trainer knows: a behaviour that appeared out of nowhere is a medical
        // question until somebody rules it out. This is what makes the supervisor call a second
        // agent — not a script, but what the first agent said.
        if ((q.contains("snap") || q.contains("growl") || q.contains("grumpy"))
                && (q.contains("never") || q.contains("suddenly") || q.contains("started"))) {
            return "I will not train this yet, and you should not either. A dog that has never "
                    + "snapped and now does has usually started hurting somewhere — teeth, ears, "
                    + "hips, back. Training a dog out of telling you it is in pain is how you get "
                    + "a dog that bites without warning first. Get him examined, then call me.\n"
                    + "ESCALATE";
        }
        if (q.contains("postman") || q.contains("letterbox") || q.contains("lunging")) {
            return "Block the hallway so he cannot reach the door, and feed him something good "
                    + "the moment the post lands — he learns the noise pays. Never let him "
                    + "rehearse the lunge; every time he does it, it works, because the postman "
                    + "always leaves.";
        }
        if (q.contains("pull") || q.contains("lead")) {
            return "Stop the walk dead every time the lead goes tight, and only move off when it "
                    + "slackens. Stop yanking him back, which teaches him that pulling is how "
                    + "walks feel.\n"
                + (kind(prompt) == Kind.BEHAVIOUR ? "ANSWERED" : "ESCALATE");
        }
        if (q.contains("bark")) {
            return "Find out what he is barking at before you train anything — the answer is "
                    + "usually a window he should not be able to see out of.\n"
                + (kind(prompt) == Kind.BEHAVIOUR ? "ANSWERED" : "ESCALATE");
        }
        return "Nothing here is a training problem.\n"
                + (kind(prompt) == Kind.BEHAVIOUR ? "ANSWERED" : "ESCALATE");
    }

    private static String everyday(String prompt) {
        String q = worry(prompt);
        if (q.contains("food") || q.contains("switch") || q.contains("puppy food")) {
            return "Move him onto an adult food of the same brand over a week: a quarter new on "
                    + "day one, half by day three, all of it by day seven. Switching in one go is "
                    + "what upsets stomachs, not the food itself.";
        }
        if (q.contains("groom") || q.contains("brush") || q.contains("coat")) {
            return "Twice a week normally, daily while he is dropping coat, and do it somewhere "
                    + "you do not mind hoovering.";
        }
        return "Keep it boring and keep it the same: same food, same times, same route.";
    }

    /**
     * Just what was asked, never the agent's own instructions — see {@link #kind}.
     *
     * <p>Every label a prompt uses for its input must be listed here. A missing one makes that
     * agent fall through to its catch-all answer, with no error anywhere.
     */
    private static final List<String> ASKED_LABELS =
            List.of("worry:", "question:", "the call:");

    private static String worry(String prompt) {
        String lower = prompt.toLowerCase(Locale.ROOT);
        int at = ASKED_LABELS.stream().mapToInt(lower::lastIndexOf).max().orElse(-1);
        return at < 0 ? "" : lower.substring(lower.indexOf(':', at) + 1);
    }

    /** Which rung of the escalation ladder a question belongs on. */
    private enum Kind { BASICS, BEHAVIOUR, MEDICAL }

    private static final String[] MEDICAL_WORDS = {
            "limp", "blood", "bleeding", "vomit", "sick", "swollen", "collapse", "breathing",
            "not eating", "won't eat", "lump", "sore", "hurt", "injur", "poison", "ate a",
            // "eaten a whole bar of dark chocolate" matches none of the above: "eaten a" is not
            // "ate a". The catalogue's most-used worry was classifying as BASICS.
            "chocolate"
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
        // The escalation tiers say "Question:", the desks say "Worry:" — same idea, and the
        // ladder now runs on the desks, so both have to be found.
        int at = Math.max(lower.lastIndexOf("question:"), lower.lastIndexOf("worry:"));
        if (at < 0) {
            return Kind.MEDICAL;
        }
        String q = lower.substring(lower.indexOf(':', at) + 1);
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
