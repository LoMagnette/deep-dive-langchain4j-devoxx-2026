package dev.devoxx.dashboard.run;

/**
 * The per-run context, made reachable from a {@code static} method.
 *
 * <p><b>This class exists because of one gap in the declarative API, and it is the whole price of
 * using it here.</b> A builder takes the listener at the call site — {@code
 * .listener(listener).build()} — so a per-run listener is just an argument. The declarative
 * equivalent is a {@code @AgentListenerSupplier} method, which
 * {@code DeclarativeUtil.buildListener} requires to be {@code static} and invokes with
 * {@code invokeStatic} — no arguments, and none of the parameter resolution that
 * {@code @ChatModelSupplier} gets. A static no-arg method cannot be handed anything, so the only
 * way to reach this run's listener from inside one is an ambient variable.
 *
 * <p>Three things make that safe rather than merely expedient:
 * <ul>
 *   <li><b>It is read at BUILD time, not at invocation time.</b>
 *       {@code AgenticServices.createAgenticSystem(...)} runs synchronously on the thread that
 *       called it, so the supplier fires on the run thread and the listener reference is captured
 *       into the built agent. Parallel sub-agents later run on other threads and never touch this
 *       — which they could not, since a {@code ThreadLocal} does not cross a fan-out.</li>
 *   <li><b>Runs execute on a pool.</b> {@link #set} / {@link #clear} therefore bracket the build
 *       in a try/finally, or a pooled thread would carry a dead run's listener into the next one.</li>
 *   <li><b>Nothing reads it outside that window.</b> If a read ever returns null it means the
 *       build moved off the run thread, which is worth failing loudly for rather than quietly
 *       attaching no listener and producing a run the page never sees.</li>
 * </ul>
 *
 * <p>The builder form needed none of this. That is the honest trade the declarative style asks
 * for here, and it is worth saying out loud on stage rather than hiding: annotations are
 * resolved by the framework, so anything that varies per invocation has to reach them by some
 * route other than an argument.
 */
public final class CurrentRun {

    private CurrentRun() {
    }

    private static final ThreadLocal<StreamingListener> LISTENER = new ThreadLocal<>();

    /** Brackets a build. Always paired with {@link #clear()} in a finally. */
    public static void set(StreamingListener listener) {
        LISTENER.set(listener);
    }

    public static void clear() {
        LISTENER.remove();
    }

    /**
     * The listener for the run currently being built. Throws rather than returning null: a null
     * here means an agent system was built off the run thread, and the symptom would otherwise
     * be a run that works perfectly and shows nothing on the page.
     */
    public static StreamingListener listener() {
        StreamingListener l = LISTENER.get();
        if (l == null) {
            throw new IllegalStateException(
                    "no run in progress on this thread — an agent system was built outside "
                            + "CurrentRun.set/clear, so its listener would be silently missing");
        }
        return l;
    }
}
