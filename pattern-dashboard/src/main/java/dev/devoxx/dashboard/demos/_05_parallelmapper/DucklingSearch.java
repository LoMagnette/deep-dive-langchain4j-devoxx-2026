package dev.devoxx.dashboard.demos._05_parallelmapper;

import java.util.List;

import dev.devoxx.dashboard.run.CurrentRun;
import dev.langchain4j.agentic.declarative.AgentListenerSupplier;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.ParallelMapperAgent;
import dev.langchain4j.agentic.observability.AgentListener;

/**
 * The mapper, declared: a list of ducklings in, a list of results out.
 *
 * <p><b>{@code itemsProvider} is a string, and has to be.</b> The annotation has no typed form of
 * it, and an annotation value must be a compile-time constant, so {@code new Ducklings().name()}
 * — what the builder form used — is not allowed here. {@code "Ducklings"} is the record's name, so
 * it is the same key; misspell it and nothing fails here, the mapper just finds no items. The
 * typing is only ever as good as the narrowest API you touch.
 */
public interface DucklingSearch {

    @ParallelMapperAgent(name = "ParallelMapper",
                         subAgent = SniffSearches.class,
                         itemsProvider = "Ducklings",
                         typedOutputKey = Keys.FoundDucklings.class)
    List<String> search(@K(Keys.Ducklings.class) List<String> ducklings);

    /** The run's listener. A static no-arg method is the only hook, hence {@link CurrentRun}. */
    @AgentListenerSupplier
    static AgentListener listener() {
        return CurrentRun.observers();
    }
}
