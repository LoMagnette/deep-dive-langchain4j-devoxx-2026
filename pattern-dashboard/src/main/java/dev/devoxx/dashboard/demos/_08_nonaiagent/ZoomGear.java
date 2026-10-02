package dev.devoxx.dashboard.demos._08_nonaiagent;

import static dev.devoxx.dashboard.support.Parsing.firstNumber;

import java.util.List;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.jboss.logging.Logger;

/** Zoom's gear. The fire-station shed holds four ladders, and only four. */
public class ZoomGear {

    private static final Logger LOG = Logger.getLogger("gear");

    /** What is in the shed, in metres. */
    private static final List<Double> LADDERS = List.of(5.0, 7.5, 10.0, 15.0);

    @Tool("Fetch a ladder from the fire-station shed. The shed has a 5 m, a 7.5 m, a 10 m and a 15 m ladder")
    public String fetch(@P(name = "item", value = "what to fetch, e.g. '7.5 m ladder'") String item) {
        // A ladder the shed does not have is answered with the next size up, never down: the
        // shed is the source of truth, and a ladder that is too short is not a ladder.
        double asked = firstNumber(item, 0);
        String got = LADDERS.stream().filter(l -> l >= asked).findFirst()
                .map(l -> "the " + (l % 1 == 0 ? String.valueOf(l.intValue()) : String.valueOf(l))
                        + " m ladder")
                .orElse("nothing — the shed has no ladder that long");
        String said = "fetched " + got + " from the fire-station shed";
        LOG.infof("fetch(%s) → %s", item, said);
        return said;
    }

    @Tool("Deliver an item to a place in Barkville")
    public String deliver(@P(name = "item", value = "what to deliver") String item,
                          @P(name = "place", value = "where to take it") String place) {
        String said = "delivered " + item + " to " + place + ", leaning against it, ready to climb";
        LOG.infof("deliver(%s, %s) → %s", item, place, said);
        return said;
    }
}
