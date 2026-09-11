package dev.devoxx.dashboard.web;

import dev.devoxx.dashboard.web.LogStream.LogLine;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestStreamElementType;

/** Streams the server log to the dashboard's log pane. */
@Path("/api/logs")
public class LogResource {

    @Inject
    LogStream logs;

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<LogLine> stream() {
        return logs.stream();
    }
}
