package dev.devoxx.dashboard.demos._15_bdi;

import static dev.devoxx.dashboard.demos._15_bdi.BdiPattern.sees;
import static dev.devoxx.dashboard.demos._15_bdi.BdiPattern.told;

import java.util.List;

import dev.devoxx.dashboard.run.CurrentRun;
import dev.langchain4j.agentic.declarative.AgentListenerSupplier;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.PlannerAgent;
import dev.langchain4j.agentic.declarative.PlannerSupplier;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.patterns.bdi.BDIPlanner;
import dev.langchain4j.agentic.patterns.bdi.Desire;
import dev.langchain4j.agentic.planner.Planner;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;

/** Beliefs in, intentions out, declared. The desires — and their priorities — are the planner. */
public interface ZoomsHead {

    @PlannerAgent(subAgents = {ZoomUpTheBank.class, ZoomTreesTheSquirrel.class, ZoomToTheFord.class,
                               ZoomBringsTheKidBack.class, ZoomNaps.class},
                  typedOutputKey = Keys.Napped.class)
    ResultWithAgenticScope<String> invoke(@K(Keys.Beliefs.class) String beliefs);

    /** Priority, not declaration order, picks the intention: the kid beats the squirrel. */
    @PlannerSupplier
    static Planner planner() {
        return new BDIPlanner(List.of(
                Desire.of("rescue the kid", 100,
                        s -> sees(s, "stranded"),          // only once he has SEEN the kid
                        s -> s.hasState(Keys.Rescued.class),
                        ZoomToTheFord.class, ZoomBringsTheKidBack.class),
                Desire.of("chase that squirrel", 50,
                        s -> told(s, "squirrel"),
                        s -> s.hasState(Keys.Treed.class),
                        ZoomUpTheBank.class, ZoomTreesTheSquirrel.class),
                Desire.of("nap", 10,
                        s -> true,
                        s -> s.hasState(Keys.Napped.class),
                        ZoomNaps.class)));
    }

    /** The run's listener. A static no-arg method is the only hook, hence {@link CurrentRun}. */
    @AgentListenerSupplier
    static AgentListener listener() {
        return CurrentRun.observers();
    }
}
