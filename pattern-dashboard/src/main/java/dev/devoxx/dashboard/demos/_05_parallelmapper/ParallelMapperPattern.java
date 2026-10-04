package dev.devoxx.dashboard.demos._05_parallelmapper;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static dev.devoxx.dashboard.support.Parsing.items;
import static java.util.Objects.requireNonNullElse;
import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.stream.IntStream;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._05_parallelmapper.Keys.Ducklings;
import dev.devoxx.dashboard.demos._05_parallelmapper.Keys.FoundDucklings;
import dev.devoxx.dashboard.demos._05_parallelmapper.Keys.Sighting;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for <b>Mission 5</b> — one Sniff, eight ducklings, all at once.
 */
public final class ParallelMapperPattern {

    private ParallelMapperPattern() {
    }

    static String run(ChatModel model, String input, StreamingListener listener) {
        var sniff = AgenticServices.agentBuilder(SniffSearches.class)
                .chatModel(model)
                .name("Sniff")
                .outputKey("Sighting")
                .build();
        DucklingSearch app = AgenticServices.parallelMapperBuilder(DucklingSearch.class)
                .name("ParallelMapper")
                .subAgents(sniff)
                .itemsProvider("Ducklings")
                .outputKey("FoundDucklings")
                .listener(listener)
                .build();
        // The ducklings come from what was typed (one per line or semicolon), so the input box
        // decides how wide the fan-out is — eight is only the default.
        List<String> ducklings = items(input);
        List<String> found = requireNonNullElse(app.search(ducklings), List.of());
        // Paired back with the duckling each result is about: the mapper preserves order, and
        // eight unlabelled lines would leave the room counting.
        return IntStream.range(0, found.size())
                .mapToObj(i -> "- **" + (i < ducklings.size() ? ducklings.get(i) : "duckling " + i)
                        + "** — " + found.get(i))
                .collect(joining("\n"));
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // One agent drawn as a stack: one Sniff, invoked once per item, all at once. A single box
        // would say "one call", which is the opposite of what a mapper does.
        Topology.Graph topo = graph("fanout",
                List.of(node("in", "ducklings", "input").withSub("8 · last seen where"),
                        node("sniff", "Sniff", "agent").withSub("once per duckling").asStack().as("sniff"),
                        node("gather", "gather", "join").withSub("foundDucklings")),
                List.of(edge("in", "sniff", "scatter"),
                        edge("sniff", "gather", "one result each")));
        return new PatternDef("parallelMapper", "Parallel Mapper", "workflow",
                "Mrs Mallard has lost all eight ducklings at once. Each was last seen somewhere "
                        + "different. One of them is following Marmalade.",
                "Mission 4 sent three different Rangers. This sends one Ranger eight times.",
                "Map one agent over a collection in parallel (scatter / gather): the same Sniff "
                        + "search, once per duckling, all at once. The contrast with Mission 4 is "
                        + "the point — there, different agents on one input; here, **one agent "
                        + "on many inputs**, and the width of the fan-out is data. Add a ninth "
                        + "duckling to the input and there are nine searches.",
                "Beware fan-out cost and rate limits when the list is long: a list of ducklings "
                        + "is a list of concurrent model calls, and nothing in the builder asks "
                        + "how long the list is.",
                topo,
                "Puddle — last seen at the duck pond; Pickle — last seen by the bakery bins; "
                        + "Waddles — last seen on the town hall steps; Biscuit — last seen in the "
                        + "fountain; Noodle — last seen under the bandstand; Pip — last seen at the "
                        + "bus stop; Socks — last seen in the Mayor's roses; Bean — last seen "
                        + "following Marmalade",
                ParallelMapperPattern::run)
                .gist("One agent, run once per item, all at once — then gathered.");
    }
}
