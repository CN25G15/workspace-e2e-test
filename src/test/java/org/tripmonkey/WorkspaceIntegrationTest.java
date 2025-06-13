package org.tripmonkey;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.client.SseEvent;
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
import org.tripmonkey.workspace.service.WorkspaceResponse;

import java.time.Duration;
import java.util.List;
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

    @Inject
    Logger log;

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

        log.infof("Received \n%s",item);

        WorkspaceResponse.Builder b = WorkspaceResponse.newBuilder();
        Assertions.assertDoesNotThrow(() -> JsonFormat.parser().merge(item, b));
        WorkspaceResponse wr = b.build();
        Assertions.assertTrue(wr.hasWorkspace());
        Workspace w = wr.getWorkspace();

        Assertions.assertDoesNotThrow(() -> workspace = UUID.fromString(w.getWid()));

        Assertions.assertTrue(w.getCollaboratorsList()
                .stream().map(User::getUserId)
                .toList().contains(user.toString()));
    }

    @Test
    @Order(2)
    void testPatchWithNotification() throws InvalidProtocolBufferException {

        UUID collaborator = UUID.randomUUID();

        Assumptions.assumeTrue(workspace != null);

        AssertSubscriber<SseEvent<String>> notifications = nc.getNotificationsFor(user.toString())
                .subscribe().withSubscriber(AssertSubscriber.create());

        String patch = String.format(
                "{\"op\":\"add\",\"path\":\"collaborators\",\"value\":{\"user_id\": \"%s\"}}", collaborator.toString());


        log.infof("Built request\n%s",patch);

        UniAssertSubscriber<Response> subber = wc.commitPatch(workspace.toString(), user.toString(), patch)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        Response r = subber.awaitItem(Duration.ofSeconds(5)).assertCompleted().getItem();

        Assertions.assertNotNull(r);
        Assertions.assertEquals(200, r.getStatus());

        Notification.Builder nb = Notification.newBuilder();

        SseEvent<String> recv = notifications.awaitNextItem().getLastItem();

        log.infof("Received %s", recv.data());

        Assertions.assertNotNull(recv.data());
        Assertions.assertTrue(recv.data().contains(collaborator.toString()));

    }

}