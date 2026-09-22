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
