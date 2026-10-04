package dev.devoxx.dashboard.demos._09_supervisor;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.devoxx.dashboard.demos._06_conditional.DigOnCall;
import dev.devoxx.dashboard.demos._06_conditional.DocOnCall;
import dev.devoxx.dashboard.demos._06_conditional.SniffOnCall;
import dev.devoxx.dashboard.demos._06_conditional.ZoomOnCall;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.langchain4j.agentic.declarative.AgentListenerSupplier;
import dev.langchain4j.agentic.declarative.BeforeCall;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.SupervisorAgent;
import dev.langchain4j.agentic.declarative.SupervisorRequest;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.supervisor.SupervisorContextStrategy;
import dev.langchain4j.agentic.supervisor.SupervisorPlanner;

/**
 * Zao supervising the fair, declared: Mission 6's four Rangers on call, named by class. No
 * {@code name} on the annotation — like the builder before it, the supervisor reports itself
 * under this method's name.
 */
public interface FairSupervisor {

    @SupervisorAgent(subAgents = {SniffOnCall.class, ZoomOnCall.class, DigOnCall.class, DocOnCall.class},
                     maxAgentsInvocations = 6,
                     contextStrategy = SupervisorContextStrategy.CHAT_MEMORY)
    String invoke(@K(Mission.class) String mission);

    /**
     * What Zao is asked to solve. Without this method the supervisor reads a scope key spelled
     * {@code "request"}; with it, the request comes from a typed key like every other input.
     */
    @SupervisorRequest
    static String request(@K(Mission.class) String mission) {
        return mission;
    }

    /**
     * Zao's standing orders. {@code @SupervisorAgent} has no attribute for the builder's
     * {@code supervisorContext(...)} — but that call is only a {@code beforeCall} writing one scope
     * key, which is exactly what this is.
     */
    @BeforeCall
    static void standingOrders(AgenticScope scope) {
        scope.writeStateIfAbsent(SupervisorPlanner.SUPERVISOR_CONTEXT_KEY, ORDERS);
    }

    /** The builder's supervisorContext text, word for word. */
    String ORDERS = """
            You are Zao, leader of the Pawer Rangers. The fair has several separate \
            problems. Send ONE Ranger at a time, to ONE problem, with that problem \
            as the call: Sniff finds the lost, Zoom catches anything running away, \
            Dig deals with holes and tight spots, Doc deals with anyone hurt. Read \
            each report before deciding who goes next. When every problem has had \
            a Ranger, finish, and say in one sentence whether the fair is under \
            control.""";

    /** The route Zao chose, then each Ranger's report — the declarative {@code .output(...)}. */
    @Output
    static String fairStatus(AgenticScope scope) {
        return SupervisorPattern.fairStatus(scope);
    }

    /** The run's listener. A static no-arg method is the only hook, hence {@link CurrentRun}. */
    @AgentListenerSupplier
    static AgentListener listener() {
        return CurrentRun.observers();
    }
}
