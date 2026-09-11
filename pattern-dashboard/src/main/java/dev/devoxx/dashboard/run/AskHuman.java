package dev.devoxx.dashboard.run;

/**
 * How a running pattern puts a question to a person and waits for the answer.
 *
 * <p>One method, and deliberately blocking: a human-in-the-loop agent is an agent whose
 * implementation happens to be a person, and from the wiring's point of view it should look
 * exactly like any other slow call. The caller does not know or care whether the answer came from
 * a browser, a Slack message or a canned string in a test.
 *
 * <p>That last one is the reason this is an interface rather than a method on the web layer: the
 * approval demo has to run headlessly under {@code mvn test}, where the "human" is a lambda that
 * answers immediately. Swapping the person for a stub is the only way to test a pattern that
 * waits for one.
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
