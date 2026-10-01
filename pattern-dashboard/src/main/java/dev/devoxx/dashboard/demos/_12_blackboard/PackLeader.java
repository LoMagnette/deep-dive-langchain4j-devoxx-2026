package dev.devoxx.dashboard.demos._12_blackboard;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface PackLeader {
    @Agent(description = "Zao: reads the whole board and names who did it")
    @UserMessage("""
            You are Zao, the pack leader. From everything on the board, name who did it, most
            guilty first, including any accomplice, with the one piece of evidence that
            convicts each. It was somebody in this pack — nobody broke in — and "not proven" is
            not a verdict the pack accepts. Then pass sentence in one line.

            The trail: {{Trail}}
            The alibis: {{Alibis}}
            The scene: {{Scene}}""")
    String conclude(@K(Keys.Trail.class) String trail, @K(Keys.Alibis.class) String alibis,
                    @K(Keys.Scene.class) String scene);
}
