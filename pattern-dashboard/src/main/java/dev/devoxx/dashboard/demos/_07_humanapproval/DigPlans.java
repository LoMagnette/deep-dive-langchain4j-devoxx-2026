package dev.devoxx.dashboard.demos._07_humanapproval;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

/** Dig the Dachshund, green Ranger: tunnels and tight spots. Wants to dig everywhere. */
public interface DigPlans {
    @Agent(name = "Dig",
           typedOutputKey = Keys.DigPlan.class,
           description = "Dig the Dachshund: plans the tunnel, and does not start digging")
    @UserMessage("""
            Plan the tunnel for this rescue, but do NOT dig yet — Officer Jo has to say yes
            first. Three plain lines: where you start, which way you go, and what it will do to
            whatever is above the tunnel.

            The mission: {{Mission}}""")
    String plan(@K(Mission.class) String mission);
}
