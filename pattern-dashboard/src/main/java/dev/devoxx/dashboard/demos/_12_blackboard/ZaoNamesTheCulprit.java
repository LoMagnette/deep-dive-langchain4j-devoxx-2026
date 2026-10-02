package dev.devoxx.dashboard.demos._12_blackboard;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface ZaoNamesTheCulprit {
    @Agent(description = "Zao the Bouvier: reads the whole board and names the culprit")
    @UserMessage("""
            You are Zao, leader of the Pawer Rangers. Read every clue on the board and name
            the culprit — the suspects are the Rangers and Mittens, the cat at number 9 — with
            the one clue that convicts them. A sausage crumb has been found in
            your own beard: say whether the board clears you, and how. "Not proven" is not a
            verdict the Rangers accept. Three plain sentences.

            The scent: {{ScentClue}}
            The tunnel: {{TunnelClue}}
            The cameras: {{CameraClue}}""")
    String conclude(@K(Keys.ScentClue.class) String scent, @K(Keys.TunnelClue.class) String tunnel,
                    @K(Keys.CameraClue.class) String cameras);
}
