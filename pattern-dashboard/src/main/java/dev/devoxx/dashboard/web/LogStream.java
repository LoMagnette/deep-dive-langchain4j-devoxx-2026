package dev.devoxx.dashboard.web;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import dev.devoxx.dashboard.support.Errors;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Mirrors the server log into the browser, so the interesting half of the demo — the supervisor
 * choosing its next agent, the blackboard activating experts, a model falling back — is visible
 * on the projector instead of in a terminal nobody is showing.
 */
@ApplicationScoped
public class LogStream {

    /** One rendered log record, as the browser consumes it. */
    public record LogLine(long seq, String time, String level, String logger, String message) {
    }

    /** Enough to cover a run or two; the browser replays this on connect. */
    private static final int HISTORY = 400;

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    /**
     * Emitting a record can itself log (the SSE layer, Vert.x, Jackson). Without this guard that
     * is unbounded recursion on the logging thread.
     */
    private static final ThreadLocal<Boolean> EMITTING = ThreadLocal.withInitial(() -> false);

    /**
     * Dev mode restarts the app inside a JVM whose root logger survives, so a plain
     * {@code addHandler} would stack a new handler on every live reload and duplicate every line.
     */
    private static volatile Handler installed;

    private final Deque<LogLine> history = new ArrayDeque<>();
    private final BroadcastProcessor<LogLine> live = BroadcastProcessor.create();
    private final AtomicLong seq = new AtomicLong();

    /**
     * Priority 1 so this attaches before any other startup observer runs — otherwise the single
     * most interesting boot line ("dashboard model: …", or the warning that it fell back to the
     * mock) is logged before anyone is listening and never reaches the browser.
     */
    void onStartup(@Observes @Priority(1) StartupEvent event) {
        Logger root = Logger.getLogger("");
        if (installed != null) {
            root.removeHandler(installed);
        }
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                capture(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        root.addHandler(handler);
        installed = handler;
    }

    /** History first (so a viewer who connects mid-run sees context), then live records. */
    public Multi<LogLine> stream() {
        return Multi.createBy().concatenating().streams(
                Multi.createFrom().iterable(snapshot()),
                // A viewer that can't keep up loses lines rather than stalling the logging thread.
                live.onOverflow().drop());
    }

    private synchronized List<LogLine> snapshot() {
        return new ArrayList<>(history);
    }

    private void capture(LogRecord record) {
        if (EMITTING.get()) {
            return;
        }
        LogLine line = new LogLine(seq.incrementAndGet(),
                LocalTime.now().format(TIME),
                level(record),
                shortLogger(record.getLoggerName()),
                message(record));
        synchronized (this) {
            history.addLast(line);
            while (history.size() > HISTORY) {
                history.removeFirst();
            }
        }
        EMITTING.set(true);
        try {
            live.onNext(line);
        } catch (RuntimeException ignored) {
            // A broken subscriber must never break the thing that was logging.
        } finally {
            EMITTING.set(false);
        }
    }

    /** JUL spells these WARNING/SEVERE/FINE; the UI (and everyone reading) expects log4j-ish names. */
    private static String level(LogRecord record) {
        String name = record.getLevel().getName();
        return switch (name) {
            case "WARNING" -> "WARN";
            case "SEVERE" -> "ERROR";
            case "FINE", "FINER", "FINEST" -> "DEBUG";
            case "CONFIG" -> "INFO";
            default -> name;
        };
    }

    private static String shortLogger(String name) {
        if (name == null || name.isEmpty()) {
            return "root";
        }
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(dot + 1);
    }

    private static String message(LogRecord record) {
        String text = format(record);
        if (record.getThrown() != null) {
            text = text + "  ↳ " + Errors.explain(record.getThrown());
        }
        return text;
    }

    /**
     * JBoss's own records already know how to render {@code LOG.warnf("%s", …)} placeholders;
     * plain JUL records only understand {@code {0}} style, which is what SimpleFormatter does.
     */
    private static String format(LogRecord record) {
        if (record instanceof org.jboss.logmanager.ExtLogRecord ext) {
            return String.valueOf(ext.getFormattedMessage());
        }
        return new SimpleFormatter().formatMessage(record);
    }
}
