package dev.devoxx.dashboard.demos._01_single;

import java.util.Locale;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.jboss.logging.Logger;

/**
 * Sniff's gear: two plain Java methods the MODEL may call. Nothing in the wiring says which one
 * goes first or how often — that is the whole of Mission 1. The answers are canned Barkville
 * facts, because a tool is where facts come from: a model asked where the hat is would invent a
 * place, and a model handed a nose finds the one that is true.
 *
 * <p>{@code @P(name = ...)} because the build does not keep parameter names: without it the
 * model is offered {@code arg0}, which a small model fills with anything.
 */
public class SniffGear {

    private static final Logger LOG = Logger.getLogger("gear");

    @Tool("Sniff one place in Barkville and report every scent there, and where any trail leads")
    public String sniff(@P(name = "place", value = "the place to sniff, e.g. 'the park bench'")
                        String place) {
        String p = place == null ? "" : place.toLowerCase(Locale.ROOT);
        String found;
        if (p.contains("bench") || p.contains("park")) {
            found = "the Mayor's scent, very strong, and a hat-shaped gap in the dust. "
                    + "A trail of green feather fluff leads east.";
        } else if (p.contains("oak") || p.contains("tree")) {
            found = "kitten! 6 metres up the oak, on the north branch, clinging on and very cross.";
        } else if (p.contains("carousel") || p.contains("fair")) {
            found = "a small child's scent, toffee apple and tears, heading for the carousel.";
        } else if (p.contains("pond")) {
            found = "ducks, bread, and something felt-and-feathery floating in the middle.";
        } else {
            found = "grass, pigeon, and a cat that passed by an hour ago. Nothing else.";
        }
        LOG.infof("sniff(%s) → %s", place, found);
        return found;
    }

    @Tool("Follow one scent trail to wherever it ends, and say what is there")
    public String followTrail(@P(name = "scent", value = "the scent to follow, e.g. 'feather fluff'")
                              String scent) {
        String s = scent == null ? "" : scent.toLowerCase(Locale.ROOT);
        String found = s.contains("feather") || s.contains("hat") || s.contains("fluff")
                ? "the trail ends at the duck pond. The Mayor's hat is floating in the middle. "
                        + "A duck is sitting in it, and does not want to leave."
                : "the trail goes round the town hall twice and gives up.";
        LOG.infof("followTrail(%s) → %s", scent, found);
        return found;
    }
}
