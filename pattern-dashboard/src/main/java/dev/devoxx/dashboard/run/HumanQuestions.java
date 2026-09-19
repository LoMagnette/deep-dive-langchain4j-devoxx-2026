package dev.devoxx.dashboard.run;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * The meeting point between a run that is waiting for a person and the HTTP request that carries
 * their answer.
 *
 * <p>A run streams over Server-Sent Events, which are one-way: the browser can be told a question
 * but has no way to reply down the same pipe. So the answer arrives as a separate POST, and this
 * bean is what lets that POST find the thread that is blocked waiting for it — keyed by the run
 * id that every run announces in its {@code run-start} event.
 *
 * <p>Two things here are load-bearing rather than defensive:
 * <ul>
 *   <li><b>The wait has a timeout.</b> Runs execute on a small fixed pool, so a question nobody
 *       answers does not just strand one demo — it holds a pool thread for ever and the next few
 *       runs silently never start. On a conference stage that looks like the app hanging.</li>
 *   <li><b>Ending a run cancels its question.</b> Close the browser tab mid-question and the
 *       waiting thread is released rather than sitting out the full timeout. That needs a
 *       callback at the other end and does not come for free: cancelling an SSE subscription
 *       tells the thread blocked in {@link #await} precisely nothing, so
 *       {@code PatternResource} registers {@code em.onTermination(() -> humans.cancel(runId))}.
 *       Without it this paragraph is a description of what the code does not do.</li>
 * </ul>
 */
@ApplicationScoped
public class HumanQuestions {

    /** Long enough to read a draft and think; short enough that a forgotten tab frees the pool. */
    static final Duration WAIT = Duration.ofMinutes(3);

    private final Map<String, CompletableFuture<String>> pending = new ConcurrentHashMap<>();

    /**
     * Blocks the calling run until someone answers, the timeout expires, or the run is cancelled.
     * Never throws: a pattern that asks a person must still be able to finish when nobody replies.
     */
    public String await(String runId) {
        CompletableFuture<String> answer = new CompletableFuture<>();
        // A run can only have one question outstanding at a time; a second replaces the first,
        // which would otherwise leak a future nobody will ever complete.
        CompletableFuture<String> previous = pending.put(runId, answer);
        if (previous != null) {
            previous.complete(AskHuman.NOBODY.ask(""));
        }
        try {
            return answer.get(WAIT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            return "No answer in " + WAIT.toMinutes() + " minutes — treat this as not approved.";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Interrupted before anyone answered.";
        } catch (Exception e) {
            return AskHuman.NOBODY.ask("");
        } finally {
            pending.remove(runId, answer);
        }
    }

    /** Hands an answer to the waiting run. False when that run is not asking anything. */
    public boolean answer(String runId, String text) {
        CompletableFuture<String> waiting = pending.remove(runId);
        if (waiting == null) {
            return false;
        }
        return waiting.complete(text == null ? "" : text);
    }

    /** Releases a run's waiting thread when the run ends or the viewer disappears. */
    public void cancel(String runId) {
        CompletableFuture<String> waiting = pending.remove(runId);
        if (waiting != null) {
            waiting.complete("The run was stopped before anyone answered.");
        }
    }
}
