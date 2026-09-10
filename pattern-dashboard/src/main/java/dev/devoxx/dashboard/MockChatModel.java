package dev.devoxx.dashboard;

import java.util.concurrent.atomic.AtomicInteger;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

/**
 * Deterministic, no-API-key chat model. It inspects the last user prompt and returns a
 * short, PARSEABLE answer so every agentic pattern can run on stage without a real LLM.
 *
 * The heuristics are intentionally simple and ordered so that loop/voting/debate/consensus
 * predicates can trigger reliably.
 */
public class MockChatModel implements ChatModel {

    private final AtomicInteger scoreCounter = new AtomicInteger();
    private final AtomicInteger zaoCounter = new AtomicInteger();
    /** Which step of the supervisor's canned plan we're on (1 = activity, 2 = meal, 3+ = done). */
    private final AtomicInteger plannerStep = new AtomicInteger();

    private static final String[] ZAO_LINES = {
            "Zao, the Belgian shepherd, chases autumn leaves across the Grand-Place in Brussels.",
            "Zao naps under the kitchen table, one ear twitching at the smell of frites.",
            "On a rainy Ghent morning, Zao proudly carries the newspaper back to the door.",
            "At the forest edge, Zao answers a distant wolf howl with one careful bark.",
            "The Ardennes pack moves as one shadow through the birches, Zao at the flank.",
            "By the canal in Bruges, Zao watches the swans and dreams of a stolen waffle.",
            "Zao herds three ducks, two children and one very patient cat into the garden.",
            "Zao greets every neighbour on the Ardennes trail, tail wagging like a metronome."
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

    private String respond(String prompt) {
        String p = prompt.toLowerCase();

        // 0. Supervisor planner -> must return a JSON AgentInvocation, NOT free text. This branch
        //    is FIRST because the planner prompt also contains "one of ... agents", which would
        //    otherwise trip the category-routing heuristic below and emit an unparseable word.
        //    Protocol (langchain4j PlannerAgent): pick the next agent, or agentName "done" with a
        //    "response" argument to finish. We walk both sub-agents, then finish, so the demo
        //    actually shows a supervisor delegating twice rather than looping on one agent.
        if (p.contains("planner expert") || p.contains("agent invocation")) {
            boolean firstRound = p.contains("last received response is: ''");
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
                return "{\"agentName\":\"ActivityPlanner\",\"arguments\":{\"request\":\"" + req + "\"}}";
            }
            if (step == 2) {
                return "{\"agentName\":\"MealPlanner\",\"arguments\":{\"request\":\"" + req + "\"}}";
            }
            return "{\"agentName\":\"done\",\"arguments\":{\"response\":\""
                    + "Planned a great day for Zao and confirmed the meal — all set.\"}}";
        }

        // Word-boundary matching so incidental substrings (e.g. "celeb-RATE-s" in a story
        // being edited) don't trip the wrong heuristic branch.
        // 1. Sentiment classification -> constant label (voting majority converges).
        if (has(p, "sentiment", "positive", "negative")) {
            return "POSITIVE";
        }
        // 2. Numeric score/rating -> alternates low(0.60) then high(0.95) so an iterative
        //    loop visibly runs a couple of rounds and then reliably crosses the exit bar.
        if (p.contains("0.0") || has(p, "score", "rate", "number")) {
            double v = (scoreCounter.getAndIncrement() % 2 == 0) ? 0.60 : 0.95;
            return String.format(java.util.Locale.US, "%.2f", v);
        }
        // 3. Care routing -> first known option present in the prompt. Must stay in step with
        //    PatternCatalog.CATEGORIES, or the router picks a branch that doesn't exist.
        if (has(p, "classify", "category") || p.contains("one of")) {
            for (String opt : new String[] {"behaviour", "nutrition", "veterinary", "other"}) {
                if (has(p, opt)) {
                    return opt;
                }
            }
            return "behaviour";
        }
        // 4. Debate / consensus / negotiation -> a line ending in AGREE so convergence fires.
        if (has(p, "agree", "consensus", "debate", "argue", "position")) {
            return "After weighing every position on Zao's daily routine, we fully AGREE.";
        }
        // 5. Default -> a varied themed sentence about the dog Zao.
        return ZAO_LINES[Math.floorMod(zaoCounter.getAndIncrement(), ZAO_LINES.length)];
    }

    /** Substring between two markers, or a themed fallback if the markers aren't found. */
    private static String between(String text, String start, String end) {
        int i = text.indexOf(start);
        if (i < 0) {
            return "plan a great Saturday for the dog Zao";
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

    /** True if any word appears as a whole word (word boundaries) in the (lowercased) text. */
    private static boolean has(String text, String... words) {
        for (String w : words) {
            if (java.util.regex.Pattern.compile("\\b" + java.util.regex.Pattern.quote(w) + "\\b")
                    .matcher(text).find()) {
                return true;
            }
        }
        return false;
    }
}
