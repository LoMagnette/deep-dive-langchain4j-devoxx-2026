package dev.devoxx.dashboard;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * All agent contracts used by the dashboard, themed around the Belgian dog "Zao".
 * Each is a public nested interface so LangChain4j can build a JDK proxy for it.
 * The {@code @UserMessage} prompts are worded so the deterministic {@link MockChatModel}
 * returns parseable output (scores, labels, AGREE lines, etc.).
 */
public final class Agents {

    private Agents() {
    }

    // 1 / 2 / 3 — creative writing chain
    public interface CreativeWriter {
        @Agent(description = "Writes a short creative story about a topic")
        @UserMessage("Write a short, vivid story (3-4 sentences) about {{topic}}.")
        String write(@V("topic") String topic);
    }

    public interface StoryEditor {
        @Agent(description = "Edits and improves a story")
        @UserMessage("Edit and improve this story, keeping it short: {{story}}")
        String edit(@V("story") String story);
    }

    /**
     * Returns the score as a {@code String}, not a {@code double}, on purpose: a real chat model
     * answers "I'd rate this **0.85** out of 1.0" often enough that a {@code double} return type
     * blows up the whole loop with an OutputParsingException. The caller extracts the number
     * defensively — see {@code PatternCatalog.score(...)}.
     */
    public interface StoryScorer {
        @Agent(description = "Scores the quality of a story from 0.0 to 1.0")
        @UserMessage("Rate the quality of this story from 0.0 to 1.0. Answer with the number "
                + "only — no words, no explanation, no markdown. Story: {{story}}")
        String score(@V("story") String story);
    }

    // 4 — parallel experts
    public interface MovieExpert {
        @Agent(description = "Recommends a movie based on a mood")
        @UserMessage("Recommend one movie for someone feeling {{mood}}. One sentence.")
        String recommendMovie(@V("mood") String mood);
    }

    public interface MealExpert {
        @Agent(description = "Recommends a meal based on a mood")
        @UserMessage("Recommend one meal for someone feeling {{mood}}. One sentence.")
        String recommendMeal(@V("mood") String mood);
    }

    // 5 — parallel mapper
    public interface TopicAnalyzer {
        @Agent(description = "Writes one interesting fact about a topic")
        @UserMessage("Write one interesting fact about {{topic}}. One sentence.")
        String analyze(@V("topic") String topic);
    }

    // 6 — conditional routing
    public interface CategoryRouter {
        @Agent(description = "Classifies a request into a category")
        @UserMessage("Classify this request into one of: technical, legal, medical. "
                + "Return one word. Request: {{request}}")
        String classify(@V("request") String request);
    }

    public interface TechnicalExpert {
        @Agent(description = "Answers technical questions")
        @UserMessage("Give a short technical answer for: {{request}}")
        String handle(@V("request") String request);
    }

    public interface LegalExpert {
        @Agent(description = "Answers legal questions")
        @UserMessage("Give a short legal answer for: {{request}}")
        String handle(@V("request") String request);
    }

    public interface MedicalExpert {
        @Agent(description = "Answers medical questions")
        @UserMessage("Give a short medical answer for: {{request}}")
        String handle(@V("request") String request);
    }

    // 7 — supervisor sub-agents
    public interface ActivityPlanner {
        @Agent(description = "Plans outdoor activities and walks for the dog Zao")
        @UserMessage("Plan an activity for the dog Zao. Request: {{request}}")
        String plan(@V("request") String request);
    }

    public interface MealPlanner {
        @Agent(description = "Plans healthy meals for the dog Zao")
        @UserMessage("Plan a meal for the dog Zao. Request: {{request}}")
        String plan(@V("request") String request);
    }

    // 8 — GOAP chain (outputKeys feed the next agent's inputs)
    public interface PersonExtractor {
        @Agent(description = "Extracts the subject/name from a prompt")
        @UserMessage("Extract the main subject from this prompt in a few words: {{prompt}}")
        String extract(@V("prompt") String prompt);
    }

    public interface Biographer {
        @Agent(description = "Writes a short write-up about a subject")
        @UserMessage("Write a short write-up about {{person}}. Two sentences.")
        String write(@V("person") String person);
    }

    // 9 — P2P negotiation (writes shared 'consensus' state to exit)
    public interface Negotiator {
        @Agent(description = "States a negotiating position on an issue")
        @UserMessage("State your position on the issue: {{issue}}")
        String propose(@V("issue") String issue);
    }

    public interface Mediator {
        @Agent(description = "Finds consensus from a proposed position")
        @UserMessage("Do the parties agree on this position? {{proposal}}")
        String mediate(@V("proposal") String proposal);
    }

    // 10 — blackboard experts
    public interface Researcher {
        @Agent(description = "Gathers facts about a problem")
        @UserMessage("Gather key facts about the problem: {{problem}}")
        String research(@V("problem") String problem);
    }

    public interface Analyst {
        @Agent(description = "Analyses gathered facts")
        @UserMessage("Analyse these facts about Zao: {{facts}}")
        String analyse(@V("facts") String facts);
    }

    public interface Solver {
        @Agent(description = "Produces a final solution from an analysis")
        @UserMessage("Produce a final solution from this analysis: {{analysis}}")
        String solve(@V("analysis") String analysis);
    }

    // 11 — voting sentiment classifiers (3 independent voters)
    public interface SentimentVoterA {
        @Agent(description = "Classifies sentiment (voter A)")
        @UserMessage("Classify the sentiment of this text as POSITIVE or NEGATIVE: {{text}}")
        String classify(@V("text") String text);
    }

    public interface SentimentVoterB {
        @Agent(description = "Classifies sentiment (voter B)")
        @UserMessage("Classify the sentiment of this text as POSITIVE or NEGATIVE: {{text}}")
        String classify(@V("text") String text);
    }

    public interface SentimentVoterC {
        @Agent(description = "Classifies sentiment (voter C)")
        @UserMessage("Classify the sentiment of this text as POSITIVE or NEGATIVE: {{text}}")
        String classify(@V("text") String text);
    }

    // 12 — debate (2 debaters + judge; judge must be LAST)
    public interface DebaterA {
        @Agent(description = "Argues one side of a motion")
        @UserMessage("Argue your position in this debate on the motion: {{motion}}")
        String argue(@V("motion") String motion);
    }

    public interface DebaterB {
        @Agent(description = "Argues the other side of a motion")
        @UserMessage("Argue your position in this debate on the motion: {{motion}}")
        String argue(@V("motion") String motion);
    }

    public interface DebateJudge {
        @Agent(description = "Judges a debate and gives the verdict")
        @UserMessage("As the judge, settle the debate on the motion: {{motion}}")
        String judge(@V("motion") String motion);
    }

    // 13 — BDI (two desires of different priority; unique agent types)
    public interface InfoGatherer {
        @Agent(description = "Gathers the information needed for a goal")
        @UserMessage("Gather information needed to achieve this goal: {{goal}}")
        String gather(@V("goal") String goal);
    }

    public interface Reporter {
        @Agent(description = "Writes a report once information is available")
        @UserMessage("Write a short report from this information: {{info}}")
        String report(@V("info") String info);
    }
}
