package org.tripmonkey.test.client;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/workspace")
@RegisterRestClient(configKey = "workspace-client")
public interface WorkspaceClient {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    Uni<String> createWorkspaceFor(@CookieParam("user") String user_uuid);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{workspace_id}")
    Uni<String> fetchWorkspace(@PathParam("workspace_id") String workspace_uuid, @CookieParam("user") String user_uuid);

    @PATCH
    @Path("/{workspace_id")
    Uni<Response> commitPatch(@PathParam("workspace_id") String workspace_uuid, @CookieParam("user") String user_uuid,
                              String patch);

}
