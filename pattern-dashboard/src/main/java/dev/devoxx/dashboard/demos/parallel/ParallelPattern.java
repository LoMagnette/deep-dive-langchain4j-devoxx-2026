package dev.devoxx.dashboard.demos.parallel;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static dev.devoxx.dashboard.support.Wiring.agent;
import static dev.devoxx.dashboard.support.Wiring.result;
import static dev.devoxx.dashboard.support.Wiring.str;
import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.PatternDef.Runner;
import dev.devoxx.dashboard.catalog.Topology;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;

/**
 * Wiring for the <b>parallel</b> demo — two independent checks, and a join that DECIDES.
 */
public final class ParallelPattern {

    private ParallelPattern() {
    }

    public static PatternDef define() {
        Topology.Graph topo = graph("fanout",
                List.of(node("in", "right now", "input"),
                        node("weather", "WeatherCheck", "agent"),
                        node("dog", "DogCheck", "agent"),
                        // The combiner is the whole second half of "fan out, then join" — and
                        // here it is a rule, not a concatenation: either check can veto the walk.
                        node("join", "either can veto", "join")),
                List.of(edge("in", "weather"), edge("in", "dog"),
                        edge("weather", "join", "weather"), edge("dog", "join", "dog")));
        Runner runner = (model, input, listener) -> {
            var weather = agent(WeatherCheck.class, model, "WeatherCheck", "weather");
            var dog = agent(DogCheck.class, model, "DogCheck", "dog");
            UntypedAgent app = AgenticServices.parallelBuilder()
                    .subAgents(weather, dog)
                    // The decision is plain Java over what the two agents wrote. Nothing about
                    // "did both checks pass" needs a model, and putting it in one would be a
                    // demo lying about where the judgement actually lives.
                    .output(s -> {
                        String w = str(s, "weather");
                        String d = str(s, "dog");
                        boolean veto = (w + " " + d).toUpperCase(Locale.ROOT).contains("FAIL");
                        return (veto ? "Not now" : "Fine — get the lead")
                                + "\n\n- Weather and ground: " + w + "\n- Zao himself: " + d;
                    })
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("walk", input));
            return String.valueOf(r.result());
        };
        return new PatternDef("parallel", "Parallel", "workflow",
                "Fan out independent work concurrently, then join. The weather does not depend on "
                        + "the dog and the dog does not depend on the weather, but you cannot put "
                        + "the lead on until both have answered — which is exactly when "
                        + "fan-out-and-join is the right shape.",
                "Only for truly independent sub-tasks; joining is on you — and the join is where "
                        + "the real rule lives, so keep it in Java where you can test it.",
                topo,
                // Everyone in the room already knows the answer: not at two in the afternoon in
                // July. So they can grade the run instead of taking it on trust.
                "two o'clock on a July afternoon, 31 degrees, the pavement has been in the sun "
                        + "all day. Zao is four, he ate an hour ago, nothing else wrong with him",
                runner);
    }
}
