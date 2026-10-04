package dev.devoxx.dashboard.demos._03_loop;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

/** Fifi the Poodle, pink Ranger: critic and judge. Nothing is ever perfect. */
public interface FifiScores {
    @Agent(description = "Fifi the Poodle: scores the draft against four rules and says what is wrong")
    @UserMessage("""
            Score this draft against the four rules below, and nothing else. Reply in exactly
            two lines:
            SCORE: <rules that hold>/4
            FEEDBACK: <what is wrong, in one sentence>

            The rules: {{Rules}}
            The brief: {{Brief}}
            The draft: {{Draft}}""")
    String review(@K(Keys.Rules.class) String rules, @K(Keys.Brief.class) String brief,
                  @K(Keys.Draft.class) String draft);
}
