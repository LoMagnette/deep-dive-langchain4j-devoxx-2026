package dev.devoxx.dashboard.demos._19_modelrouting;

import dev.devoxx.dashboard.demos._06_conditional.Keys.Call;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface DocOnNights {
    @Agent(description = "Doc the St. Bernard, on the night phone: answers every call, whatever it is")
    @UserMessage("""
            You are Doc, on the Pup HQ night phone, and you answer every call yourself tonight.
            Answer this one directly and practically: what to do right now, and whether Officer
            Jo needs waking. Keep it short.

            The call: {{Call}}""")
    String answer(@K(Call.class) String call);
}
