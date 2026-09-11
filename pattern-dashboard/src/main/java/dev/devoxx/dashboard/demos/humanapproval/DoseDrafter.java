package dev.devoxx.dashboard.demos.humanapproval;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface DoseDrafter {
    @Agent(description = "Drafts what to give the dog tonight, for a person to approve")
    @UserMessage("""
            The vet is closed. From what the owner describes and what is in the cupboard, draft
            what you would give the dog tonight: the medicine, the amount, and when. If the right
            answer is to give nothing and wait, draft that instead — it often is.

            Write it as the instruction the owner would follow, in three or four lines. Do not
            say it is approved; that is not yours to decide.

            Tonight: {{situation}}""")
    String draft(@V("situation") String situation);
}
