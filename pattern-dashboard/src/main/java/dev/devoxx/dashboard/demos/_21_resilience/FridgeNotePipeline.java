package dev.devoxx.dashboard.demos._21_resilience;

import dev.devoxx.dashboard.demos._01_single.Keys.Message;
import dev.devoxx.dashboard.demos._21_resilience.Keys.Meds;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;

/**
 * The clerk → meds → fridge-magnet sequence, as a real interface, not {@code UntypedAgent}.
 * {@code meds} is passed as {@code null} when the message never mentions medication — reading
 * it back is null either way, so a missing argument still reads as missing and
 * {@code optional(true)} still skips the step.
 */
public interface FridgeNotePipeline {
    @Agent
    String write(@K(Message.class) String message, @K(Meds.class) String meds);
}
