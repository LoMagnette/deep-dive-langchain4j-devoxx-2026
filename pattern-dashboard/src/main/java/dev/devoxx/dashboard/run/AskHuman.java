package dev.devoxx.dashboard.run;

/**
 * How a running pattern puts a question to a person and waits for the answer.
 */
@FunctionalInterface
public interface AskHuman {

    /**
     * Puts {@code question} to a person and blocks until they answer.
     *
     * @return what they said, or a short marker when nobody answered in time — never null, and
     *         never an exception, because a pattern that asks a person must still be able to end
     *         when nobody is listening.
     */
    String ask(String question);

    /** The stand-in for a run with nobody attached: nothing is approved by default. */
    AskHuman NOBODY = question -> "No answer — nobody was watching this run.";
}
