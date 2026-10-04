package dev.devoxx.dashboard.demos._00_aiservice;

import java.util.Locale;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.jboss.logging.Logger;

/**
 * The front desk's tools: plain Java the MODEL chooses to call. Who does which job, and who is
 * awake, are facts — a model asked would answer plausibly, and a roster answers correctly.
 */
public class DutyRoster {

    private static final Logger LOG = Logger.getLogger("gear");

    @Tool("Which Pawer Ranger does a kind of job: finding, fetching, digging, first aid, writing, judging, leading")
    public String rangerFor(@P(name = "job", value = "the kind of job, e.g. 'finding something lost'") String job) {
        String j = job == null ? "" : job.toLowerCase(Locale.ROOT);
        String who = j.contains("find") || j.contains("lost") || j.contains("search") ? "Sniff"
                : j.contains("fetch") || j.contains("deliver") || j.contains("run") ? "Zoom"
                : j.contains("dig") || j.contains("tunnel") || j.contains("hole") ? "Dig"
                : j.contains("hurt") || j.contains("aid") || j.contains("safe") ? "Doc"
                : j.contains("writ") || j.contains("poster") ? "Howl"
                : j.contains("judg") || j.contains("critic") ? "Fifi"
                : "Zao";
        LOG.infof("rangerFor(%s) → %s", job, who);
        return who;
    }

    @Tool("Whether a Ranger is on duty right now, and if not, when they will be")
    public String onDuty(@P(name = "ranger", value = "the Ranger's name, e.g. 'Sniff'") String ranger) {
        String r = ranger == null ? "" : ranger.strip().toLowerCase(Locale.ROOT);
        String status = switch (r) {
            case "sniff" -> "on duty, nose ready, can be there in ten minutes";
            case "zoom" -> "on duty, but chasing a squirrel — back in five minutes";
            case "dig" -> "napping until 15:00";
            case "doc" -> "on duty at the clinic";
            default -> "on duty";
        };
        LOG.infof("onDuty(%s) → %s", ranger, status);
        return status;
    }
}
