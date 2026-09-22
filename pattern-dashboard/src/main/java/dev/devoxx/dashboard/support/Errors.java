package dev.devoxx.dashboard.support;

import java.nio.channels.UnresolvedAddressException;

/**
 * Error formatting for the dashboard.
 */
public final class Errors {

    private static final int MAX = 500;

    private Errors() {
    }

    /** Flattens a throwable's cause chain into one readable line: {@code "A: msg ← B: msg"}. */
    public static String explain(Throwable t) {
        StringBuilder sb = new StringBuilder();
        Throwable c = t;
        for (int depth = 0; c != null && depth < 10 && sb.length() < MAX; depth++) {
            String part = describe(c);
            // Wrappers often re-throw with the cause's message; don't say the same thing twice.
            if (sb.indexOf(part) < 0) {
                sb.append(sb.isEmpty() ? "" : " ← ").append(part);
            }
            Throwable next = c.getCause();
            if (next == c) {
                break;
            }
            c = next;
        }
        return sb.length() > MAX ? sb.substring(0, MAX) + "…" : sb.toString();
    }

    private static String describe(Throwable t) {
        String m = t.getMessage();
        String name = t.getClass().getSimpleName();
        // Bare ConnectException/SocketTimeoutException carry no message at all — name the fix.
        if (m == null || m.isBlank()) {
            // A hostname that doesn't resolve surfaces as an empty ConnectException wrapping this,
            // which reads exactly like a stopped server unless we spell the difference out.
            if (t instanceof UnresolvedAddressException) {
                return name + " (hostname does not resolve from here)";
            }
            if (name.contains("Connect")) {
                return t.getCause() == null ? name + " (nothing listening on the model's base-url)"
                        : name;
            }
            return name;
        }
        return name + ": " + m;
    }
}
