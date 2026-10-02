package dev.devoxx.dashboard.demos._13_voting;

import static dev.devoxx.dashboard.support.Parsing.firstNumber;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.devoxx.dashboard.demos._13_voting.Keys.Vote3;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;

/** Bolt votes by the book: 10 cm of clear ice holds a skating party, and less does not. */
public class BoltVotes {

    static final double SAFE_CM = 10.0;

    @Agent(name = "Bolt",
           description = "Bolt the robot dog: votes on the measured thickness, in plain Java",
           typedOutputKey = Vote3.class)
    public String vote(@K(Mission.class) String mission) {
        double cm = firstNumber(mission, 0);
        return (cm >= SAFE_CM ? "SAFE" : "NOT SAFE") + " — measured " + cm + " cm, and the rule is "
                + SAFE_CM + " cm.";
    }
}
