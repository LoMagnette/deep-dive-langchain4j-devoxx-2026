package dev.devoxx.dashboard;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * All agent contracts used by the dashboard, themed around the Belgian shepherd "Zao" and the
 * pack he runs with. Each is a public nested interface so LangChain4j can build a JDK proxy.
 *
 * <p>The names are the demo: a room reading {@code KennelRouter → VetExpert} understands
 * conditional routing before the speaker explains it, in a way {@code Router → ExpertB} never
 * manages. The domain is deliberately one everybody already has intuitions about — is the dog
 * limping, is it hungry, should it be on the sofa — so attention stays on the topology.
 *
 * <p>The {@code @UserMessage} prompts are worded so the deterministic {@link MockChatModel}
 * returns parseable output (scores, labels, AGREE lines). Change the wording and check the mock:
 * its branches key off words like "0.0", "classify", "POSITIVE", "debate".
 */
public final class Agents {

    private Agents() {
    }

    // 1 / 2 / 3 — the pack chronicle: write, edit, judge
    public interface PackChronicler {
        @Agent(description = "Writes a short tale from the pack's chronicle")
        @UserMessage("Write a short, vivid tale (3-4 sentences) about {{topic}}.")
        String write(@V("topic") String topic);
    }

    public interface PackEditor {
        @Agent(description = "Tightens up a tale from the pack's chronicle")
        @UserMessage("Edit and improve this tale, keeping it short: {{tale}}")
        String edit(@V("tale") String tale);
    }

    /**
     * Returns the score as a {@code String}, not a {@code double}, on purpose: a real chat model
     * answers "I'd rate this **0.85** out of 1.0" often enough that a {@code double} return type
     * blows up the whole loop with an OutputParsingException. The caller extracts the number
     * defensively — see {@code PatternCatalog.score(...)}.
     */
    public interface PackCritic {
        @Agent(description = "Scores how good a tale is, from 0.0 to 1.0")
        @UserMessage("Rate the quality of this tale from 0.0 to 1.0. Answer with the number "
                + "only — no words, no explanation, no markdown. Tale: {{tale}}")
        String score(@V("tale") String tale);
    }

    // 4 — parallel: two specialists read the same mood, independently
    public interface WalkExpert {
        @Agent(description = "Picks the right walk for a dog in a given mood")
        @UserMessage("Recommend one walk or outing for a dog feeling {{mood}}. One sentence.")
        String recommendWalk(@V("mood") String mood);
    }

    public interface TreatExpert {
        @Agent(description = "Picks the right food or treat for a dog in a given mood")
        @UserMessage("Recommend one meal or treat for a dog feeling {{mood}}. One sentence.")
        String recommendTreat(@V("mood") String mood);
    }

    // 5 — parallel mapper: the same scout runs over every item in a list
    public interface PackScout {
        @Agent(description = "Digs up one fact about something in the dog's world")
        @UserMessage("Write one interesting fact about {{topic}}. One sentence.")
        String scout(@V("topic") String topic);
    }

    // 6 — conditional routing: triage a worried owner's question
    public interface KennelRouter {
        @Agent(description = "Sorts a question about a dog into the right kind of care")
        @UserMessage("Classify this request into one of: behaviour, nutrition, veterinary. "
                + "Return one word. Request: {{request}}")
        String classify(@V("request") String request);
    }

    public interface BehaviourExpert {
        @Agent(description = "Answers dog training and behaviour questions")
        @UserMessage("Give a short dog-behaviour answer for: {{request}}")
        String handle(@V("request") String request);
    }

    public interface NutritionExpert {
        @Agent(description = "Answers what and how much a dog should eat")
        @UserMessage("Give a short canine-nutrition answer for: {{request}}")
        String handle(@V("request") String request);
    }

    public interface VetExpert {
        @Agent(description = "Answers veterinary and health questions about a dog")
        @UserMessage("Give a short veterinary answer for: {{request}}")
        String handle(@V("request") String request);
    }

    // 7 — supervisor sub-agents. Keep these two names: MockChatModel's canned plan calls them.
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
    public interface DogExtractor {
        @Agent(description = "Works out which animal a prompt is really about")
        @UserMessage("Extract the animal this prompt is about, in a few words: {{prompt}}")
        String extract(@V("prompt") String prompt);
    }

    public interface PackBiographer {
        @Agent(description = "Writes the life story of an animal")
        @UserMessage("Write a short write-up about {{dog}}. Two sentences.")
        String write(@V("dog") String dog);
    }

    // 9 — P2P negotiation (writes shared 'consensus' state to exit)
    public interface PackNegotiator {
        @Agent(description = "States a position on a question facing the pack")
        @UserMessage("State your position on the issue: {{issue}}")
        String propose(@V("issue") String issue);
    }

    public interface PackMediator {
        @Agent(description = "Finds the consensus between pack members")
        @UserMessage("Do the members of the pack agree on this position? {{proposal}}")
        String mediate(@V("proposal") String proposal);
    }

    // 10 — blackboard: a wolf-pack hunt, in three roles
    public interface Tracker {
        @Agent(description = "Follows the trail and gathers the facts")
        @UserMessage("Gather key facts about the problem: {{problem}}")
        String track(@V("problem") String problem);
    }

    public interface PackAnalyst {
        @Agent(description = "Reads the trail the tracker found")
        @UserMessage("Analyse these facts about Zao: {{facts}}")
        String analyse(@V("facts") String facts);
    }

    public interface PackLeader {
        @Agent(description = "Makes the pack's final call")
        @UserMessage("Produce a final solution from this analysis: {{analysis}}")
        String decide(@V("analysis") String analysis);
    }

    // 11 — voting: three noses on the same news, majority wins
    public interface MoodSnifferA {
        @Agent(description = "Sniffs whether news about the dog is good or bad (nose A)")
        @UserMessage("Classify the mood of this news about the dog as POSITIVE or NEGATIVE: {{text}}")
        String classify(@V("text") String text);
    }

    public interface MoodSnifferB {
        @Agent(description = "Sniffs whether news about the dog is good or bad (nose B)")
        @UserMessage("Classify the mood of this news about the dog as POSITIVE or NEGATIVE: {{text}}")
        String classify(@V("text") String text);
    }

    public interface MoodSnifferC {
        @Agent(description = "Sniffs whether news about the dog is good or bad (nose C)")
        @UserMessage("Classify the mood of this news about the dog as POSITIVE or NEGATIVE: {{text}}")
        String classify(@V("text") String text);
    }

    // 12 — debate (2 debaters + judge; judge must be LAST)
    public interface DogAdvocate {
        @Agent(description = "Argues the dog's side of a motion")
        @UserMessage("Argue the dog's side in this debate on the motion: {{motion}}")
        String argue(@V("motion") String motion);
    }

    public interface HouseholdAdvocate {
        @Agent(description = "Argues the household's side of a motion")
        @UserMessage("Argue the household's side in this debate on the motion: {{motion}}")
        String argue(@V("motion") String motion);
    }

    public interface PackJudge {
        @Agent(description = "Judges the debate and gives the verdict")
        @UserMessage("As the judge, settle the debate on the motion: {{motion}}")
        String judge(@V("motion") String motion);
    }

    // 13 — BDI (two desires of different priority; unique agent types)
    public interface CareScout {
        @Agent(description = "Gathers what is known about the dog before anything is written")
        @UserMessage("Gather information needed to achieve this goal: {{goal}}")
        String gather(@V("goal") String goal);
    }

    public interface CareReporter {
        @Agent(description = "Writes the care report once the facts are in")
        @UserMessage("Write a short report from this information: {{info}}")
        String report(@V("info") String info);
    }
}
