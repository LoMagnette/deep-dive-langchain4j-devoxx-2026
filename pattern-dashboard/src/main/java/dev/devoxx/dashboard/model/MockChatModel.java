package dev.devoxx.dashboard.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

/**
 * Deterministic, no-API-key chat model for the Pawer Rangers. It inspects the last user prompt
 * and returns a short, PARSEABLE answer so every mission can run on stage without a real LLM —
 * which is also what {@code mvn test} runs against.
 *
 * <p>Three things it does that a canned table usually does not, each because a mission needs it:
 * <ul>
 *   <li><b>Tool calls.</b> When the request carries tools, it asks for them one at a time, the
 *       way a model would, and only answers once every planned call has a result — so Mission 1's
 *       {@code tool-call} events are real round trips through the framework, offline.</li>
 *   <li><b>Item awareness.</b> The ducklings and the ice spots each get their own answer: eight
 *       identical lines would run a mapper perfectly and demonstrate nothing.</li>
 *   <li><b>A reactive supervisor plan.</b> Zao sends one Ranger per problem in the fair, read out
 *       of the request, and stops when there are none left.</li>
 * </ul>
 */
public class MockChatModel implements ChatModel {

    /** Fifi's reviews alternate 2/4 then 4/4, so a loop visibly iterates once and then exits. */
    private final AtomicInteger reviews = new AtomicInteger();
    private final AtomicInteger lineCounter = new AtomicInteger();
    /** How many fair problems Zao has handed out so far. */
    private final AtomicInteger plannerStep = new AtomicInteger();

    /** Filler for prompts no rule claims — themed, so an unmatched prompt still looks alive. */
    private static final String[] HOUSE_LINES = {
            "Paws up! Zao has noted it on the Pup Board.",
            "Sniff has his nose on it.",
            "Nothing to report, and Zoom has gone after a squirrel.",
            "Fifi has read it and finds it… adequate.",
            "Bolt has filed it. Bolt files everything."
    };

    private final List<ChatModelListener> listeners;

    public MockChatModel() {
        this(List.of());
    }

    /**
     * With listeners, the offline demo logs its prompts and answers exactly as a real model
     * does — {@code ChatModel.chat()} fires them and then calls {@link #doChat}, so overriding
     * doChat rather than chat is what buys that for free. Tests construct the no-arg version.
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
        String prompt = lastUserText(request);
        if (!request.toolSpecifications().isEmpty()) {
            // A model with gear asks for one tool at a time and reads each result before the
            // next call. Planned from the PROMPT, counted from the conversation, so every
            // round trip goes through the framework's own tool loop.
            List<ToolExecutionRequest> plan = toolPlan(collapse(prompt));
            int done = toolResultsSinceLastUser(request.messages());
            if (done < plan.size()) {
                return ChatResponse.builder().aiMessage(AiMessage.from(List.of(plan.get(done))))
                        .build();
            }
            prompt = prompt + " TOOL RESULTS: " + toolResults(request.messages());
        }
        return ChatResponse.builder().aiMessage(AiMessage.from(respond(prompt))).build();
    }

    // ------------------------------------------------------------------------------------------
    // Gear
    // ------------------------------------------------------------------------------------------

    private List<ToolExecutionRequest> toolPlan(String p) {
        List<ToolExecutionRequest> plan = new ArrayList<>();
        if (p.contains("answering letters at the pup hq front desk")
                && !p.contains("rewrite this answer to fit the noticeboard")) {
            // Mission 0: look up who does the job, then whether that Ranger is awake.
            boolean digging = p.contains("dig") && !p.contains("glasses") || p.contains("hole");
            plan.add(tool("rangerFor", digging ? "{\"job\":\"digging\"}"
                    : "{\"job\":\"finding something lost\"}"));
            plan.add(tool("onDuty", digging ? "{\"ranger\":\"Dig\"}" : "{\"ranger\":\"Sniff\"}"));
        } else if (p.contains("find what this mission is looking for")) {
            String mission = after(p, "mission:");
            if (mission.contains("hat")) {
                plan.add(tool("sniff", "{\"place\":\"the park bench\"}"));
                plan.add(tool("followTrail", "{\"scent\":\"green feather fluff\"}"));
            } else if (mission.contains("kitten") || mission.contains("oak")) {
                plan.add(tool("sniff", "{\"place\":\"the oak tree on Main Street\"}"));
            } else {
                plan.add(tool("sniff", "{\"place\":\"" + jsonEscape(mission.strip()) + "\"}"));
            }
        } else if (p.contains("bolt says the ladder must be at least")) {
            // The FIRST "at least" carries Bolt's number; the prompt says it twice.
            String size = ladderFor(firstNumber(after(p, "bolt says the ladder must be at least")));
            plan.add(tool("fetch", "{\"item\":\"" + size + " ladder\"}"));
            plan.add(tool("deliver",
                    "{\"item\":\"the " + size + " ladder\",\"place\":\"the rescue\"}"));
        }
        return plan;
    }

    private static final AtomicInteger TOOL_IDS = new AtomicInteger();

    private static ToolExecutionRequest tool(String name, String arguments) {
        return ToolExecutionRequest.builder().id("call_" + TOOL_IDS.incrementAndGet())
                .name(name).arguments(arguments).build();
    }

    /** The shed's next size up — the same rule ZoomGear applies, so the call matches the gear. */
    private static String ladderFor(double metres) {
        for (double l : new double[] {5, 7.5, 10, 15}) {
            if (l >= metres) {
                return (l % 1 == 0 ? String.valueOf((int) l) : String.valueOf(l)) + " m";
            }
        }
        return "15 m";
    }

    private static int toolResultsSinceLastUser(List<ChatMessage> messages) {
        int n = 0;
        for (ChatMessage m : messages) {
            if (m instanceof UserMessage) {
                n = 0;
            } else if (m instanceof ToolExecutionResultMessage) {
                n++;
            }
        }
        return n;
    }

    private static String toolResults(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder();
        for (ChatMessage m : messages) {
            if (m instanceof UserMessage) {
                sb.setLength(0);
            } else if (m instanceof ToolExecutionResultMessage t) {
                sb.append(t.text()).append(' ');
            }
        }
        return sb.toString();
    }

    /** The LAST user message only — plus any system prompt, which carries the agent's role. */
    private static String lastUserText(ChatRequest request) {
        StringBuilder sb = new StringBuilder();
        String lastUser = "";
        for (ChatMessage m : request.messages()) {
            if (m instanceof UserMessage um && um.hasSingleText()) {
                lastUser = um.singleText();
            } else if (m instanceof SystemMessage sm) {
                sb.append(' ').append(sm.text());
            }
        }
        return sb.append(' ').append(lastUser).toString();
    }

    // ------------------------------------------------------------------------------------------
    // The rule table
    // ------------------------------------------------------------------------------------------

    /** One canned behaviour: when the collapsed, lowercased prompt matches, reply from it. */
    private record Rule(Predicate<String> when, Function<String, String> reply) {
    }

    /**
     * Ordered, and each rule says what it stands in front of. Two rules for every trigger here:
     * it is an INSTRUCTION from the prompt, never content an answer could quote back (a refining
     * loop feeds its own output into the next prompt), and never a dog's name on its own (the
     * same Ranger speaks in a dozen missions).
     */
    private List<Rule> rules() {
        return List.of(
                // --- Mission 9. The supervisor's planner prompt. FIRST, because it quotes the
                // descriptions of all four Rangers and would otherwise trip their rules.
                new Rule(p -> p.contains("planner expert") || p.contains("agent invocation"),
                        this::supervisorPlan),

                // --- Mission 0. The front desk. The REPROMPT first: it arrives with the same
                // system message, so the long-answer rule below would claim it too. The first
                // answer is deliberately too long for PawSized, so the output guardrail fires
                // offline every time and the room sees the reprompt.
                new Rule(p -> p.contains("rewrite this answer to fit the noticeboard"),
                        p -> "Sniff will find your glasses, Mr Mayor — he is on duty and can be "
                                + "with you in ten minutes. Paws up!"),
                new Rule(p -> p.contains("answering letters at the pup hq front desk"),
                        p -> "Dear Mr Mayor, thank you so very much for your letter to Pup HQ, "
                                + "which we have pinned to the wall and read aloud to the whole "
                                + "team twice. I have checked the duty roster most carefully: the "
                                + "Ranger for finding lost things is Sniff, our Beagle, whose nose "
                                + "has never once let Barkville down, and I am delighted to report "
                                + "that he is on duty, nose ready, and can be with you in about "
                                + "ten minutes. Warmest regards from all of us at Pup HQ, Zao."),

                // --- Missions 3 and 17. Fifi before Howl: her prompt quotes his draft.
                new Rule(p -> p.contains("score this draft against the four rules"),
                        p -> reviews.getAndIncrement() % 2 == 0
                                ? "SCORE: 2/4\nFEEDBACK: Too loud, too long, and it never says "
                                + "when or where."
                                : "SCORE: 4/4\nFEEDBACK: Acceptable. I suppose."),
                new Rule(p -> p.contains("write what the brief below asks for"),
                        MockChatModel::howlDraft),

                // --- Missions 13 and 18. The two model voters; Bolt votes in Java.
                new Rule(p -> p.contains("judge only by who is already out on the ice"),
                        p -> "SAFE — the ducks are walking on it and it smells of nothing but duck."),
                new Rule(p -> p.contains("you are the medic and you look for what could go wrong"),
                        p -> "NOT SAFE — the dark patch by the reeds is thin ice over moving water."),

                // --- Missions 6 and 19. Zao's classifier. Reads ONLY the call, never the
                // definitions its own prompt gives: those mention every category's words.
                new Rule(p -> p.contains("classify this emergency call"),
                        p -> kind(after(p, "the call:"))),

                // --- Missions 1, 2, 17, 21. Sniff finds; doChat has appended the tool results.
                new Rule(p -> p.contains("find what this mission is looking for"),
                        MockChatModel::sniffFound),

                // --- Mission 2. The kitten chain, one Ranger after another.
                new Rule(p -> p.contains("bring the ladder from pup hq"),
                        p -> "Ladder up against the north side of the oak, I went up, and the "
                                + "kitten came down under my chin. It reached the ground scared, "
                                + "holding one paw up, but in one piece."),
                new Rule(p -> p.contains("check whoever was just rescued"),
                        p -> "The kitten from 6 metres up the Main Street oak is scared but fine, "
                                + "with a thorn in its front paw. Nobody picks it up by that paw "
                                + "until the thorn is out."),
                new Rule(p -> p.contains("write the barkville gazette story of this rescue"),
                        p -> "KITTEN SAVED IN MAIN STREET DRAMA\nSix metres up, clinging to the "
                                + "north branch, a kitten faced the end. The Pawer Rangers did not "
                                + "let it. It is scared, thorny, and fine."),

                // --- Missions 4 and 20. Zao's merge FIRST: it quotes all three inspections.
                new Rule(p -> p.contains("merge the three inspections"),
                        p -> "Bridge: CLOSED\nForest: OPEN — the south path only\nTunnels: OPEN\n"
                                + "Tonight everyone goes through the tunnels, and nobody crosses "
                                + "the bridge."),
                new Rule(p -> p.contains("inspect the river bridge"),
                        p -> "Two planks cracked and the rope rail is fraying.\nCLOSED"),
                new Rule(p -> p.contains("inspect the forest path"),
                        p -> "A dead oak leans over the north path; the south path is clear.\n"
                                + "OPEN — south path only"),
                new Rule(p -> p.contains("inspect the old drainage tunnels"),
                        p -> "Dry, clear, and every grate is holding.\nOPEN"),

                // --- Mission 5. One duckling per call, read from the item, never the prompt.
                new Rule(p -> p.contains("search for this one duckling"),
                        p -> duckling(after(p, "the duckling:"))),

                // --- Mission 7. Dig plans, Jo answers, Dig acts.
                new Rule(p -> p.contains("plan the tunnel for this rescue"),
                        p -> "Start at the garden wall, behind the roses, not in the bed.\nTunnel "
                                + "under the third bush, where the hedgehog is.\nThe roots above "
                                + "will be disturbed: two prize blooms may drop."),
                new Rule(p -> p.contains("officer jo has answered"), MockChatModel::digActs),

                // --- Missions 8, 10, 17. Zoom with the ladder (gear first, then this).
                new Rule(p -> p.contains("bolt says the ladder must be at least"),
                        p -> "Fetched " + ladderFetched(p) + " from the fire-station shed and "
                                + "delivered it, leaning against the trunk, ready to climb."),

                // --- Mission 10. GOAP's last two links.
                new Rule(p -> p.contains("dig its feet into the ground and hold it steady"),
                        p -> "Feet dug in twenty centimetres, me sitting on the bottom rung. It "
                                + "is not going anywhere: secure."),
                new Rule(p -> p.contains("climb it, bring mittens down"),
                        p -> "Up the ladder, Mittens under one paw, down again. She is unhurt, "
                                + "ungrateful, and has scratched my nose."),

                // --- Mission 11. The maze. Three peers, and the rules only read what the OTHER
                // pups reported: Sniff's first scent splits (path AND hedge) so Zoom and Dig are
                // woken together; their two reports wake Sniff, who now points at the middle; and
                // Zoom, sent there, sees the goat.
                new Rule(p -> p.contains("you are sniff, in a giant corn maze"),
                        p -> after(p, "zoom reports:").contains("found:")
                                ? "FOUND: confirmed by nose — Gertrude is in the middle of the maze."
                                : after(p, "zoom reports:").contains("towards the middle")
                                ? "Scent is strong now and runs north, along the paths towards "
                                + "the middle. Nothing goes underground — Dig, that burrow is a "
                                + "dead end. Zoom, the middle."
                                : "Goat scent at the entrance, and it splits: one trail along the "
                                + "east path, one under the west hedge. Zoom, the east path. Dig, "
                                + "the hedge."),
                new Rule(p -> p.contains("you are zoom, in a giant corn maze"),
                        p -> after(p, "sniff says:").contains("middle")
                                ? "FOUND: Gertrude is in the very middle of the maze, eating the "
                                + "scarecrow's hat."
                                : "Ran the east path: cleared, no goat. Fresh hoof prints turn "
                                + "north, towards the middle."),
                new Rule(p -> p.contains("you are dig, in a giant corn maze"),
                        p -> after(p, "sniff says:").contains("west hedge")
                                ? "Under the west hedge: a rabbit burrow, far too small for a "
                                + "goat. Dead end — and one cross rabbit."
                                : "Nothing under the north hedges. Standing down, as told."),

                // --- Mission 12. Zao FIRST: his prompt quotes Dig's and Doc's clues.
                new Rule(p -> p.contains("read the clues on the board and name the culprit"),
                        p -> "The culprit is Mittens: four-toed, clawless prints run from the "
                                + "drain to her garden at number 9. The crumb in my beard is from "
                                + "the sausage dropped on our mat at 02:33, which I ate at 07:02 — "
                                + "at 02:20 I was asleep in my basket, on camera. I am innocent. "
                                + "It was Mittens."),
                new Rule(p -> p.contains("examine the crumb in zao's beard"),
                        p -> "The crumb is from the sausage dropped on Pup HQ's mat at 02:33, "
                                + "which Zao ate at 07:02.\nAt 02:20, while the sausages were "
                                + "going, he was asleep in his basket: cleared."),
                new Rule(p -> p.contains("report what your nose found, in two short plain sentences"),
                        p -> "The scent runs from the butcher's back door, along the alley, and "
                                + "down the storm drain at the end.\nIt never goes near Pup HQ."),
                new Rule(p -> p.contains("you have just crawled the drain sniff found"),
                        p -> "Small paw prints, four toes, no claw marks — a cat's, not a "
                                + "dog's.\nThey come up in the garden of number 9."),

                // --- Mission 14. Fifi FIRST: her prompt quotes the whole transcript.
                new Rule(p -> p.contains("moderating the barkville town council"),
                        p -> "Winner: the dog park. 1. Howl showed the lot is the only green space "
                                + "on Elm Street. 2. Mittens conceded a café is open eight hours "
                                + "and a park all day. 3. Nobody answered Howl's point about the "
                                + "school next door. Howl's weakest moment: the howling."),
                new Rule(p -> p.contains("you are howl, at the barkville town council"),
                        p -> round(p, new String[] {
                                "The lot on Elm Street is the only green on the street, and a dog "
                                        + "park keeps it green. Every dog in Barkville needs room "
                                        + "to RUN!",
                                "Mittens says a café brings visitors — a park brings every family "
                                        + "in Barkville, every single day, for free.",
                                "And the school next door gets a park to look at, not a window "
                                        + "full of cats looking back."})),
                new Rule(p -> p.contains("you are mittens the cat"),
                        p -> round(p, new String[] {
                                "Green? It is mud with ideas. A cat café brings visitors, pays "
                                        + "rent, and is quiet, which is more than can be said for "
                                        + "Howl.",
                                "Every family, every day, every dog — and every one of them "
                                        + "barking. A café is open eight hours and calm in all of "
                                        + "them.",
                                "The school would learn more from a cat than from a dog. I rest "
                                        + "my case, and then I rest."})),

                // --- Mission 15. Zoom's five plan steps. The lookout is the belief revision: it
                // reports a STRANDED kid unless the radio already said the kid is safe — which is
                // the whole difference between a preempted chase and a straight one.
                new Rule(p -> p.contains("step one, chase it up the riverbank"),
                        p -> after(p, "what the radio said:").contains("safe")
                                ? "From the top of the bank: the bridge is out, and the far bank "
                                + "is empty — Officer Jo has the kid.\nThe squirrel is still in "
                                + "sight."
                                : "From the top of the bank: the bridge is out, and a kid is "
                                + "STRANDED on the far bank, waving.\nThe squirrel is still in "
                                + "sight."),
                new Rule(p -> p.contains("step two, chase it up a tree"),
                        p -> "Up the big oak by the river, round it twice, and it went up.\nDid "
                                + "not catch it. Will not catch it. Will try again."),
                new Rule(p -> p.contains("step one, the bridge is out, so run downstream"),
                        p -> "Downstream to the old ford, two kilometres the long way round.\n"
                                + "Knee-deep and very cold."),
                new Rule(p -> p.contains("step two of the rescue"),
                        p -> "Reached the kid, and back over the ford with them holding my "
                                + "collar.\nSafe, wet to the knees, and very impressed."),
                new Rule(p -> p.contains("everything that matters is done. nap"),
                        p -> "On the warm stones by the river, for an hour and a half."),

                // --- Mission 18. One spot of ice per call, and Howl's announcement.
                new Rule(p -> p.contains("check the ice at this one spot"),
                        p -> iceSpot(after(p, "the spot:"))),
                new Rule(p -> p.contains("announce the skating party"),
                        p -> "BARKVILLE! The skating party is OFF. The ice by the reeds is thin "
                                + "over running water, and one NOT SAFE is all it takes. Hot "
                                + "chocolate at Pup HQ instead!"),

                // --- Mission 19. One Doc, every call.
                new Rule(p -> p.contains("you are doc, on the pup hq night phone"),
                        p -> nightPhone(after(p, "the call:"))),

                // --- Mission 21. The optional first aid.
                new Rule(p -> p.contains("give first aid for the injury below"),
                        p -> "Lie still and let Doc ease the thorn out with his front teeth.\nNo "
                                + "licking the paw for an hour.\nIf it swells, or the kitten will "
                                + "not stand on it, fetch Officer Jo."),

                // --- Missions 6, 9, 16. The four Rangers on call. LAST, because they answer
                // whatever the call is about, and every mission above is more specific.
                new Rule(p -> p.contains("you are sniff, the pawer ranger who"),
                        p -> onCall("Sniff", after(p, "the call:"))),
                new Rule(p -> p.contains("you are dig, the pawer ranger who"),
                        p -> onCall("Dig", after(p, "the call:"))),
                new Rule(p -> p.contains("you are doc, the pawer ranger who"),
                        p -> onCall("Doc", after(p, "the call:"))),
                new Rule(p -> p.contains("you are zoom, the pawer ranger who"),
                        p -> onCall("Zoom", after(p, "the call:"))));
    }

    private String respond(String prompt) {
        // Whitespace is collapsed before matching because the prompts are text blocks: a phrase
        // that wraps is one phrase to a reader and "a\nb" to String.contains.
        String p = collapse(prompt);
        for (Rule r : rules()) {
            if (r.when().test(p)) {
                return r.reply().apply(p);
            }
        }
        return HOUSE_LINES[Math.floorMod(lineCounter.getAndIncrement(), HOUSE_LINES.length)];
    }

    private static String collapse(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    // ------------------------------------------------------------------------------------------
    // Answers that depend on what was asked
    // ------------------------------------------------------------------------------------------

    private static String sniffFound(String p) {
        String found = after(p, "tool results:");
        if (found.contains("duck pond") && found.contains("hat")) {
            return "The Mayor's hat is floating in the middle of the duck pond, with a duck "
                    + "sitting in it.";
        }
        if (found.contains("kitten")) {
            return "The kitten is 6 metres up the oak on Main Street, on the north branch.";
        }
        return "Nose down, nothing yet: the trail stops at the town hall.";
    }

    /** First draft loud and incomplete, second fixed — so Fifi's 2/4 then 4/4 is earned. */
    private static String howlDraft(String p) {
        boolean first = after(p, "fifi's feedback on your last version:").contains("none yet");
        boolean gazette = after(p, "the brief:").contains("gazette");
        if (gazette) {
            return first
                    ? "KITTEN!!! A KITTEN!!! The Pawer Rangers, bravest dogs who ever lived, faced "
                    + "the mightiest oak in all of Barkville and WON, and nobody will ever forget "
                    + "this day as long as there are dogs to howl about it!!!"
                    : "KITTEN SAVED ON MAIN STREET\nThe Rangers brought a kitten down from 6 "
                    + "metres up the Main Street oak. It is scared but fine, minus one thorn.";
        }
        return first
                ? "AWOOOO! THE TOWN FAIR!!! SAUSAGES!!! DOGS!!! THE GREATEST DAY IN THE HISTORY OF "
                + "BARKVILLE IS COMING AND YOU WILL NOT BELIEVE THE SAUSAGES!!!"
                : "Barkville Town Fair — Saturday 12 October, Barkville Green, 10:00 to 16:00. "
                + "Free entry. Sausage stall, and the dog show at 14:00.";
    }

    private static String digActs(String p) {
        String said = after(p, "what officer jo said:");
        boolean refused = said.startsWith(" no") || said.contains(" no.") || said.contains("don't")
                || said.contains("do not") || said.contains("refuse") || said.contains("wait");
        if (refused) {
            return "Not digging. Instead: a saucer of cat food by the garden wall tonight, and "
                    + "the hedgehog will walk out on his own.";
        }
        boolean changed = said.contains("but") || said.contains("only") || said.contains("instead");
        return "Dug from the garden wall as planned; the hedgehog walked out, and the roses are "
                + "still standing." + (changed ? " And did exactly what Jo added:" + said : "");
    }

    private static String ladderFetched(String p) {
        Matcher m = Pattern.compile("the (\\d+(?:\\.\\d+)?) m ladder").matcher(p);
        return m.find() ? "the " + m.group(1) + " m ladder" : "a ladder";
    }

    private static String duckling(String item) {
        String d = item.strip();
        String where = d.contains("puddle") ? "asleep under the lily pads at the far end of the pond"
                : d.contains("pickle") ? "inside the bakery bin, eating a croissant"
                : d.contains("waddles") ? "on the town hall steps, being fed by the Mayor"
                : d.contains("biscuit") ? "swimming in circles in the fountain, very pleased"
                : d.contains("noodle") ? "under the bandstand, stuck behind a drum — pulled out"
                : d.contains("pip") ? "at the bus stop, waiting for the number 7"
                : d.contains("socks") ? "in the Mayor's roses, eating them"
                : d.contains("bean") ? "on Mittens' doorstep, being watched very closely — rescued "
                + "just in time"
                : "by the duck pond, perfectly fine";
        return "Found " + where + ".";
    }

    private static String iceSpot(String spot) {
        return spot.contains("reeds") ? "Dark grey ice, and I can hear water moving underneath. I "
                + "would not put a paw on it."
                : spot.contains("middle") ? "Clear, hard ice. It does not creak under me."
                : spot.contains("jetty") ? "Fine at the jetty, a little slushy at the very edge."
                : "Solid all round; the ducks have been here all morning.";
    }

    private static String nightPhone(String call) {
        return switch (kind(call)) {
            case "hurt" -> "Keep him sitting down with the ankle up on a cushion and something "
                    + "cold on it. Nobody walks him home — wake Officer Jo, he needs the doctor "
                    + "tonight.";
            case "underground" -> "Do not climb in after him. Keep talking to him, and wake "
                    + "Officer Jo: Dig is on his way.";
            case "urgent" -> "Wake Officer Jo and send Zoom. Everybody else, out of the road.";
            default -> "No need to wake anyone. Check the bakery's umbrella stand first — "
                    + "everything in Barkville ends up there.";
        };
    }

    /** What a Ranger on call says, from what the call is actually about. */
    private static String onCall(String ranger, String call) {
        String c = call.strip();
        if (c.contains("child")) {
            return "Found the child by the carousel, sharing a toffee apple with a clown. Back "
                    + "with their mum.";
        }
        if (c.contains("sausage cart")) {
            return "Caught the sausage cart ten metres from the duck pond. Two sausages are "
                    + "missing; I was not involved.";
        }
        if (c.contains("bouncy castle")) {
            return "Crawled inside the bouncy castle and found the hole — a Mittens-sized claw "
                    + "mark. Patched it with the Mayor's sash.";
        }
        if (c.contains("glasses")) {
            return "Found the Mayor's reading glasses. They were on the Mayor's head.";
        }
        if (c.contains("post")) {
            return "Fetched the post from the station in four minutes flat.";
        }
        if (c.contains("drain")) {
            return "Drain on Elm Street cleared: one tennis ball, eleven leaves and a sock.";
        }
        if (c.contains("puppy")) {
            return "The new puppy at number 4 is healthy, loud, and chewing a slipper.";
        }
        if (c.contains("well") || c.contains("tortoise")) {
            return "Head first down the old well behind the bakery: the tortoise is on a ledge two "
                    + "metres down. I will dig a ramp and walk him out.";
        }
        return switch (ranger) {
            case "Sniff" -> "Nose down at the last place it was seen. I will follow the trail "
                    + "until it ends.";
            case "Dig" -> "Going in through the nearest tight spot, and out again with them.";
            case "Doc" -> "Checking breathing first, then the leg. Nobody moves them until I have.";
            default -> "Running now — there in two minutes.";
        };
    }

    /**
     * The emergency phone's classifier, from the call alone. Must stay in step with
     * {@code Parsing.category}'s four destinations, or it picks a branch that does not exist.
     */
    private static String kind(String call) {
        String c = call.toLowerCase(Locale.ROOT);
        if (has(c, "hurt", "injured", "bleeding", "swollen", "swelling", "slipped", "ankle",
                "broken", "sick", "thorn")) {
            return "hurt";
        }
        if (has(c, "well", "hole", "tunnel", "drain", "stuck under", "underground", "fallen down")) {
            return "underground";
        }
        if (has(c, "rolling", "runaway", "downhill", "on fire", "racing")) {
            return "urgent";
        }
        return "lost";
    }

    private static boolean has(String text, String... words) {
        for (String w : words) {
            if (text.contains(w)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The debaters' next line. DebatePlanner hands each round only the LAST round's statements
     * (debateContext), so there is no round counter on the board: the round is worked out from
     * which of this debater's own lines is already in the context — none means round one.
     */
    private static String round(String p, String[] lines) {
        String lastRound = after(p, "last round:");
        int next = 0;
        for (int i = 0; i < lines.length; i++) {
            if (lastRound.contains(collapse(lines[i]))) {
                next = i + 1;
            }
        }
        return lines[Math.min(next, lines.length - 1)];
    }

    // ------------------------------------------------------------------------------------------
    // Mission 9: Zao, the supervisor
    // ------------------------------------------------------------------------------------------

    /**
     * The argument every Ranger on call takes. A planner's JSON names the agent's parameter, so
     * this is {@code demos._06_conditional.Keys.Call} spelled out — the mock cannot import it
     * without making the offline model depend on the demos.
     */
    private static final String CALL_ARG = "Call";

    /**
     * The canned plan, and it is a reactive one: the fair's problems are read out of the request
     * in order, one Ranger is sent per problem, and Zao finishes when none are left — so the
     * supervisor genuinely decides "next" from where it has got to.
     */
    private String supervisorPlan(String prompt) {
        if (prompt.contains("last received response is: ''")) {
            plannerStep.set(0);
        }
        String request = between(prompt, "the user request is: '", "'.");
        List<String> problems = new ArrayList<>();
        for (String part : request.split(",| and ")) {
            String s = part.strip();
            if (s.contains("child") || s.contains("cart") || s.contains("hole")
                    || s.contains("hurt")) {
                problems.add(s);
            }
        }
        int step = plannerStep.getAndIncrement();
        if (step < problems.size()) {
            String problem = problems.get(step);
            String ranger = problem.contains("child") ? "Sniff"
                    : problem.contains("cart") ? "Zoom"
                    : problem.contains("hole") ? "Dig" : "Doc";
            return "{\"agentName\":\"" + ranger + "\",\"arguments\":{\"" + CALL_ARG + "\":\""
                    + jsonEscape(problem) + "\"}}";
        }
        return "{\"agentName\":\"done\",\"arguments\":{\"response\":\"Every problem has had a "
                + "Ranger. The fair is under control.\"}}";
    }

    // ------------------------------------------------------------------------------------------
    // Small helpers
    // ------------------------------------------------------------------------------------------

    /** Everything after the LAST occurrence of a label — what was asked, not the instructions. */
    private static String after(String text, String label) {
        int at = text.lastIndexOf(label);
        return at < 0 ? "" : text.substring(at + label.length());
    }

    private static double firstNumber(String text) {
        Matcher m = Pattern.compile("\\d+(?:\\.\\d+)?").matcher(text);
        return m.find() ? Double.parseDouble(m.group()) : 0;
    }

    /** Substring between two markers, or a themed fallback if the markers aren't found. */
    private static String between(String text, String start, String end) {
        int i = text.indexOf(start);
        if (i < 0) {
            return "paws up, rangers";
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
}
