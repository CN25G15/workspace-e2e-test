package org.tripmonkey;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.tripmonkey.notification.service.Notification;
import org.tripmonkey.patch.data.JsonPatch;
import org.tripmonkey.patch.data.Operation;
import org.tripmonkey.proto.contents.data.UserDTO;
import org.tripmonkey.test.client.NotificationsClient;
import org.tripmonkey.test.client.WorkspaceClient;
import org.tripmonkey.workspace.service.User;
import org.tripmonkey.workspace.service.Workspace;

import java.time.Duration;
import java.util.UUID;

@QuarkusTest
class WorkspaceIntegrationTest {

    static UUID user;
    static UUID workspace;

    @Inject
    @RestClient
    NotificationsClient nc;

    @Inject
    @RestClient
    WorkspaceClient wc;

    @BeforeAll
    public static void initialize(){
        user = UUID.randomUUID();
    }


    @Test
    @Order(1)
    void createWorkspace() {

        UniAssertSubscriber<String> subber = wc.createWorkspaceFor(user.toString())
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        String item = subber.awaitItem(Duration.ofSeconds(5)).assertCompleted().getItem();
        Assertions.assertNotNull(item);

        Workspace.Builder b = Workspace.newBuilder();
        Assertions.assertDoesNotThrow(() -> JsonFormat.parser().merge(item, b));
        Workspace w = b.build();

        Assertions.assertDoesNotThrow(() -> workspace = UUID.fromString(w.getWid()));

        Assertions.assertTrue(w.getCollaboratorsList()
                .stream().map(User::getUserId)
                .toList().contains(user.toString()));
    }

    @Test
    @Order(2)
    void testPatchWithNotification() throws InvalidProtocolBufferException {

        Assumptions.assumeTrue(workspace != null);

        AssertSubscriber<String> notifications = nc.getNotificationsFor(user.toString())
                .subscribe().withSubscriber(AssertSubscriber.create());

        String patch = JsonFormat.printer().print(JsonPatch.newBuilder()
                .setOp(Operation.add)
                .setPath("collaborators")
                .setUserId(UserDTO.newBuilder()
                        .setUserId("b441a660-1020-4b50-869d-b8db5eabee50").build()).build());

        UniAssertSubscriber<Response> subber = wc.commitPatch(workspace.toString(), user.toString(), patch)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        Response r = subber.awaitItem(Duration.ofSeconds(5)).assertCompleted().getItem();

        Assertions.assertNotNull(r);
        Assertions.assertEquals(200, r.getStatus());

        Notification.Builder nb = Notification.newBuilder();

        String n = notifications.getItems().stream()
                .filter(s -> !s.equals("keep_alive"))
                .findFirst().orElse(null);

        Assertions.assertNotNull(n);

    }

}