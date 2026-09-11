package dev.devoxx.dashboard.support;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * LangChain4j reports every agent failure as "Failed to invoke agent method", so surfacing only
 * getMessage() makes a dead Ollama and a parse failure look identical on stage.
 */
class ErrorsTest {

    @Test
    void errorsExposeTheRootCauseNotJustTheWrapper() {
        var boom = new RuntimeException("Failed to invoke agent method: write(String)",
                new IllegalStateException("outer", new java.net.ConnectException()));
        String explained = Errors.explain(boom);
        assertTrue(explained.contains("ConnectException"), explained);
        assertTrue(explained.contains("base-url"), "should hint at the fix: " + explained);
    }
}
