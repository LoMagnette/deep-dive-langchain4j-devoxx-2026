package dev.devoxx.dashboard.demos._21_resilience;

import dev.devoxx.dashboard.demos._01_single.Keys.Message;
import dev.devoxx.dashboard.demos._21_resilience.Keys.Injuries;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;

/**
 * The clerk → first aid → battle plan sequence, as a real interface, not {@code UntypedAgent}.
 * {@code injuries} is passed as {@code null} when the report mentions nobody hurt — reading it
 * back is null either way, so a missing argument still reads as missing and
 * {@code optional(true)} still skips the step.
 */
public interface ReportPipeline {
    @Agent
    String write(@K(Message.class) String message, @K(Injuries.class) String injuries);
}
