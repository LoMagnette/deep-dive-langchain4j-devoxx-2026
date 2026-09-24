package dev.devoxx.dashboard.demos._05_parallelmapper;

import java.util.List;

import dev.devoxx.dashboard.demos._05_parallelmapper.Keys.Beard;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;

/**
 * The mapper as a real interface, not {@code UntypedAgent}. Its return type has to be the
 * collection itself — {@link dev.langchain4j.agentic.scope.ResultWithAgenticScope} isn't a
 * collection, and {@code ParallelMapperServiceImpl} checks the raw return type at build time.
 */
public interface BeardCheck {
    @Agent
    List<String> check(@K(Beard.class) List<String> found);
}
