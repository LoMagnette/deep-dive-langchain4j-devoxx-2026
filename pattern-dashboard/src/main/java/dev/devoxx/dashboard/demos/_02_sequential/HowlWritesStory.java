package dev.devoxx.dashboard.demos._02_sequential;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

/** Howl the Husky, purple Ranger: writes and argues. Loud and dramatic. */
public interface HowlWritesStory {
    @Agent(name = "Howl",
           typedOutputKey = Keys.Article.class,
           description = "Howl the Husky: writes the Barkville Gazette story of the rescue")
    @UserMessage("""
            Write the Barkville Gazette story of this rescue: a headline, then three sentences.
            Be as dramatic as you like, but every fact must come from the report below — you
            were not there.

            Doc's report: {{HealthReport}}""")
    String write(@K(Keys.HealthReport.class) String healthReport);
}
