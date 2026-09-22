package dev.devoxx.dashboard.demos._02_sequential;

import dev.devoxx.dashboard.demos._01_single.Keys.Message;
import dev.devoxx.dashboard.demos._01_single.Keys.Notes;
import dev.devoxx.dashboard.demos._01_single.NoteRetriever;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.langchain4j.agentic.declarative.AgentListenerSupplier;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.observability.AgentListener;

/**
 * Two agents in order, declared. The hand-off is the shared {@code Notes} key and nothing else —
 * demo 1's agent writes it, demo 2's reads it, and neither knows the other exists.
 */
public interface SequentialSystem {

    @SequenceAgent(name = "Sequential",
                   subAgents = {NoteRetriever.class, FridgeMagnet.class},
                   typedOutputKey = Notes.class)
    String write(@K(Message.class) String message);

    @AgentListenerSupplier
    static AgentListener listener() {
        return CurrentRun.listener();
    }
}
