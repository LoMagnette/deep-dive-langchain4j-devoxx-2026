package dev.devoxx.dashboard.demos._18_lakeparty;

import java.util.List;
import java.util.stream.IntStream;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;

/**
 * The adapter, and the price of reuse said out loud: Mission 13's voters read {@code Mission},
 * because that is the key they declared. So the inspection is written back INTO the mission —
 * the original words first, so Rivet's ruler still finds the measured thickness, then every spot.
 */
public class IceReport {

    @Agent(name = "IceReport",
           description = "Writes the spot checks into the mission the voters read, in plain Java",
           typedOutputKey = Mission.class)
    public static String report(@K(Mission.class) String mission, @K(Keys.Findings.class) List<String> findings,
                         @K(Keys.Spots.class) List<String> spots) {
        StringBuilder out = new StringBuilder(mission).append("\nSpot checks:");
        IntStream.range(0, findings.size()).forEach(i -> out.append("\n- ")
                .append(i < spots.size() ? spots.get(i) : "spot " + i).append(": ")
                .append(findings.get(i)));
        return out.toString();
    }
}
