package dev.devoxx.dashboard.demos._03_loop;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface HowlWrites {
    @Agent(name = "Howl",
           typedOutputKey = Keys.Draft.class,
           description = "Howl the Husky: writes the piece the brief asks for, and rewrites it from Fifi's feedback")
    @UserMessage("""
            Write what the brief below asks for. You are loud and dramatic, and that is fine —
            but if Fifi has given feedback, fix every point she raised. Reply with the text only.

            The brief: {{Brief}}
            Fifi's feedback on your last version: {{Feedback}}""")
    String write(@K(Keys.Brief.class) String brief, @K(Keys.Feedback.class) String feedback);
}
