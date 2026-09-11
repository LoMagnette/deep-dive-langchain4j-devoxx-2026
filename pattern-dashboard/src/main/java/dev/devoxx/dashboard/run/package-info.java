/**
 * Observing one run: the events the page animates, and the listener that produces them.
 *
 * <p>Separate from {@code web} on purpose — a run is observable whether or not anything is
 * watching it over HTTP, which is what lets the tests assert on the very same events the browser
 * sees.
 */
package dev.devoxx.dashboard.run;
