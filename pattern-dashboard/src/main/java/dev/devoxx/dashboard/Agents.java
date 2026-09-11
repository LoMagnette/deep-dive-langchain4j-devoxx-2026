package dev.devoxx.dashboard;

import java.util.List;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * All agent contracts used by the dashboard. The setting is <b>Zao</b>, a Belgian shepherd, and
 * the household he runs — so the talk's "From Puppy to Pack" thread holds from the puppy's first
 * hour to the question of whether to get a second dog.
 *
 * <p>Two rules decide every scenario in here, and they pull against each other:
 *
 * <p><b>1. The pattern must be load-bearing.</b> Take it away and the answer visibly degrades:
 * the vote must be able to split, the critic must have rules to check, the planner must have an
 * order to discover. A demo where one plain prompt would do as well teaches the wiring and
 * nothing else.
 *
 * <p><b>2. The audience must not need the domain explained.</b> Everyone knows chocolate is bad
 * for dogs, that hot pavement burns paws, that a puppy needs the garden before he needs a
 * training session, and that neither half of a couple outranks the other about the bed. Nobody
 * knows what a 21-day rabies clearance is. A scenario that costs a sentence of setup costs it
 * fifteen times over, and the room spends the talk learning the domain instead of the patterns.
 *
 * <p>So the constraints these agents are checked against are ones the room already holds:
 * grapes are dangerous and cheddar is not, a fridge note needs the vet's number on it, you
 * practise recall in the garden before the park.
 *
 * <p>The {@code @UserMessage} prompts are worded so the deterministic {@link MockChatModel}
 * returns parseable output (scores, labels, PASS/FAIL lines, votes). Change the wording and check
 * the rule that matched it — the mock's table keys off phrases like "0.0", "classify this worry",
 * "YES or LATER".
 */
public final class Agents {

    private Agents() {
    }

    // ---------------------------------------------------------------- 1 / 2 — the sitter note
    // Everyone has either written this note or wished the owner had. It is the perfect first
    // demo: a rambling message in, something structured out, and the room grades it instantly —
    // did it keep the vet's number, did it invent a feeding time nobody mentioned?

    public interface SitterCardClerk {
        @Agent(description = "Turns a rambling message about the dog into a structured sitter card")
        @UserMessage("""
                Turn this message into a sitter card with exactly these five lines. Invent
                nothing — if the message does not say, write "not given":
                Dog:
                Meals:
                Walks:
                Watch out for:
                Vet:

                Message: {{message}}""")
        String card(@V("message") String message);
    }

    public interface FridgeChecklist {
        @Agent(description = "Turns a sitter card into the timed checklist that goes on the fridge")
        @UserMessage("""
                Turn this sitter card into the checklist that goes on the fridge door: the times
                of day in order, one line each, nothing the sitter has to work out for themselves.

                Sitter card: {{card}}""")
        String checklist(@V("card") String card);
    }

    // ---------------------------------------------------------------- 3 — the refinement loop
    // The critic scores against FOUR RULES THE ROOM AGREES WITH ON SIGHT. That is the whole
    // point: a critic scoring "quality" out of 1.0 gives a number nobody watching can check, so
    // the loop becomes a light show. Here the audience can hold the draft against the same four
    // rules and see which one each pass fixes.

    public interface SitterNoteWriter {
        @Agent(description = "Rewrites a note for the dog sitter until it is actually usable")
        @UserMessage("""
                Rewrite this note so someone who has never met the dog could follow it. All four
                rules must hold:
                1. every meal has a time and an amount
                2. it says where the lead and the poo bags are
                3. it gives the vet's telephone number
                4. under 100 words, so it fits on the fridge door

                Note: {{note}}""")
        String rewrite(@V("note") String note);
    }

    /**
     * Returns the score as a {@code String}, not a {@code double}, on purpose: a real chat model
     * answers "I'd rate this **0.85** out of 1.0" often enough that a {@code double} return type
     * blows up the whole loop with an OutputParsingException. The caller extracts the number
     * defensively — see {@link Parsing#score}.
     */
    public interface FridgeRuleCheck {
        @Agent(description = "Checks a sitter note against the four fridge-door rules")
        @UserMessage("""
                Check this note against four rules: every meal has a time and an amount, it says
                where the lead and poo bags are, it gives the vet's telephone number, and it is
                under 100 words. Give the fraction of rules that hold as a number from 0.0 to
                1.0 — the number only, no words, no explanation, no markdown.

                Note: {{note}}""")
        String check(@V("note") String note);
    }

    // ---------------------------------------------------------------- 4 — "walk him now?"
    // Two checks that plainly do not need each other, and both of which must pass before you put
    // the lead on. That is fan-out and join — and the join is a DECISION (either check can veto),
    // not a string concatenation.

    public interface WeatherCheck {
        @Agent(description = "Says whether the weather and the ground are safe for a walk now")
        @UserMessage("""
                Judge only the weather and the ground for a walk right now — heat, the pavement
                under a bare paw, ice, storms. Answer with one line starting PASS or FAIL, then
                the reason in a few words.

                Right now: {{walk}}""")
        String check(@V("walk") String walk);
    }

    public interface DogCheck {
        @Agent(description = "Says whether the dog himself is fit for a walk right now")
        @UserMessage("""
                Judge only the dog himself for a walk right now — has he just eaten, his age, any
                limp, anything he had done at the vet today. Answer with one line starting PASS or
                FAIL, then the reason in a few words.

                Right now: {{walk}}""")
        String check(@V("walk") String walk);
    }

    // ---------------------------------------------------------------- 5 — the picnic blanket
    // The best kind of scatter/gather demo: the room already knows every answer. Grapes and
    // chocolate are dangerous, cheddar and bread are not. The width of the fan-out is data, and
    // the cost caveat needs no explaining either — a shopping list is fifty calls.

    public interface FoodSafetyCheck {
        @Agent(description = "Says whether one thing the dog ate is dangerous, and what to do")
        @UserMessage("""
                The dog ate this off the picnic blanket. In one line: is it dangerous for a dog,
                and what should the owner do — nothing, watch him, or ring the vet now?

                He ate: {{food}}""")
        String check(@V("food") String food);
    }

    // ---------------------------------------------------------------- 6 — triage the worry
    // Routing where mis-routing is obviously expensive, which is the only kind worth a router.
    // "He ate a bar of dark chocolate" must not reach the trainer, and the room knows that
    // without being told — so it can judge the classifier itself.

    public interface WorryRouter {
        @Agent(description = "Sends the owner's worry to the one who can actually answer it")
        @UserMessage("""
                Classify this worry about a dog into one of: emergency, training, everyday.
                Anything the dog has eaten that could poison him, and anything about breathing,
                bleeding or collapse, is always emergency. Return one word only.

                Worry: {{worry}}""")
        String classify(@V("worry") String worry);
    }

    public interface EmergencyVet {
        @Agent(description = "Answers when the dog may be in danger right now")
        @UserMessage("""
                You are the emergency vet on the telephone. Say what the owner must do in the next
                ten minutes, and whether this is a get-in-the-car-now case. Be brief.

                Worry: {{worry}}""")
        String handle(@V("worry") String worry);
    }

    public interface DogTrainer {
        @Agent(description = "Answers questions about behaviour and training")
        @UserMessage("""
                You are the dog trainer. Give the owner one thing to change this week and one
                thing to stop doing. Be brief.

                Worry: {{worry}}""")
        String handle(@V("worry") String worry);
    }

    public interface EverydayCare {
        @Agent(description = "Answers the ordinary questions about living with a dog")
        @UserMessage("""
                You answer everyday dog questions — food, grooming, kit, routine. Give a short,
                practical answer.

                Worry: {{worry}}""")
        String handle(@V("worry") String worry);
    }

    // ---------------------------------------------------------------- 7 — supervisor sub-agents
    // Keep both names: MockChatModel's canned plan calls them literally.
    // The supervisor earns its place because the request does not say what it needs. "A baby
    // arrives in three months and the dog has never met one" might need the routine changed, the
    // training changed, or both — and no amount of thinking up front tells you which.

    public interface RoutinePlanner {
        @Agent(description = "Changes the dog's daily routine to fit what is coming")
        @UserMessage("""
                Plan the changes to the dog's daily routine — walks, feeding, where he sleeps,
                where he is when the house is busy. Be brief.

                Request: {{request}}""")
        String plan(@V("request") String request);
    }

    public interface TrainingPlanner {
        @Agent(description = "Plans what the dog needs to be taught before then")
        @UserMessage("""
                Plan what the dog needs to be taught, and in what order, before this happens. Be
                brief.

                Request: {{request}}""")
        String plan(@V("request") String request);
    }

    // ---------------------------------------------------------------- 8 — recall, in three steps
    // A precondition chain nobody has to be told about: you cannot practise recall at the park
    // before it works in the garden, and it will not work in the garden before it works indoors.
    // Three agents whose I/O keys only fit together one way — which is why the planner has
    // something to discover. They are deliberately registered in the WRONG order in the wiring.

    public interface IndoorRecall {
        @Agent(description = "The first step: recall indoors, with no distractions at all")
        @UserMessage("""
                Give the indoor step for teaching this: what the owner does, for how long, and how
                they know it is working. Two or three lines.

                Goal: {{goal}}""")
        String step(@V("goal") String goal);
    }

    public interface GardenRecall {
        @Agent(description = "The second step: recall in the garden, once indoors is solid")
        @UserMessage("""
                Give the garden step, which comes after the indoor step and must build on it. Say
                what changes and what to do if he ignores the call. Two or three lines.

                Indoor step already done: {{indoor}}
                Goal: {{goal}}""")
        String step(@V("indoor") String indoor, @V("goal") String goal);
    }

    public interface ParkRecall {
        @Agent(description = "The last step: recall at the park, once the garden is solid")
        @UserMessage("""
                Give the park step, which comes last because it is the hardest. Say what the long
                line is for and when the owner can finally drop it. Two or three lines.

                Garden step already done: {{garden}}
                Goal: {{goal}}""")
        String step(@V("garden") String garden, @V("goal") String goal);
    }

    // ---------------------------------------------------------------- 9 — the household argument
    // Two peers with opposed positions and NO authority over each other, which is the one
    // situation where peer-to-peer beats a supervisor: nobody can be told to give way, so the
    // only way out is a rule both will actually keep. Every household has had this argument.

    public interface TeamOnTheBed {
        @Agent(description = "Argues the dog should be allowed on the bed, and will not just fold")
        @UserMessage("""
                You are the half of the household that wants the dog on the bed. Say why, in a
                short paragraph, and say the one thing you will not give up.

                Question: {{question}}""")
        String propose(@V("question") String question);
    }

    public interface TeamOnTheFloor {
        @Agent(description = "Argues for the dog's own bed, and can settle or counter a proposal")
        @UserMessage("""
                You are the half of the household that wants the dog in his own bed. If the other
                half's proposal is one you could actually live with, write the house rule you both
                keep and end with the word AGREED. If it is not, counter it and say why it will
                not last a week.

                Their proposal: {{proposal}}""")
        String settle(@V("proposal") String proposal);
    }

    // ---------------------------------------------------------------- 10 — the barking problem
    // Debugging, which is what a blackboard is for. Three note-takers who each read ONLY the
    // problem, so any of them can go first and the board collects three different KINDS of
    // knowledge. Chain them instead and you have a sequence wearing a blackboard's coat.

    public interface WalkNotes {
        @Agent(description = "Adds what the dog's exercise explains about the problem")
        @UserMessage("""
                Add the exercise angle to the board: how much he actually gets, whether it is
                enough for his breed and age, and what you would try next. Two or three lines,
                exercise only — other people cover the rest.

                Problem: {{problem}}""")
        String add(@V("problem") String problem);
    }

    public interface RoutineNotes {
        @Agent(description = "Adds what changed in the household recently")
        @UserMessage("""
                Add the "what changed" angle to the board: new working hours, someone moved out, a
                different feeding time, a moved bed — and what you would try next. Two or three
                lines, changes only.

                Problem: {{problem}}""")
        String add(@V("problem") String problem);
    }

    public interface HomeNotes {
        @Agent(description = "Adds what the dog can see and hear from inside the house")
        @UserMessage("""
                Add the angle of what he can see and hear from indoors: the window onto the
                street, the post, next door's cat, deliveries — and what you would try next. Two
                or three lines, the house only.

                Problem: {{problem}}""")
        String add(@V("problem") String problem);
    }

    public interface TrainerLead {
        @Agent(description = "Reads the whole board and ranks the likely causes")
        @UserMessage("""
                You are the trainer. From everything on the board, give the two or three most
                likely causes, most likely first, and the one thing to try for each.

                Exercise: {{walks}}
                What changed: {{routine}}
                What he sees and hears: {{home}}""")
        String conclude(@V("walks") String walks, @V("routine") String routine,
                        @V("home") String home);
    }

    // ---------------------------------------------------------------- 11 — a second dog?
    // Three voters with DELIBERATELY DIFFERENT criteria over the same household, so they can
    // genuinely split — which is the only way a majority means anything. Three copies of one
    // prompt always agree, and then the tally is decoration.
    //
    // Each answers in one word, because that is what a voting strategy can tally: a one-line
    // reason per voter would make every answer unique and no majority could ever form. The
    // reasoning is still visible — the result pane shows all three votes side by side.

    public interface SpaceAndTime {
        @Agent(description = "Votes on a second dog on space and hours alone")
        @UserMessage("""
                Should this household get a second dog? Judge ONLY the space they have and the
                hours the dogs would be alone. Ignore money and ignore the dog they already have.
                Answer with one word: YES or LATER.

                Household: {{household}}""")
        String vote(@V("household") String household);
    }

    public interface MoneyAndVet {
        @Agent(description = "Votes on a second dog on what two dogs cost")
        @UserMessage("""
                Should this household get a second dog? Judge ONLY what a second dog costs —
                food, insurance, vet bills, boarding when they travel. Ignore everything else.
                Answer with one word: YES or LATER.

                Household: {{household}}""")
        String vote(@V("household") String household);
    }

    public interface AskZaoHimself {
        @Agent(description = "Votes on a second dog from the point of view of the dog they have")
        @UserMessage("""
                Should this household get a second dog? Judge ONLY from the point of view of the
                dog they already have — does he actually like other dogs? Ignore everything else.
                Answer with one word: YES or LATER.

                Household: {{household}}""")
        String vote(@V("household") String household);
    }

    // ---------------------------------------------------------------- 12 — take him, or not?
    // Both sides are genuinely strong, which is what makes a debate worth its tokens: a single
    // prompt picks one and then rationalises it, whereas a debate forces the case against the
    // winner to be said out loud before anyone rules. And the room can check the ruling, because
    // it has the same two lists of tradeoffs the panel does.

    public interface TakeHimAdvocate {
        @Agent(description = "Argues for taking the dog on the holiday")
        @UserMessage("""
                Argue for taking the dog along. Make your best case, then answer the strongest
                objection to taking him honestly rather than dodging it. Two or three sentences.

                Motion: {{motion}}""")
        String argue(@V("motion") String motion);
    }

    public interface LeaveHimAdvocate {
        @Agent(description = "Argues for leaving the dog at home with a sitter")
        @UserMessage("""
                Argue for leaving the dog at home with a sitter. Make your best case, then answer
                the strongest objection to leaving him honestly rather than dodging it. Two or
                three sentences.

                Motion: {{motion}}""")
        String argue(@V("motion") String motion);
    }

    public interface HolidayVerdict {
        @Agent(description = "Settles whether the dog comes on the holiday, and on what condition")
        @UserMessage("""
                Having heard both sides, rule whether the dog comes or stays. Name the one fact
                that decided it, and set one condition on the decision.

                Motion: {{motion}}""")
        String rule(@V("motion") String motion);
    }

    // ---------------------------------------------------------------- 13 — the puppy's first hour
    // Three desires whose PRIORITIES, not their declaration order, decide what happens. Nobody
    // needs to be told that a puppy goes to the garden before he gets a training session, or that
    // you feed him before you teach him anything — so the room can see the planner making the
    // right call rather than taking it on trust. Shuffle the declarations and nothing changes,
    // which is the point of BDI and impossible to show with two agents in the only order they
    // could ever have run.

    public interface ToiletTrip {
        @Agent(description = "Takes the puppy to the garden — before anything else, always")
        @UserMessage("""
                The puppy has just arrived. Take him out to the garden first. Say what the owner
                does, and what they do when he gets it right. Two or three lines.

                First hour: {{hour}}""")
        String take(@V("hour") String hour);
    }

    public interface FirstMeal {
        @Agent(description = "Gives the puppy his first meal, once he has been out")
        @UserMessage("""
                Now he has been out, give him his first meal in the new house: how much, where,
                and what the owner should not do while he eats. Two or three lines.

                Already been out: {{out}}""")
        String feed(@V("out") String out);
    }

    public interface FirstTraining {
        @Agent(description = "The first tiny training session, once he is out and fed")
        @UserMessage("""
                He has been out and he has eaten. Give the first tiny training session — one
                thing, two minutes, ending well. Two or three lines.

                Been out: {{out}}
                Eaten: {{fed}}""")
        String teach(@V("out") String out, @V("fed") String fed);
    }

    // ------------------------------------------------- 14 — the escalation ladder (custom planner)
    // Declared cheapest-first, because that order IS the policy in EscalationPlanner. Each one
    // ends with ANSWERED or ESCALATE, which is the only thing the planner reads: the model makes
    // a local judgement about its own competence, and the Java decides what that costs.
    // Everyone already knows this ladder — you look it up, then you ring the trainer, then you
    // ring the vet — and everyone knows you do not start at the vet to ask about kibble.

    public interface PuppyBook {
        @Agent(description = "The book on the shelf: free, instant, and only good for the basics")
        @UserMessage("""
                You are the puppy book on the shelf. Answer only if this is ordinary, settled
                information — food, kit, grooming, routine, house-training. Anything about how the
                dog behaves, or anything that might be a health problem, is past you.
                Answer in one or two sentences, then end with exactly one word on its own:
                ANSWERED if you fully covered it, or ESCALATE if you did not.

                Question: {{question}}""")
        String answer(@V("question") String question);
    }

    public interface TrainerOnCall {
        @Agent(description = "The trainer on the phone: costs a call, knows behaviour")
        @UserMessage("""
                You are the trainer, reached by telephone. Answer only if this is about behaviour
                or training — pulling, barking, recall, resource guarding, fear. Anything that
                might be pain, injury or illness is past you and belongs to the vet.
                Answer in two or three sentences, then end with exactly one word on its own:
                ANSWERED if you fully covered it, or ESCALATE if you did not.

                Question: {{question}}""")
        String answer(@V("question") String question);
    }

    public interface VetOnCall {
        @Agent(description = "The vet: the expensive last rung, and the only one who ends the ladder")
        @UserMessage("""
                You are the vet. You are the last rung of the ladder, so answer as best you can
                whatever is asked, and say plainly if the dog needs to be seen in person.
                Answer in two or three sentences, then end with exactly one word on its own:
                ANSWERED.

                Question: {{question}}""")
        String answer(@V("question") String question);
    }

    // ---------------------------------------------------------------- 15 — the weekend away
    // The capstone's own agents. The artefact is the one a household really does produce: a
    // single note on the fridge door that a sitter can follow without ringing you.

    public interface MealPlanner {
        @Agent(description = "Plans the dog's meals for the days the owners are away")
        @UserMessage("""
                Plan the dog's meals for the days the owners are away: times, amounts, and
                anything he must not be given. Be brief.

                The stay: {{stay}}""")
        String plan(@V("stay") String stay);
    }

    public interface WalkPlanner {
        @Agent(description = "Plans the dog's walks for the days the owners are away")
        @UserMessage("""
                Plan the dog's walks for the days the owners are away: when, how long, on or off
                the lead, and anywhere to avoid. Be brief.

                The stay: {{stay}}""")
        String plan(@V("stay") String stay);
    }

    public interface SitterNoteMerger {
        @Agent(description = "Merges the answer and the two plans into one note for the sitter")
        @UserMessage("""
                Write the note that goes on the fridge for the dog sitter. Put the thing that
                matters most at the top.

                What the expert said: {{answer}}
                Meals: {{meals}}
                Walks: {{walks}}""")
        String write(@V("answer") String answer, @V("meals") String meals,
                     @V("walks") String walks);
    }

    public interface NoteTightener {
        @Agent(description = "Tightens a sitter note without dropping any instruction")
        @UserMessage("""
                Tighten this note so it satisfies all four rules, without dropping any
                instruction: every meal has a time and an amount, it says where the lead and poo
                bags are, it gives the vet's telephone number, and it is under 100 words so it
                fits on the fridge door.

                Note: {{note}}""")
        String tighten(@V("note") String note);
    }

    // ---------------------------------------------------------------- 16 — the second-dog council
    // The same question the voting demo asks, but put through a whole council — so the room has
    // already met the three assessors and can watch them ratify a debated motion instead of
    // voting cold. Both agents below are "glue": each exists to hand one pattern's output to the
    // next in the shape that one expects. Composites need more of these than you expect, and they
    // are where the seams show.

    public interface AngleScout {
        @Agent(description = "Digs out what one angle of the household really says")
        @UserMessage("""
                Look at this household from one angle only. Say what it tells you and what is
                still unknown on it. Two sentences.

                Angle: {{angle}}""")
        String scout(@V("angle") String angle);
    }

    public interface CouncilBriefer {
        /**
         * {@code findings} is a {@code List}, not a String, because that is what the parallel
         * mapper writes into the scope — declare it as a String and the invocation dies with a
         * bare "argument type mismatch". Scope values are passed through as-is, never coerced.
         */
        @Agent(description = "Turns what the scouts found into the motion the council will weigh")
        @UserMessage("""
                Write the motion this household should decide on, as one sentence: a second dog or
                not, and if so what kind and when.

                Question: {{question}}
                What the scouts found: {{findings}}""")
        String brief(@V("question") String question, @V("findings") List<String> findings);
    }

    public interface SecondDogFor {
        @Agent(description = "Argues for the motion in front of the council")
        @UserMessage("""
                Argue for this motion, and answer the strongest objection to it honestly. Two or
                three sentences.

                Motion: {{motion}}""")
        String argue(@V("motion") String motion);
    }

    public interface SecondDogAgainst {
        @Agent(description = "Argues against the motion in front of the council")
        @UserMessage("""
                Argue against this motion, and answer the strongest point in its favour honestly.
                Two or three sentences.

                Motion: {{motion}}""")
        String argue(@V("motion") String motion);
    }

    public interface HouseholdVerdict {
        @Agent(description = "Rules on the council's motion, and on what condition")
        @UserMessage("""
                You chair the household council. Rule on the motion, name the one fact that
                decided it, and set one condition.

                Motion: {{motion}}""")
        String rule(@V("motion") String motion);
    }

    public interface CouncilNote {
        @Agent(description = "Restates the ruling as the household line the assessors will ratify")
        @UserMessage("""
                Restate this ruling as one line describing the household as it would be if the
                ruling is carried out, so the assessors can vote on it.

                Ruling: {{verdict}}""")
        String note(@V("verdict") String verdict);
    }
}
