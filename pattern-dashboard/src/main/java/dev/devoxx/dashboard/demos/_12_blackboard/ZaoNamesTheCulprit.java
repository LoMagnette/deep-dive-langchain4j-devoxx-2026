package dev.devoxx.dashboard.demos._12_blackboard;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface ZaoNamesTheCulprit {
    @Agent(description = "Zao the Bouvier: reads the board and names the culprit")
    @UserMessage("""
            You are Zao, leader of the Pawer Rangers. Read the clues on the board and name the
            culprit — the suspects are the Rangers and Mittens, the cat who lives at number 9 —
            with the clue that convicts them. A sausage crumb was found in your own beard: say
            whether the board clears you, and how. "Not proven" is not a verdict the Rangers
            accept. Three short plain sentences, no headings.

            The drain: {{TunnelClue}}
            The crumb: {{CrumbClue}}""")
    String conclude(@K(Keys.TunnelClue.class) String tunnel, @K(Keys.CrumbClue.class) String crumb);
}
