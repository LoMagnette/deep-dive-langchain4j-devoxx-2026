package dev.devoxx.dashboard.demos._16_customplanner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import dev.devoxx.dashboard.demos._06_conditional.Keys.Call;
import dev.langchain4j.agentic.planner.Action;
import dev.langchain4j.agentic.planner.AgentInstance;
import dev.langchain4j.agentic.planner.InitPlanningContext;
import dev.langchain4j.agentic.planner.Planner;
import dev.langchain4j.agentic.planner.PlanningContext;
import dev.langchain4j.agentic.scope.AgenticScope;

/**
 * Zao's rule, in plain Java. The whole policy is the loop in {@link #nextAction}: hungry → feed;
 * energy above 70 and not last on a mission → go; otherwise → nap. Feeding and napping change the
 * board and call nobody, so the loop keeps going until somebody is sent or the day is over.
 */
public final class NapSchedule implements Planner {

    private static final int MISSION_COST = 40, NAP = 40, FOOD = 20, BEDTIME = 12;
    private static final Pattern ENERGY = Pattern.compile("(Sniff|Zoom|Dig|Doc)\\D{0,3}(\\d+)");
    private static final Pattern HUNGRY = Pattern.compile("(Sniff|Zoom|Dig|Doc) is hungry");

    private final Map<String, AgentInstance> rangers = new LinkedHashMap<>();
    private final Map<String, Integer> energy = new LinkedHashMap<>();
    private final Set<String> hungry = new HashSet<>();
    private final List<String> queue = new ArrayList<>();
    private final List<String> schedule = new ArrayList<>();
    private String last;
    private int steps;
    private boolean read;

    @Override
    public void init(InitPlanningContext context) {
        context.subagents().forEach(a -> rangers.put(a.name(), a));
    }

    @Override
    public Action nextAction(PlanningContext context) {
        AgenticScope board = context.agenticScope();
        if (!read) {
            readTheBoard(board.readState(Keys.Roster.class));
        }
        var report = context.previousAgentInvocation();
        if (report != null) {
            schedule.add("   " + report.agentName() + " reports: " + report.output());
        }
        while (steps++ < BEDTIME) {
            if (queue.isEmpty()) {
                return finish(board, "Queue empty. Everybody naps.");
            }
            String mission = nextMission();
            String pup = whoDoes(mission);
            if (hungry.remove(pup)) {
                energy.merge(pup, FOOD, Integer::sum);
                schedule.add("FEED  " + pup + " is hungry → fed (+" + FOOD + ")");
            } else if (energy.get(pup) > 70 && !pup.equals(last)) {
                queue.remove(mission);
                energy.merge(pup, -MISSION_COST, Integer::sum);
                last = pup;
                schedule.add("GO    " + pup + " → " + mission);
                board.writeState(Call.class, mission);
                pin(board);
                return call(rangers.get(pup));
            } else {
                energy.merge(pup, NAP, Integer::sum);
                if (pup.equals(last)) {
                    last = null;   // a nap breaks the run: rested, he may go again
                }
                schedule.add("NAP   " + pup + " naps (+" + NAP + ")");
            }
            pin(board);
        }
        return finish(board, "Bedtime. Whatever is left waits for tomorrow.");
    }

    /** The first mission someone OTHER than the last Ranger can do; else simply the first. */
    private String nextMission() {
        return queue.stream().filter(m -> !whoDoes(m).equals(last)).findFirst()
                .orElse(queue.get(0));
    }

    /** One job per Ranger, and the name says it: Sniff finds, Zoom fetches, Dig digs, Doc checks. */
    private static String whoDoes(String mission) {
        String m = mission.toLowerCase(Locale.ROOT);
        if (m.contains("dig") || m.contains("drain") || m.contains("hole")) return "Dig";
        if (m.contains("fetch") || m.contains("deliver") || m.contains("bring")) return "Zoom";
        if (m.contains("check") || m.contains("hurt") || m.contains("puppy")) return "Doc";
        return "Sniff";
    }

    private Action finish(AgenticScope board, String why) {
        schedule.add(why);
        pin(board);
        return done(String.join("\n", schedule));
    }

    /** Everything the planner knows, back on the board where the Scope tab can show it. */
    private void pin(AgenticScope board) {
        board.writeState(Keys.Energy.class, energy.toString());
        board.writeState(Keys.LastOnMission.class, last == null ? "nobody" : last);
        board.writeState(Keys.MissionQueue.class, String.join("; ", queue));
        board.writeState(Keys.Schedule.class, String.join("\n", schedule));
    }

    private void readTheBoard(String roster) {
        read = true;
        String text = roster == null ? "" : roster;
        int at = text.toLowerCase(Locale.ROOT).indexOf("missions:");
        int end = text.toLowerCase(Locale.ROOT).indexOf("energy:");
        if (at >= 0) {
            String list = text.substring(at + "missions:".length(), end > at ? end : text.length());
            for (String m : list.split("[;\\n]")) {
                if (!m.isBlank()) queue.add(m.strip().replaceAll("\\.$", ""));
            }
        }
        rangers.keySet().forEach(r -> energy.put(r, 50));
        ENERGY.matcher(text).results().forEach(r -> energy.put(r.group(1), Integer.parseInt(r.group(2))));
        HUNGRY.matcher(text).results().forEach(r -> hungry.add(r.group(1)));
    }
}
