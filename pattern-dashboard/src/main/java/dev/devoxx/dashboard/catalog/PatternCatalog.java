package dev.devoxx.dashboard.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.devoxx.dashboard.catalog.PatternDef.PatternInfo;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * The registry. Everything interesting lives in the group classes — this only decides what is
 * in the catalogue and in which order, which is also the order the rail and the gallery show.
 *
 * <p>To add a pattern, put it in the group matching its category and list it in that group's
 * {@code all()}. Nothing here needs to change.
 */
@ApplicationScoped
public class PatternCatalog {

    private final List<PatternDef> patterns = build();

    private static List<PatternDef> build() {
        List<PatternDef> all = new ArrayList<>();
        all.addAll(WorkflowPatterns.all());
        all.addAll(PureAgentPatterns.all());
        all.addAll(ZooPatterns.all());
        all.addAll(CompositePatterns.all());
        return List.copyOf(all);
    }

    public List<PatternInfo> infos() {
        return patterns.stream().map(PatternDef::toInfo).toList();
    }

    public Optional<PatternDef> byId(String id) {
        return patterns.stream().filter(p -> p.id().equals(id)).findFirst();
    }
}
