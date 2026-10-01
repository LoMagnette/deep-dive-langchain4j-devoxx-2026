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

                // --- 1. The loop critic. Keyed on "0.0 to 1.0", NOT on "number": three agents'
                // rules mention the vet's telephone number. 0.60 then 0.95, so a loop iterates
                // once and then crosses the 0.8 bar.
                new Rule(p -> p.contains("0.0 to 1.0") || has(p, "score", "rate"),
                        p -> String.format(Locale.US, "%.2f",
                                scoreCounter.getAndIncrement() % 2 == 0 ? 0.60 : 0.95)),

                // --- 2. The puppy vote. One word, and deliberately NOT the same word for
                // all three — a real 2-1 majority rather than three agents agreeing.
                new Rule(p -> p.contains("judge only the food"), p -> "YES"),
                new Rule(p -> p.contains("yes or later"), p -> "LATER"),

                // --- 3. The worry router. Must stay in step with Parsing.CATEGORIES, or it
                // picks a branch that does not exist. Classified from the WORRY, never from the
                // destinations the prompt lists — matching those routes everything to the vet,
                // which looks right offline because the shipped worry really is an emergency.
                new Rule(p -> p.contains("classify this worry"), p -> switch (kind(p)) {
                    case MEDICAL -> "emergency";
                    case BEHAVIOUR -> "training";
                    case BASICS -> "everyday";
                }),

                // --- 4. The beard, one item at a time. Item-aware, so the gathered verdicts
                // differ per item — five identical lines would run the pattern perfectly and
                // demonstrate nothing.
                new Rule(p -> p.contains("out of the beard"), MockChatModel::beardVerdict),

                // --- 5. Operation Squirrel, narrowest first. Both prompts talk about what we
                // know, and the battle plan's prompt quotes the card it was handed.
                new Rule(p -> p.contains("battle plan for the back door"),
                        p -> """
                                Beagle: under the bird feeder, silent until it lands. Then bark.
                                Labrador: guards the bait from two metres. Does not eat the bait.
                                Greyhound: under the oak, leads the chase when it comes down.
                                Corgi: the fence line, on our side of it.
                                Dachshund: by the shed, above ground. No digging.
                                Zao: at the back door. Calls it.
                                Nobody goes over the fence. The cat is not a target."""),
                new Rule(p -> p.contains("target card with exactly"),
                        p -> """
                                Target: the grey squirrel with a bit missing off its tail
                                Where: the big oak by the back fence, and the bird feeder
                                Time: not given
                                Route: down the oak, along the top of the fence, to the bird feeder, back up the oak
                                Watch out for: the cat on the shed roof"""),

                // --- 7. The operation end to end, narrowest first. Its refining loop reuses
                // demo 3's BattlePlanner rather than an agent of its own, so it is claimed by
                // the battle-plan rule above — there is deliberately no rule of its own here.
                new Rule(p -> p.contains("write the operation order"),
                        p -> """
                                The Dachshund stays above ground: he gets his own digging spot by \
                                the shed, and he leaves the fence alone.
                                Bait: peanut butter on the bird feeder, guarded by the Labrador.
                                Chase: the Greyhound under the oak, the Beagle on lookout by the \
                                feeder. Nobody goes over the fence."""),
                new Rule(p -> p.contains("plan the bait for the squirrel trap"),
                        p -> "A smear of peanut butter on the bird feeder, and half a croissant "
                                + "at the foot of the oak. The Labrador guards it from two metres "
                                + "back. He does not eat it. He has been told twice."),
                new Rule(p -> p.contains("plan the lookout and the chase"),
                        p -> "The Beagle watches the oak from under the bird feeder. The Corgi "
                                + "takes the fence line, on our side of it. The Greyhound leads "
                                + "the chase the moment it touches the ground. Nobody goes over "
                                + "the fence, and the cat is not a target."),

                // --- 8. The three desks, AFTER the composite's rules because the merger's
                // prompt quotes whichever of them answered. Each ends with the word the
                // escalation ladder branches on; the rescue dog is the last rung, so it always
                // answers.
                new Rule(p -> p.contains("you sniff every problem first"), MockChatModel::firstSniff),

                new Rule(p -> p.contains("the pack's rescue dog"),
                        p -> vet(p) + "\nANSWERED"),
                new Rule(p -> p.contains("who trains the rest of the pack"),
                        MockChatModel::trainer),
                new Rule(p -> p.contains("answer the everyday questions"),
                        p -> everyday(p) + "\n"
                                + (kind(p) == Kind.BASICS ? "ANSWERED" : "ESCALATE")),

                // --- 10. The sausage heist, in three steps. The Corgi's rule is FIRST because
                // its prompt quotes the chair step's answer, and the chair prompt quotes the
                // decoy's. Each is keyed on its own step's name, which no answer repeats.
                new Rule(p -> p.contains("final step of the heist"),
                        p -> "Chair, seat, counter — three hops, and the third one is a stretch. "
                                + "Sausage in the mouth, down in one, and it is in Zao's bowl "
                                + "before the howling stops. The human comes back to a chair "
                                + "against the counter, an empty plate and four dogs asleep."),
                new Rule(p -> p.contains("the chair step of the heist"),
                        p -> "Head down, shoulder against the leg, push. It is louder than I "
                                + "would like, but the Beagle is louder. The chair ends up flush "
                                + "against the counter, right under the plate."),
                new Rule(p -> p.contains("the decoy step of the heist"),
                        p -> "I go to the front door and howl like there is a parcel outside "
                                + "and a stranger holding it. The human goes to look. The kitchen "
                                + "is empty the moment I hear the front door open."),

                // --- 11. The sofa. Each peer reads only the OTHER's key, so a rule has to tell
                // its OWN opening turn from its second one. The Greyhound does that by looking
                // for the Labrador's counter in the draft it was handed; without that branch it
                // says the same thing twice, nobody ever writes AGREED, and the negotiation runs
                // to the ten-round cap.
                //
                // Note the lowercase() inside the reply: the lambda is handed the RAW prompt,
                // not the lowercased text the rule matched on. And "corner cushion" is in the
                // Labrador's own prompt, never the Greyhound's, so only the counter can carry it.
                new Rule(p -> p.contains("you want the whole sofa"),
                        p -> p.toLowerCase(Locale.ROOT).contains("i get the corner cushion")
                                ? "Fine. I keep the long end and stretch out as far as I like, "
                                + "the corner cushion is yours, and the middle cushion is "
                                + "neutral ground — no legs across it. AGREED."
                                : "I sleep twenty hours a day and I am mostly legs. Proposal: "
                                + "the sofa is mine, all of it, and the Labrador has the rug, "
                                + "which is a very nice rug."),
                new Rule(p -> p.contains("you want a place on the sofa"),
                        p -> "The rug is where crumbs go to die. Counter-proposal: you keep the "
                                + "long end; I get the corner cushion, the one with the crisps "
                                + "down the back — and nobody's legs cross the middle cushion."),

                // --- 12. The cake. Zao's rule is FIRST because his prompt quotes all three
                // investigators' notes.
                new Rule(p -> p.contains("most guilty first"),
                        p -> """
                                1. The Dachshund — guilty. The crumbs go out through a flap only \
                                he fits through, and stop at a very round dog asleep on the lawn.
                                2. The Labrador — accessory after the fact. The cream is from \
                                the plate, which he licked clean once the cake had already gone.
                                Sentence: no birthday cake for either of them, ever. Neither of \
                                them is sorry."""),
                new Rule(p -> p.contains("add the trail"),
                        p -> "Crumbs from the coffee table to the dog flap, out across the lawn, "
                                + "and they stop at the Dachshund. The trail goes nowhere near the "
                                + "Labrador's bed."),
                new Rule(p -> p.contains("add the alibis"),
                        p -> "Whoever took the cake took it out through the dog flap, and the "
                                + "Labrador does not fit through the dog flap — he has tried, "
                                + "and it took two humans to get him out. The Dachshund fits."),
                new Rule(p -> p.contains("add the scene"),
                        p -> "Cream on the Labrador's nose, and the plate licked perfectly "
                                + "clean — not a crumb on it. That is somebody finishing a job, "
                                + "not starting one."),

                // --- 13. The puppy's first hour, three desires.
                new Rule(p -> p.contains("first tiny training session"),
                        p -> "One thing only: his name. The pack says it once, and the Labrador "
                                + "drops a piece of kibble the moment he looks. Five goes, then stop "
                                + "while he still wants more. Two minutes is a long session for an "
                                + "eight-week-old puppy."),
                new Rule(p -> p.contains("first meal in the new house"),
                        p -> "The amount on the breeder's sheet, not more, in a quiet corner "
                                + "where nobody walks past. Then the whole pack walks away and "
                                + "leaves him alone with it — me especially. I do not check the "
                                + "bowl. I do not check the bowl."),
                new Rule(p -> p.contains("out to the garden first"),
                        p -> "Straight out of the car and onto the grass, before he comes "
                                + "indoors at all. I stand by the spot I use and say nothing until "
                                + "he goes, then I tell him he is wonderful the second he finishes."),

                // Before the desks' own rules: this prompt quotes whichever desk answered.
                new Rule(p -> p.contains("honouring the human's decision"),
                        MockChatModel::finalNote),

                // --- 14. The puppy council. The chair and the glue are listed before the two
                // advocates, because all three prompts talk about a motion.
                new Rule(p -> p.contains("chair the pack council"),
                        p -> "The motion is carried, but not yet. The fact that decided it: Zao "
                                + "stiffens and growls at dogs that come at him, and a sofa that "
                                + "already holds two dogs gives him nowhere to get away from one. "
                                + "Condition: not before he can meet a strange dog calmly on "
                                + "neutral ground."),
                new Rule(p -> p.contains("restate this ruling"),
                        p -> "The same pack and the same full sofa, with a calm puppy brought "
                                + "in later, once Zao can meet other dogs calmly."),
                new Rule(p -> p.contains("write the motion"),
                        p -> "Motion: take in a puppy, but not this year — a calm one, and only "
                                + "after Zao can meet a strange dog on neutral ground without "
                                + "stiffening."),
                new Rule(p -> p.contains("one angle only"),
                        p -> "On this angle it points one way: the sofa is full, the food is "
                                + "fine and the dog in charge does not enjoy other dogs. What is "
                                + "unknown: whether that is every dog, or just the ones that run "
                                + "straight at him."),
                // The two council advocates answer DIFFERENTLY, so unanimous() does not converge
                // and the debate runs its full two rounds before the chair rules — the opposite
                // of the holiday debate below, which converges in one. Both are worth seeing.
                new Rule(p -> p.contains("argue for this motion"),
                        p -> "A puppy would give the pack somebody new to teach, and me somebody "
                                + "shorter than I am, which is the real point here. The objection "
                                + "is fair: Zao does not like strange dogs — which is why the "
                                + "motion says a calm one, and says later, not now."),
                new Rule(p -> p.contains("argue against this motion"),
                        p -> "A puppy on a sofa that already holds a Greyhound and a Labrador is "
                                + "a fight about cushions every evening. The point in favour is "
                                + "real — the pack would grow — but the answer to a pack that "
                                + "wants company is more garden, not another dog."),

                // --- 15. The holiday debate. "comes or stays" is the JUDGE's prompt; the two
                // advocates fall through to the catch-all, which hands them the same words and
                // is what makes unanimous() converge after round one. The catch-all is
                // load-bearing: a rule between these two that tells the advocates apart kills
                // the contrast with the council's debate, and turns that test red.
                new Rule(p -> p.contains("comes or stays"),
                        p -> "He stays, at the kennels. The fact that decided it: a house with "
                                + "no shade in Tuscany in August is dangerous for a black "
                                + "double-coated bouvier, and the twelve-hour drive is on top of "
                                + "that. Condition: two trial nights at the kennels before "
                                + "August, so he knows the place before he is left there."),
                new Rule(p -> has(p, "argue"),
                        p -> "The twelve hours in the car and a house with no shade are the whole "
                                + "argument, and August in Tuscany is not survivable for a black "
                                + "double-coated dog. Two weeks at kennels he knows costs him "
                                + "a fortnight of missing the human; the alternative could cost "
                                + "more."),

                // --- 16. Running it for real. Last and safely so: each is keyed on an
                // instruction no rule above quotes.
                new Rule(p -> p.contains("walked the whole garden fence"),
                        p -> "Sound all the way round, except behind the shed: there is a gap "
                                + "under the third panel exactly the width of a Dachshund.\nNobody "
                                + "goes behind the shed until the human is back. Especially the "
                                + "Dachshund."),
                // Deliberately DROPS the sausage and the remote's exact spots, so the cat has
                // something to catch. Copy them here and that step becomes ceremony.
                new Rule(p -> p.contains("from the cat's diary below"),
                        p -> """
                                The TV remote first — the humans asked for it. Dachshund: the \
                                sandpit, and dig gently.

                                Then the sausage, Beagle: somewhere in the left flowerbed.

                                Then the sock, Zao: under the rosemary, 30 cm down.

                                Labrador: no digging for you. You know what you did."""),
                new Rule(p -> p.contains("first-aid paragraph"),
                        p -> "Corgi: a scraped nose bleeds more than it hurts.\nLick nothing, rub "
                                + "nothing — lie still with your nose on the cool kitchen tiles.\n"
                                + "Still bleeding when the human gets home, or he will not let "
                                + "anyone near it: wake the human for the vet.\nNobody goes back "
                                + "to that fence today."),
                // One agent, three kinds of question — because the point of the demo is that the
                // ANSWER is not what changes between tiers, so it had better be a real answer
                // whichever question is typed in.
                new Rule(p -> p.contains("the one the pack comes to with a worry"), p -> switch (kind(p)) {
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
     */
    /**
     * The argument the Beagle and the three desks all take. A planner's JSON names the agent's
     * parameter, so this is {@code demos._06_conditional.Keys.Worry} spelled out — the mock cannot
     * import it without making the offline model depend on the demos, so it is named here
     * instead of hidden inside two string concatenations.
     */
    private static final String WORRY_ARG = "Worry";

    private String supervisorPlan(String prompt) {
        boolean firstRound = prompt.toLowerCase(Locale.ROOT)
                .contains("last received response is: ''");
        String req = jsonEscape(between(prompt, "The user request is: '", "'."));
        if (firstRound) {
            plannerStep.set(1);
            return "{\"agentName\":\"FirstSniff\",\"arguments\":{\"" + WORRY_ARG + "\":\""
                    + req + "\"}}";
        }
        // ONLY the last response, never the whole prompt: the supervisor context spells out
        // every phrase the Beagle can use, so scanning the page would match them all.
        String last = between(prompt, "last received response is: '", "'").toLowerCase(Locale.ROOT);
        String needs = last.contains("needs: vet") ? "RescueDog"
                : last.contains("needs: trainer") ? "DogTrainer"
                : last.contains("needs: everyday") ? "EverydayCare"
                : null;
        if (needs != null && plannerStep.incrementAndGet() == 2) {
            return "{\"agentName\":\"" + needs + "\",\"arguments\":{\"" + WORRY_ARG + "\":\""
                    + req + "\"}}";
        }
        return "{\"agentName\":\"done\",\"arguments\":{\"response\":\"The Beagle named who it "
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
            return "Do nothing yet. Lie next to him, keep him where the whole pack can see him, "
                    + "and wait by the door — the human is coming home and will decide.";
        }
        boolean changed = said.contains("but") || said.contains("also") || said.contains("add")
                || said.contains("instead");
        return "Keep him still and give him nothing to eat or drink. Be at the door when the "
                + "human gets home: they are taking him straight to the vet."
                + (changed ? " And do exactly what they added: " + said.trim() : "")
                + " Nobody tries to make him sick.";
    }



    /** What the Beagle makes of the problem, and which dog she says it needs. */
    private static String firstSniff(String prompt) {
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
            return "Nothing here smells like pain — he is well in himself and this is about what "
                    + "he has learned to do.\nNEEDS: trainer";
        }
        if (q.contains("food") || q.contains("switch") || q.contains("groom")) {
            return "Ordinary stuff, nothing urgent in it.\nNEEDS: everyday care";
        }
        return "He is bright, eating, and nothing about this needs anybody tonight. Sniff him "
                + "again in the morning if it has not settled.\nNEEDS: nobody";
    }

    /**
     * The three desks answer what they were actually ASKED, not one canned line each.
     */
    private static String vet(String prompt) {
        String q = worry(prompt);
        if (q.contains("chocolate") || q.contains("ate a") || q.contains("poison")) {
            return "Wake the human now — bark at the bedroom door until the light goes on — and "
                    + "bring them the wrapper: the vet will want the cocoa percentage, and dark "
                    + "is the worst kind. Do not wait to see whether he is sick.";
        }
        if (has(q, "ear", "ears")) {
            return "That is an infection until a vet says otherwise, and a smell means it has "
                    + "been going a while. Get the human to book today, nobody pokes at it, and "
                    + "stop him scratching it open.";
        }
        if (q.contains("snap") || q.contains("growl") || q.contains("grumpy")) {
            return "The Beagle is right to send him. A dog that growls where he never used to is "
                    + "telling you something hurts, and at four the usual suspects are teeth and "
                    + "ears. Get the human to book a full examination — mouth, ears, hips, spine "
                    + "— and everyone stays off his bed until he has been seen.";
        }
        if (q.contains("sock") || q.contains("swallow")) {
            return "Get the human home now and do not let anyone make him sick — a sock coming "
                    + "back up is how it gets stuck somewhere worse. Nothing to eat or drink. "
                    + "Tell them roughly when he swallowed it, because under two hours the vet "
                    + "has options it loses afterwards.";
        }
        if (q.contains("wasp") || q.contains("sting") || q.contains("stung")) {
            return "Watch his breathing, not his nose — a swollen face is ugly and usually fine, "
                    + "a swollen throat is not. If the swelling spreads past the muzzle, or he "
                    + "starts retching or wheezing, wake the human: vet, now. Meanwhile, a cold "
                    + "floor tile to lie on, and nobody goes looking for the wasp.";
        }
        if (q.contains("stuck") || q.contains("trapped")) {
            return "Do not pull him out by the back legs — that is how a stuck dog gets hurt. "
                    + "Dig the earth away from his chest from our side, keep him calm, and wake "
                    + "the human now: if he will not come free in ten minutes, the fence panel "
                    + "comes off.";
        }
        if (q.contains("limp") || q.contains("sore")) {
            return "Keep him still and off the stairs, and nobody brings him anything from the "
                    + "human's cupboard. A dog that will not put weight on a leg needs the vet "
                    + "today: get the human.";
        }
        return "Nothing here needs me tonight, but sniff him again in the morning if it has "
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
                    + "a dog that bites without warning first. Get him to the vet, then come "
                    + "back to me.\n"
                    + "ESCALATE";
        }
        if (q.contains("postman") || q.contains("letterbox") || q.contains("lunging")) {
            return "Lie across the hallway so he cannot reach the door, and make a fuss of him "
                    + "the moment the post lands — he learns the noise pays. Never let him "
                    + "rehearse the lunge; every time he does it, it works, because the postman "
                    + "always leaves.";
        }
        if (q.contains("pull") || q.contains("lead")) {
            return "Sit down dead every time he pulls ahead, and only move off when he comes "
                    + "back to your shoulder. Stop chasing him to catch up, which teaches him "
                    + "that pulling is how the pack moves.\n"
                + (kind(prompt) == Kind.BEHAVIOUR ? "ANSWERED" : "ESCALATE");
        }
        if (q.contains("dig")) {
            return "Give him a digging spot of his own by the shed and make a fuss of him every "
                    + "time he uses it. Stop shouting at him at the fence, which only tells him the "
                    + "fence is where the fun is.\n"
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
            return "Twice a week normally, daily while he is dropping coat — and lie still for "
                    + "it, it is over faster that way.";
        }
        return "Keep it boring and keep it the same: same food, same times, same garden.";
    }

    /**
     * Just what was asked, never the agent's own instructions — see {@link #kind}.
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
            "chocolate",
            // A dog stuck under a fence is a rescue, and no word above says so.
            "stuck", "trapped",
            // A swallowed sock and a stung face are medical for reasons no word above covers,
            // and they exist so demos 7 and 19 stop being the chocolate a second and third time.
            "swallow", "sting", "stung", "wasp"
    };
    private static final String[] BEHAVIOUR_WORDS = {
            "pull", "bark", "bite", "biting", "growl", "jump", "recall", "come back", "lead",
            "aggress", "scared", "afraid", "anxious", "chew", "destroy", "toilet", "training",
            "dig"
    };

    /**
     * Reads ONLY the question, never the tier's own instructions. This one is worth the comment
     * because the obvious version is wrong in a way tests caught and eyes would not: every tier's
     * prompt explains what is past it ("anything about pain, injury or illness"), so a match
     * against the whole prompt finds "injur" every single time, classifies every question as
     * medical, and the ladder walks to the top no matter what is asked — a planner that looks
     * exactly like a sequence.
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

    /**
     * What the room already knows, in a table: the dangerous ones and the harmless ones. The
     * first four are what the beard actually came back with; the rest are still here because
     * the input box is live on stage and somebody always types "chocolate".
     */
    private static final String[][] FOODS = {
            {"bone,chicken bone,rib", "Dangerous — a cooked bone splinters, and the splinters "
                    + "are the problem, not the bone. Wake the human: vet, now, and nobody gives "
                    + "him anything else to eat."},
            {"conker,chestnut,acorn", "Dangerous — conkers are toxic AND exactly the right size "
                    + "to block a gut. Wake the human for the "
                    + "vet, and count how many trees he walked under."},
            {"glove,sock,fabric,tea towel", "Dangerous — fabric does not pass, it wedges. Wake "
                    + "the human for the vet even though he looks delighted with himself."},
            {"croissant,pastry,bread,crust,toast", "Fine — plain baked dough does nothing. "
                    + "Nothing to do (raw dough would be a different answer)."},
            {"puddle,water,pond,rain", "Fine — that is just beard. It is going on the sofa, not "
                    + "into the dog. Nothing to do."},
            {"grape,raisin,sultana", "Dangerous — grapes and raisins can shut a dog's kidneys "
                    + "down and there is no known safe amount. Wake the human: vet, now."},
            {"chocolate,cocoa", "Dangerous — and dark is the worst kind. Wake the human: vet, "
                    + "now, with his weight and how much he ate; keep the wrapper."},
            {"onion,garlic,leek,shallot", "Dangerous — onions damage red blood cells, and raw is "
                    + "worse. Wake the human for the vet, even if he seems fine today."},
            {"xylitol,sweetener,sugar-free", "Dangerous — xylitol drops a dog's blood sugar "
                    + "within minutes. Wake the human: vet, now."},
            {"macadamia", "Dangerous — macadamias cause weakness and tremors. Wake the "
                    + "human for the vet."},
            {"cheese,cheddar", "Fine — a slice of cheese is fat and salt, nothing worse. Nothing "
                    + "to do."},
            {"bread,crust,toast", "Fine — plain baked bread does nothing. Nothing to do (raw "
                    + "dough would be a different answer)."},
            {"carrot,apple,banana", "Fine — nothing to do. Take the apple core off him though."}
    };

    /**
     * One verdict for one thing out of the beard. Reads ONLY the item, never the instruction:
     * the prompt itself uses the words "problem" and "ring the vet", so matching the whole text
     * would give every item the same answer and hide the entire point of a scatter/gather.
     */
    private static String beardVerdict(String prompt) {
        String item = prompt.toLowerCase(Locale.ROOT);
        int at = item.lastIndexOf("out of the beard:");
        item = at < 0 ? item : item.substring(at + "out of the beard:".length());
        for (String[] food : FOODS) {
            for (String name : food[0].split(",")) {
                if (item.contains(name)) {
                    return food[1];
                }
            }
        }
        return "Probably nothing, but watch him for a few hours and wake the human if he is sick "
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
