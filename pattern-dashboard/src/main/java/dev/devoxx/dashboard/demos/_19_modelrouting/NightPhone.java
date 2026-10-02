package dev.devoxx.dashboard.demos._19_modelrouting;

import dev.devoxx.dashboard.demos._06_conditional.Keys.Call;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;

/** Classify, then answer on the right model — as a real interface. */
public interface NightPhone {
    @Agent
    String answer(@K(Call.class) String call);
}
