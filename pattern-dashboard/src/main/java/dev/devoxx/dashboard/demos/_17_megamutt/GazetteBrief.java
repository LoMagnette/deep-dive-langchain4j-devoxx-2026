package dev.devoxx.dashboard.demos._17_megamutt;

import dev.devoxx.dashboard.demos._01_single.Keys.Location;
import dev.devoxx.dashboard.demos._02_sequential.Keys.HealthReport;
import dev.devoxx.dashboard.demos._03_loop.Keys.Brief;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;

/**
 * More glue: Mission 3's loop expects a {@code Brief}, and the rescue produced a location and a
 * health report. Two pins in, one pin out, no model — the adapter between two patterns that were
 * built without knowing about each other.
 */
public class GazetteBrief {

    @Agent(name = "GazetteBrief",
           description = "Turns the rescue's pins into the brief the Gazette loop expects",
           typedOutputKey = Brief.class)
    public static String brief(@K(Location.class) String location,
                        @K(HealthReport.class) String health) {
        return "The Barkville Gazette story of today's rescue: a headline, then three sentences. "
                + "Where: " + location + " How: " + health;
    }
}
