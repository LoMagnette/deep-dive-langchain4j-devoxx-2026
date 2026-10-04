package dev.devoxx.dashboard.demos._17_megamutt;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static java.util.Objects.requireNonNullElse;

import java.util.List;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._01_single.Keys.Location;
import dev.devoxx.dashboard.demos._02_sequential.Keys.HealthReport;
import dev.devoxx.dashboard.demos._02_sequential.Keys.RescueStatus;
import dev.devoxx.dashboard.demos._02_sequential.SequentialPattern;
import dev.devoxx.dashboard.demos._03_loop.Keys.Feedback;
import dev.devoxx.dashboard.demos._08_nonaiagent.Keys.LadderLength;
import dev.devoxx.dashboard.demos._08_nonaiagent.Rivet;
import dev.devoxx.dashboard.demos._08_nonaiagent.ZoomFetchesLadder;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for <b>Mission 17</b> — three missions and two bits of glue, as one Ranger.
 */
public final class MegaMuttPattern {

    private MegaMuttPattern() {
    }

    /** The Gazette's own four rules, for Mission 3's critic. Same loop, different bar. */
    static final String GAZETTE_RULES =
            "1. it has a headline; 2. it says where the kitten was; 3. it says how the kitten is "
                    + "now; 4. it is under 60 words";

    static String run(ChatModel model, String input, StreamingListener listener) {
        // The wiring chooses Zoom's output key: in this sequence the ladder Zoom brings is the
        // rescue Doc reads, so it pins RescueStatus. Declared, the key lives on Zoom's own @Agent
        // (Ladder, from Mission 8) — so the composite rewrites that one contract here, with the
        // configurator the declarative API calls for every agent it builds, last.
        var zoomPinsTheRescue = new AgenticServices.AgentConfigurator(agent -> {
            if (agent.agentServiceClass() == ZoomFetchesLadder.class) {
                agent.agentBuilder().outputKey(RescueStatus.class);
            }
        });
        var r = CurrentRun.with(listener, () ->
                AgenticServices.createAgenticSystem(MegaMutt.class, model, zoomPinsTheRescue)
                        .rescue(input, GAZETTE_RULES, "(none yet — this is the first draft)"));
        var s = r.agenticScope();
        return "**The Barkville Gazette**\n\n" + r.result()
                + "\n\n---\n\n*Sniff:* " + requireNonNullElse(s.readState(Location.class), "")
                + "\n\n*Rivet:* the ladder must be " + s.readState(LadderLength.class) + " m"
                + "\n\n*Zoom:* " + requireNonNullElse(s.readState(RescueStatus.class), "")
                + "\n\n*Doc:* " + requireNonNullElse(s.readState(HealthReport.class), "")
                + "\n\n*Fifi's last word:* " + requireNonNullElse(s.readState(Feedback.class), "");
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // Stages, because no automatic layout recovers the real order of a composite. The two
        // glue steps are drawn as code: they are the part of a composite you actually write.
        Topology.Graph topo = graph("stages",
                List.of(node("in", "mission", "input", 0).withSub("kitten up the oak"),
                        node("sniff", "Sniff", "agent", 1).withSub("Mission 1").as("sniff"),
                        node("tape", "TapeMeasure", "code", 2).withSub("glue · sentence → m"),
                        node("rivet", "Rivet", "code", 3).withSub("Mission 8 · dropped in").as("rivet"),
                        node("zoom", "Zoom", "agent", 4).withSub("Mission 8").as("zoom"),
                        node("doc", "Doc", "agent", 5).withSub("Mission 2").as("doc"),
                        node("brief", "GazetteBrief", "code", 6).withSub("glue · pins → brief"),
                        node("howl", "Howl", "agent", 7).withSub("Mission 3 · loop").as("howl"),
                        node("fifi", "Fifi", "agent", 7).withSub("score ≥ 0.8 to exit").as("fifi")),
                List.of(edge("in", "sniff"),
                        edge("sniff", "tape", "location"),
                        edge("tape", "rivet", "height"),
                        edge("rivet", "zoom", "ladderLength"),
                        edge("zoom", "doc", "rescueStatus"),
                        edge("doc", "brief", "healthReport"),
                        edge("brief", "howl", "brief"),
                        edge("howl", "fifi", "draft"),
                        edge("fifi", "howl", "score < 0.8")));
        return new PatternDef("megaMutt", "The Mega Mutt (composite)", "composite",
                "Rangers combine! The kitten is back up the oak, and this time the whole team "
                        + "goes — as one very large dog.",
                "Mission 2's chain, with Mission 8's Rivet and Zoom dropped in and Mission 3's loop "
                        + "nested at the end.",
                "A system, not a pattern, and it exists to show that **the builders nest**: a "
                        + "loop is an agent, so it sits in a sequence like any Ranger. Mission 2's "
                        + "kitten rescue, with Rivet between Sniff and Zoom so the ladder is the "
                        + "right length, and Mission 3's write-and-score loop polishing the "
                        + "Gazette story — same Howl, same Fifi, different rules.",
                "Most of a composite is glue. Sniff pins a sentence and Rivet needs a number; the "
                        + "rescue pins a health report and the loop needs a brief — so two plain "
                        + "Java steps, `TapeMeasure` and `GazetteBrief`, do the joining. And Zoom's "
                        + "output key is chosen by this wiring, not by Zoom: the key is the "
                        + "contract, and a composite is where contracts get written.",
                topo, SequentialPattern.KITTEN, MegaMuttPattern::run)
                .gist("Workflows nest: earlier missions and a loop, inside one sequence.");
    }
}
