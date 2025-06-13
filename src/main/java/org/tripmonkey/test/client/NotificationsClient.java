package org.tripmonkey.test.client;

import io.smallrye.mutiny.Multi;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.client.SseEvent;
import org.jboss.resteasy.reactive.client.SseEventFilter;

import java.util.function.Predicate;

@Path("/notifications/{user_uuid}")
@RegisterRestClient(configKey = "notif-client")
public interface NotificationsClient {

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @SseEventFilter(NotificationsClient.HeartbeatFilter.class)
    Multi<SseEvent<String>> getNotificationsFor(@PathParam("user_uuid") String userId);

    class HeartbeatFilter implements Predicate<SseEvent<String>> {

        @Override
        public boolean test(SseEvent<String> event) {
            return !"keep_alive".equals(event.id());
        }
    }

}
