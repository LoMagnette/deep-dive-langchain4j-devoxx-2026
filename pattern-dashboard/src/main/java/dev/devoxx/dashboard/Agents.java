package dev.devoxx.dashboard;

import java.util.List;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * All agent contracts used by the dashboard. The setting is a working boarding kennel and rescue
 * in the Ardennes, where the Belgian shepherd <b>Zao</b> is the resident dog — so the talk's
 * "From Puppy to Pack" thread still holds, but every demo now has an operation behind it.
 *
 * <p>The setting is doing real work, not decoration. A kennel has <b>hard constraints</b>
 * (capacity, vaccination rules, medication times), <b>competing interests</b> (revenue vs.
 * welfare, two families wanting the same rescue dog) and <b>artefacts that can be wrong in ways
 * the room can see</b> (a discharge note that forgets a dose, a call routed to the booking desk
 * when the dog has bloat). That is what makes each pattern load-bearing: take the pattern away
 * and the answer visibly degrades, which a "write me a story about a dog" demo can never show.
 *
 * <p>The names are part of the demo: a room reading {@code NightLineRouter → EmergencyVet}
 * understands conditional routing before the speaker explains it, in a way {@code Router →
 * ExpertB} never manages.
 *
 * <p>The {@code @UserMessage} prompts are worded so the deterministic {@link MockChatModel}
 * returns parseable output (scores, labels, PASS/FAIL lines, votes). Change the wording and check
 * the mock: its branches key off words like "0.0", "classify", "PLACE or HOLD", "vaccination".
 */
public final class Agents {

    private Agents() {
    }

    // ---------------------------------------------------------------- 1 / 2 — intake
    // The kennel's first job of the day, and the one everything downstream is built on: turn a
    // hurried hand-over at the door into a record the night shift can act on.

    public interface IntakeClerk {
        @Agent(description = "Turns a hurried drop-off note into a structured boarding record")
        @UserMessage("""
                You are on the desk of an Ardennes boarding kennel. Turn this drop-off note into a
                boarding record with exactly these five lines, and invent nothing that is not in
                the note — write "not given" instead:
                Dog:
                Stay:
                Medication:
                Feeding:
                Flags:

                Note: {{note}}""")
        String record(@V("note") String note);
    }

    public interface RunSheetWriter {
        @Agent(description = "Turns a boarding record into the run sheet the kennel hand carries")
        @UserMessage("""
                Turn this boarding record into the run sheet a kennel hand carries on the round.
                Give the handling instruction first, then the timed tasks in order. Keep it under
                60 words and use no abbreviations.

                Record: {{record}}""")
        String write(@V("record") String record);
    }

    // ---------------------------------------------------------------- 3 — the refinement loop
    // The critic scores against FOUR NAMED RULES rather than taste. That is the whole point: the
    // room can check the draft against the same four rules and watch the loop fix the one it
    // missed. A critic scoring "quality" out of 1.0 teaches nothing, because nobody watching can
    // tell 0.6 from 0.9.

    public interface DischargeWriter {
        @Agent(description = "Rewrites go-home instructions until they satisfy the kennel's rules")
        @UserMessage("""
                Rewrite these go-home instructions so an owner with no medical training can follow
                them. All four rules must hold:
                1. every medicine is named with its dose and the times of day it is given
                2. under 90 words
                3. no clinical jargon
                4. the last line is exactly: Call the kennel on 061 22 33 44 if anything worries you.

                Instructions: {{draft}}""")
        String rewrite(@V("draft") String draft);
    }

    /**
     * Returns the score as a {@code String}, not a {@code double}, on purpose: a real chat model
     * answers "I'd rate this **0.85** out of 1.0" often enough that a {@code double} return type
     * blows up the whole loop with an OutputParsingException. The caller extracts the number
     * defensively — see {@link Parsing#score}.
     */
    public interface DischargeChecker {
        @Agent(description = "Checks go-home instructions against the kennel's four rules")
        @UserMessage("""
                Check these instructions against the four rules: every medicine named with dose
                and times, under 90 words, no clinical jargon, and the last line is exactly
                "Call the kennel on 061 22 33 44 if anything worries you." Give the fraction of
                rules that hold as a number from 0.0 to 1.0 — the number only, no words, no
                explanation, no markdown.

                Instructions: {{draft}}""")
        String check(@V("draft") String draft);
    }

    // ---------------------------------------------------------------- 4 — the parallel checks
    // Two checks that genuinely do not need each other, and both of which must finish before the
    // kennel can answer at all. That is fan-out and join, not two things that happen to be
    // convenient to run together — and the join is a DECISION (any FAIL declines the booking),
    // not a string concatenation.

    public interface CapacityCheck {
        @Agent(description = "Says whether a free kennel run of the right size exists for the dates")
        @UserMessage("""
                The kennel has 11 runs: 4 large, 5 medium, 2 small. Decide whether a suitable run
                is free for this booking. Answer with one line starting PASS or FAIL, then the
                reason — which run, or what clashes.

                Booking: {{booking}}""")
        String check(@V("booking") String booking);
    }

    public interface HealthCheck {
        @Agent(description = "Says whether the paperwork and medication are within house rules")
        @UserMessage("""
                House rules: rabies and kennel-cough vaccination must be valid and given at least
                21 days before arrival; staff may give tablets and liquids but never injections.
                Decide whether this booking is admissible. Answer with one line starting PASS or
                FAIL, then the reason.

                Booking: {{booking}}""")
        String check(@V("booking") String booking);
    }

    // ---------------------------------------------------------------- 5 — the morning round
    // The same inspection over every occupied run. The list is data, so the fan-out width is
    // decided at run time — which is also where the cost caveat bites: 30 runs is 30 calls.

    public interface RunInspector {
        @Agent(description = "Reads one run's overnight notes and flags what the shift must act on")
        @UserMessage("""
                Read one kennel run's overnight notes and give a single line for the shift's
                watch-list: the run, then either "no action" or what to do first. Treat not eating,
                not passing stool, retching, or a swollen belly as urgent.

                Overnight notes: {{notes}}""")
        String inspect(@V("notes") String notes);
    }

    // ---------------------------------------------------------------- 6 — the night line
    // Routing where mis-routing is dangerous, which is the only kind worth a router. A swollen,
    // retching dog is bloat: routed to the booking desk it dies overnight. The room knows the
    // right answer, so it can judge the classifier — and the caveat about unseen categories
    // stops being abstract.

    public interface NightLineRouter {
        @Agent(description = "Triages an out-of-hours call to the desk that can answer it")
        @UserMessage("""
                You triage the out-of-hours line of a boarding kennel. Classify this call into one
                of: emergency, behaviour, booking. A dog in physical distress is always emergency.
                Return one word only.

                Call: {{call}}""")
        String classify(@V("call") String call);
    }

    public interface EmergencyVet {
        @Agent(description = "Handles calls where the dog may be in physical danger")
        @UserMessage("""
                You are the on-call vet for a boarding kennel. Say what the handler must do in the
                next ten minutes, and whether this is a drive-to-the-clinic-now case. Be brief.

                Call: {{call}}""")
        String handle(@V("call") String call);
    }

    public interface BehaviourDesk {
        @Agent(description = "Handles calls about how a boarded dog is coping or behaving")
        @UserMessage("""
                You run the behaviour desk of a boarding kennel. Give the handler one thing to
                change tonight and one thing to write in the record. Be brief.

                Call: {{call}}""")
        String handle(@V("call") String call);
    }

    public interface BookingDesk {
        @Agent(description = "Handles calls about dates, prices and paperwork")
        @UserMessage("""
                You run the booking desk of a boarding kennel. Answer the question and name any
                paperwork the owner must bring. Be brief.

                Call: {{call}}""")
        String handle(@V("call") String call);
    }

    // ---------------------------------------------------------------- 7 — supervisor sub-agents
    // Keep both names: MockChatModel's canned plan calls them literally.
    // The supervisor earns its place here because the request does not say which of these is
    // needed — an owner phoning about a dog off his food may need the feed plan, the rota, or
    // both, and enumerating that in advance is exactly what you cannot do.

    public interface RotaPlanner {
        @Agent(description = "Plans a boarded dog's exercise and handling slots for the week")
        @UserMessage("""
                Plan the exercise and handling slots for one boarded dog's week at the kennel,
                taking any handling restriction into account. Be brief.

                Request: {{request}}""")
        String plan(@V("request") String request);
    }

    public interface FeedPlanner {
        @Agent(description = "Plans a boarded dog's feeding and medication times for the week")
        @UserMessage("""
                Plan the feeding and medication times for one boarded dog's week at the kennel,
                keeping medicines with or away from food as required. Be brief.

                Request: {{request}}""")
        String plan(@V("request") String request);
    }

    // ---------------------------------------------------------------- 8 — the quote chain (GOAP)
    // A real precondition chain: you cannot price a stay before a run is allocated, and you
    // cannot allocate a run before the vaccination status is known. Three agents whose I/O keys
    // only fit together one way — which is why the planner has something to discover. They are
    // deliberately registered in the WRONG order in the wiring; GOAP still runs them correctly.

    public interface VaccinationAuditor {
        @Agent(description = "Establishes the vaccination status of a booking request")
        @UserMessage("""
                State the vaccination status for this booking in one line: valid, expired, or not
                given, with the date if there is one. House rule: shots must be at least 21 days
                before arrival.

                Request: {{request}}""")
        String audit(@V("request") String request);
    }

    public interface RunAllocator {
        @Agent(description = "Allocates a kennel run, once the vaccination status is known")
        @UserMessage("""
                The kennel has 11 runs: 4 large, 5 medium, 2 small. Allocate a run for this
                booking in one line, or refuse it. A dog whose vaccination is not valid may not be
                allocated a run at all.

                Vaccination status: {{vaccination}}
                Request: {{request}}""")
        String allocate(@V("vaccination") String vaccination, @V("request") String request);
    }

    public interface QuotePricer {
        @Agent(description = "Works out what the stay costs, once a run has been allocated")
        @UserMessage("""
                Work out the total cost of this stay: a large run is 28 euro a night, medium 24,
                small 20, plus 5 euro a day when staff give medication. Show the arithmetic in one
                line. If no run was allocated, say the stay cannot be quoted and why.

                Allocated run: {{run}}
                Request: {{request}}""")
        String quote(@V("run") String run, @V("request") String request);
    }

    // ---------------------------------------------------------------- 9 — peer negotiation (P2P)
    // Two peers with genuinely opposed mandates and NO authority over each other — which is the
    // one situation where peer-to-peer beats a supervisor. Nobody can be told to give way, so
    // the only way out is a plan both can sign.

    public interface KennelForeman {
        @Agent(description = "Speaks for keeping the kennel full and the bookings honoured")
        @UserMessage("""
                You are the kennel foreman. You are judged on honouring every booking you took.
                Propose how to handle this, in one short paragraph, and say what you will not give
                up.

                Problem: {{issue}}""")
        String propose(@V("issue") String issue);
    }

    public interface WelfareOfficer {
        @Agent(description = "Speaks for the dogs' welfare and can settle or counter a proposal")
        @UserMessage("""
                You are the welfare officer. Dogs may not be doubled up unless they already live
                together, and no dog goes more than four hours without a check. If the foreman's
                proposal is acceptable, write the agreed plan and end with the word AGREED. If it
                is not, counter it and say which rule it breaks.

                Proposal: {{plan}}""")
        String settle(@V("plan") String plan);
    }

    // ---------------------------------------------------------------- 10 — the blackboard
    // Three note-takers who each read ONLY the problem, so any of them can go first and the board
    // accumulates three different KINDS of knowledge. That is a blackboard. The earlier version
    // chained facts → analysis → solution, which is a sequence wearing a blackboard's coat.

    public interface MedicalNotes {
        @Agent(description = "Adds what the medical file says about the dog")
        @UserMessage("""
                Add the medical angle to the case board: temperature, wounds, medicines and their
                side effects, and what you would check next. Two or three lines, medical only —
                another expert covers behaviour.

                Case: {{problem}}""")
        String add(@V("problem") String problem);
    }

    public interface BehaviourNotes {
        @Agent(description = "Adds what kennel stress and the dog's neighbours explain")
        @UserMessage("""
                Add the behaviour angle to the case board: kennel stress, the neighbouring dogs,
                noise, routine change, and what you would check next. Two or three lines,
                behaviour only — another expert covers the medical side.

                Case: {{problem}}""")
        String add(@V("problem") String problem);
    }

    public interface FeedNotes {
        @Agent(description = "Adds what the feeding log explains")
        @UserMessage("""
                Add the feeding angle to the case board: food change, portion, timing, who feeds
                and whether the bowl is guarded, and what you would check next. Two or three
                lines, feeding only.

                Case: {{problem}}""")
        String add(@V("problem") String problem);
    }

    public interface VetLead {
        @Agent(description = "Reads the whole board and writes the ranked differential")
        @UserMessage("""
                You are the kennel's lead vet. From everything on the case board, rank the two or
                three most likely causes, most likely first, and give the single next check for
                each.

                Medical: {{medical}}
                Behaviour: {{behaviour}}
                Feeding: {{feed}}""")
        String conclude(@V("medical") String medical, @V("behaviour") String behaviour,
                        @V("feed") String feed);
    }

    // ---------------------------------------------------------------- 11 — the placement vote
    // Three assessors reading the SAME dossier against DELIBERATELY DIFFERENT criteria, so they
    // can genuinely split — which is the only way a majority vote means anything. Three copies of
    // one prompt (the earlier MoodSnifferA/B/C) always agree, so the tally was decoration.
    //
    // Each answers with one word, because that is what a voting strategy can actually tally: a
    // one-line reason per voter would make every answer unique and no majority could ever form.
    // The disagreement is still visible — the Run events show what each assessor was asked.

    public interface TemperamentAssessor {
        @Agent(description = "Votes on a placement from the temperament test alone")
        @UserMessage("""
                You assess rescue dogs for placement in a home with a small child. Judge ONLY the
                temperament test in this dossier and ignore every other source. Answer with one
                word: PLACE or HOLD.

                Dossier: {{dossier}}""")
        String vote(@V("dossier") String dossier);
    }

    public interface FosterAssessor {
        @Agent(description = "Votes on a placement from the foster carer's diary alone")
        @UserMessage("""
                You assess rescue dogs for placement in a home with a small child. Judge ONLY the
                foster carer's diary in this dossier — how the dog behaves over weeks in a real
                house — and ignore every other source. Answer with one word: PLACE or HOLD.

                Dossier: {{dossier}}""")
        String vote(@V("dossier") String dossier);
    }

    public interface MedicalAssessor {
        @Agent(description = "Votes on a placement from the medical file alone")
        @UserMessage("""
                You assess rescue dogs for placement in a home with a small child. Judge ONLY the
                medical file in this dossier, on the principle that a dog in pain is a dog that
                may bite. Ignore every other source. Answer with one word: PLACE or HOLD.

                Dossier: {{dossier}}""")
        String vote(@V("dossier") String dossier);
    }

    // ---------------------------------------------------------------- 12 — the placement debate
    // Two homes have applied for the same rescue dog and only one can have him, so the two sides
    // are not a rhetorical exercise — each advocate has real facts on its side and real problems
    // to explain away. A single prompt would pick one home and rationalise it; the debate forces
    // the counter-case to be stated out loud, which is the whole value. And the panel's verdict
    // is checkable: the room has the same constraints in front of it.

    public interface HomeOneAdvocate {
        @Agent(description = "Argues for placing the dog with the first applicant home")
        @UserMessage("""
                You are the case worker for the FIRST home in this placement decision. Argue why
                the dog should go to them, and answer the strongest objection to them honestly.
                Two or three sentences.

                Motion: {{motion}}""")
        String argue(@V("motion") String motion);
    }

    public interface HomeTwoAdvocate {
        @Agent(description = "Argues for placing the dog with the second applicant home")
        @UserMessage("""
                You are the case worker for the SECOND home in this placement decision. Argue why
                the dog should go to them, and answer the strongest objection to them honestly.
                Two or three sentences.

                Motion: {{motion}}""")
        String argue(@V("motion") String motion);
    }

    public interface PlacementPanel {
        @Agent(description = "Rules which home the dog is placed with, and on what condition")
        @UserMessage("""
                You chair the placement panel. Having heard both case workers, rule which home the
                dog goes to, name the one fact that decided it, and set one condition on the
                placement.

                Motion: {{motion}}""")
        String rule(@V("motion") String motion);
    }

    // ---------------------------------------------------------------- 13 — the morning shift (BDI)
    // Three desires whose PRIORITIES, not their declaration order, decide what happens: the
    // safety round outranks everything, medication is only achievable once every dog has been
    // looked at, and the report is only achievable once both are done. Shuffle the declarations
    // and the behaviour does not change — which is the point of BDI and impossible to show with
    // two agents in the only order they could ever run.

    public interface SafetyRound {
        @Agent(description = "Walks every occupied run and flags anything wrong (top priority)")
        @UserMessage("""
                Walk every occupied run and report, in one line each, any dog that needs attention
                before anything else happens this shift.

                Shift: {{shift}}""")
        String walk(@V("shift") String shift);
    }

    public interface MedsRound {
        @Agent(description = "Gives the morning medication, but only after the safety round")
        @UserMessage("""
                Give the morning medication round, taking the safety round into account: a dog
                flagged as unwell is not medicated before the vet is called. One line per dog.

                Safety round: {{round}}""")
        String give(@V("round") String round);
    }

    public interface ShiftReport {
        @Agent(description = "Writes the shift report, once the rounds are done")
        @UserMessage("""
                Write the shift report in under 70 words: what was found, what was given, and what
                the next shift must pick up.

                Safety round: {{round}}
                Medication round: {{meds}}""")
        String write(@V("round") String round, @V("meds") String meds);
    }

    // ---------------------------------------------------------------- 14 — the night handover
    // The capstone's own agents. The artefact is the thing a kennel really does produce at the
    // end of a call: one handover sheet for the night hand, checked against rules before it is
    // handed over.

    public interface HandoverWriter {
        @Agent(description = "Merges the desk's answer and the day's plans into one handover sheet")
        @UserMessage("""
                Write the night handover sheet for the kennel hand coming on shift. Put what to do
                first at the top.

                What the desk advised: {{answer}}
                Exercise and handling: {{rota}}
                Feeding and medication: {{feed}}""")
        String write(@V("answer") String answer, @V("rota") String rota, @V("feed") String feed);
    }

    public interface HandoverEditor {
        @Agent(description = "Tightens a handover sheet without dropping any instruction")
        @UserMessage("""
                Tighten this handover sheet so it satisfies all three rules, without dropping any
                instruction:
                1. it names the dog and its run
                2. every medicine appears with its dose and time
                3. it says who to telephone, and when

                Sheet: {{sheet}}""")
        String edit(@V("sheet") String sheet);
    }

    /** Returns a String for the same reason {@link DischargeChecker} does — see its javadoc. */
    public interface HandoverChecker {
        @Agent(description = "Checks a handover sheet against the kennel's three rules")
        @UserMessage("""
                Check this handover sheet against three rules: it names the dog and its run, every
                medicine appears with dose and time, and it says who to telephone and when. Give
                the fraction of rules that hold as a number from 0.0 to 1.0 — the number only, no
                words, no explanation, no markdown.

                Sheet: {{sheet}}""")
        String check(@V("sheet") String sheet);
    }

    // ---------------------------------------------------------------- 15 — the placement council
    // Both of these are "glue": each exists to hand one pattern's output to the next in the shape
    // that one expects. Composites need more of these than you expect, and they are where the
    // seams show.

    public interface CaseScout {
        @Agent(description = "Digs out what one angle of a rescue dossier actually says")
        @UserMessage("""
                Pull out what this dossier says on one angle only, and what is missing on it. Two
                sentences.

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
                Write the motion the placement council should vote on, as one sentence naming the
                home you propose and the condition attached.

                Question: {{question}}
                What the scouts found: {{findings}}""")
        String brief(@V("question") String question, @V("findings") List<String> findings);
    }

    public interface CouncilNote {
        @Agent(description = "Restates the panel's ruling as the dossier line to be ratified")
        @UserMessage("""
                Restate the panel's ruling as one dossier line for the assessors to ratify, naming
                the home and the condition.

                Ruling: {{verdict}}""")
        String note(@V("verdict") String verdict);
    }
}
