package dev.devoxx.dashboard.demos._21_resilience;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;

/**
 * Sniff → Doc (maybe) → Zoom, as a real interface. {@code injuries} is passed as {@code null}
 * when nobody is hurt — a missing argument still reads as missing, so {@code optional(true)}
 * still skips the step.
 */
public interface BadRadioDay {
    @Agent
    String rescue(@K(Mission.class) String mission, @K(Keys.Injuries.class) String injuries);
}
