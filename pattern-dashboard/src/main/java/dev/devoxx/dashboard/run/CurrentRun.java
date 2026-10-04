package dev.devoxx.dashboard.run;

import java.util.function.Supplier;

import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.observability.ComposedAgentListener;

/**
 * The per-run context, made reachable from a {@code static} method.
 *
 * <p><b>This class exists because of one gap in the declarative API, and it is the whole price of
 * using it here.</b> A builder takes the listener at the call site — {@code
 * .listener(listener).build()} — so a per-run listener is just an argument. The declarative
 * equivalent is an {@code @AgentListenerSupplier} method, which {@code DeclarativeUtil} requires
 * to be {@code static} and invokes with no arguments. A static no-arg method cannot be handed
 * anything, so the only way to reach this run's listener from inside one is an ambient variable.
 * The same is true of every other per-run thing a declared system reads from a static method:
 * the person Officer Jo's question goes to (Mission 7) and the two model tiers (Mission 19).
 *
 * <p>Three things make that safe rather than merely expedient:
 * <ul>
 *   <li><b>It brackets the whole run, on the run's own thread.</b>
 *       {@code createAgenticSystem(...)} builds synchronously, so the listener supplier fires
 *       here and the reference is captured into the built agent. A sequence then invokes its
 *       steps on the calling thread, which is why the human question and the model choice — read
 *       at INVOCATION time — can find it too. Parallel branches run on other threads and never
 *       read it, which they could not: a {@code ThreadLocal} does not cross a fan-out.</li>
 *   <li><b>Runs execute on a pool.</b> {@link #with} clears in a {@code finally}, or a pooled
 *       thread would carry a dead run's listener into the next one.</li>
 *   <li><b>A missing run fails loudly.</b> A null here would mean a system was built or a
 *       static method was called off the run thread, and the symptom would otherwise be a run
 *       that works perfectly and shows nothing on the page.</li>
 * </ul>
 *
 * <p>The builder form needed none of this. That is the honest trade the declarative style asks
 * for, and it is worth saying out loud on stage rather than hiding: annotations are resolved by
 * the framework, so anything that varies per invocation has to reach them by some route other
 * than an argument.
 */
public final class CurrentRun {

    private CurrentRun() {
    }

    private record Run(StreamingListener listener, AgentListener observers, ModelTiers tiers) {
    }

    private static final ThreadLocal<Run> RUN = new ThreadLocal<>();

    /** Runs {@code body} — build AND invocation — with this run reachable from static methods. */
    public static <T> T with(StreamingListener listener, Supplier<T> body) {
        return with(new Run(listener, listener, null), body);
    }

    /** As above, with a second listener watching the same run — Mission 7's {@code AgentMonitor}. */
    public static <T> T with(StreamingListener listener, AgentListener alsoWatching, Supplier<T> body) {
        return with(new Run(listener, new ComposedAgentListener(listener, alsoWatching), null), body);
    }

    /** As above, with the two models this run may choose between — Mission 19. */
    public static <T> T with(StreamingListener listener, ModelTiers tiers, Supplier<T> body) {
        return with(new Run(listener, listener, tiers), body);
    }

    private static <T> T with(Run run, Supplier<T> body) {
        RUN.set(run);
        try {
            return body.get();
        } finally {
            RUN.remove();
        }
    }

    /** What an {@code @AgentListenerSupplier} returns: everything watching this run. */
    public static AgentListener observers() {
        return run().observers();
    }

    /** The run's own listener — for {@code askHuman} and {@code streamingModel}. */
    public static StreamingListener listener() {
        return run().listener();
    }

    /** The model tiers this run was started with. Only Mission 19 starts one with any. */
    public static ModelTiers tiers() {
        ModelTiers tiers = run().tiers();
        if (tiers == null) {
            throw new IllegalStateException("this run was started without model tiers — "
                    + "use CurrentRun.with(listener, tiers, ...)");
        }
        return tiers;
    }

    private static Run run() {
        Run run = RUN.get();
        if (run == null) {
            throw new IllegalStateException(
                    "no run in progress on this thread — a declared system was built or called "
                            + "outside CurrentRun.with(...), so its listener would be silently missing");
        }
        return run;
    }
}
