package dev.devoxx.dashboard.demos._03_loop;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface HowlWrites {
    @Agent(description = "Howl the Husky: writes the piece the brief asks for, and rewrites it from Fifi's feedback")
    @UserMessage("""
            Write what the brief below asks for. You are loud and dramatic, and that is fine —
            but if Fifi has given feedback, fix every point she raised. Reply with the text only.

            The brief: {{Brief}}
            Fifi's feedback on your last version: {{Feedback}}""")
    String write(@V("Brief") String brief, @V("Feedback") String feedback);
}
