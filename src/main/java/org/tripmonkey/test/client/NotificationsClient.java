package org.tripmonkey.test.client;

import io.smallrye.mutiny.Multi;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/notifications/{user_uuid}")
@RegisterRestClient(configKey = "notif-client")
public interface NotificationsClient {

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    Multi<String> getNotificationsFor(@PathParam("user_uuid") String userId);

}
