package dev.devoxx.dashboard.demos._17_megamutt;

import static dev.devoxx.dashboard.support.Parsing.firstNumber;

import dev.devoxx.dashboard.demos._01_single.Keys.Location;
import dev.devoxx.dashboard.demos._08_nonaiagent.Keys.Height;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;

/**
 * Glue, and the reason it exists is the lesson of every composite: Sniff pins a SENTENCE, Rivet
 * needs a NUMBER, and neither of them is wrong. Reading "6 metres up the oak" into 6.0 is plain
 * Java, so it is a plain Java step — the two missions were never designed to meet.
 */
public class TapeMeasure {

    @Agent(name = "TapeMeasure",
           description = "Reads the height out of Sniff's location, in plain Java",
           typedOutputKey = Height.class)
    public static double measure(@K(Location.class) String location) {
        return firstNumber(location, 6.0);
    }
}
