package net.gjerull.etherpad.client;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.graphwalker.core.machine.ExecutionContext;
import org.graphwalker.java.annotation.AfterExecution;
import org.graphwalker.java.annotation.BeforeExecution;
import org.graphwalker.java.annotation.GraphWalker;

import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.StringBody;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;

@GraphWalker(value = "random(edge_coverage(100))", start = "e_connection")
public class EPLiteGroupPadsTest extends ExecutionContext implements EPLiteGroupPads {
    private EPLiteClient client;
    private ClientAndServer mockServer;

    private String groupId;
    private String padId;
    private boolean publicStatus = false;
    private boolean isPasswordProtected = false;

    private final String APIKEY = "a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58";
    private final int PORT = 9001;
    private final List<String> POST_ACTIONS = Arrays.asList("createGroup", "createGroupPad", "deleteGroup", "deletePad", "setPassword", "setPublicStatus");

    private void mockRequest(String action, StringBody requestBody, String responseBody) {
        this.mockServer
                .when(HttpRequest.request()
                        .withMethod(POST_ACTIONS.contains(action) ? "POST" : "GET")
                        .withPath("/api/1.2.13/" + action)
                        .withBody(requestBody),
                        Times.exactly(1))
                .respond(HttpResponse.response()
                        .withStatusCode(200)
                        .withBody(responseBody));
    }

    @BeforeExecution
    public void setUp() {
        ((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.graphwalker.core.machine"))
                .setLevel(ch.qos.logback.classic.Level.OFF);
    }

    @AfterExecution
    public void cleanUp() {
        if (this.mockServer != null) {
            this.mockServer.stop();
        }
    }

    public void e_connection() {
        System.out.println("Connecting...");

        this.client = new EPLiteClient("http://localhost:" + PORT, APIKEY);

        ((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.mockserver.mock"))
                .setLevel(ch.qos.logback.classic.Level.OFF);

        this.mockServer = startClientAndServer(PORT);
    }

    public void v_NoGroups() {
        mockRequest("listAllGroups",
                new StringBody("apikey=" + APIKEY),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"groupIDs\":[]}}");

        Map response = this.client.listAllGroups();
        assertTrue(response.containsKey("groupIDs"));
        List groupIDs = (List) response.get("groupIDs");
        assertEquals(0, groupIDs.size());

        System.out.println("There are no groups.");
    }

    public void v_GroupWithoutPads() {
        mockRequest("listPads",
                new StringBody("apikey=" + APIKEY + "&groupID=" + this.groupId),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"padIDs\":[]}}");

        Map response = this.client.listPads(this.groupId);
        assertTrue(response.containsKey("padIDs"));
        List padIds = (List) response.get("padIDs");
        assertEquals(0, padIds.size());

        System.out.println("There is a group without pads.");
    }

    public void v_PadIntoGroup() {
        mockRequest("listPads",
                new StringBody("apikey=" + APIKEY + "&groupID=" + this.groupId),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"padIDs\":[\"" + this.groupId + "$pad-name\"]}}");

        Map response = this.client.listPads(this.groupId);
        assertTrue(response.containsKey("padIDs"));
        List padIds = (List) response.get("padIDs");
        assertEquals(1, padIds.size());

        System.out.println("There is a group pad!");
    }

    public void e_createGroup() {
        System.out.println("Creating a group...");

        mockRequest("createGroup",
                new StringBody("apikey=" + APIKEY),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"groupID\":\"g.rjMvLsIZmbAUowBD\"}}");

        Map response = this.client.createGroup();
        assertTrue(response.containsKey("groupID"));
        this.groupId = (String) response.get("groupID");
    }

    public void e_deleteGroup() {
        System.out.println("Deleting the group...");

        mockRequest("deleteGroup",
                new StringBody("apikey=" + APIKEY + "&groupID=" + this.groupId),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        this.client.deleteGroup(this.groupId);
        this.groupId = null;
    }

    public void e_createGroupPad() {
        System.out.println("Creating a group pad...");

        mockRequest("createGroupPad",
                new StringBody("apikey=" + APIKEY + "&groupID=" + this.groupId + "&padName=pad-name"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"padID\":\"" + this.groupId + "$pad-name\"}}");

        Map padResponse = this.client.createGroupPad(this.groupId, "pad-name");
        assertTrue(padResponse.containsKey("padID"));
        this.padId = (String) padResponse.get("padID");
    }

    public void e_deletePad() {
        System.out.println("Deleting the group pad...");

        mockRequest("deletePad",
                new StringBody("apikey=" + APIKEY + "&padID=" + this.groupId + "%24pad-name"),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        this.client.deletePad(this.padId);
        this.padId = null;
    }

    public void e_setPublicStatus() {
        System.out.println("Set public status.");

        mockRequest("setPublicStatus",
                new StringBody("apikey=" + APIKEY + "&padID=" + this.groupId + "%24pad-name&publicStatus=true"),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        this.client.setPublicStatus(this.padId, true);
        this.publicStatus = true;
    }

    public void e_getPublicStatus() {
        System.out.println("Get public status.");

        mockRequest("getPublicStatus",
                new StringBody("apikey=" + APIKEY + "&padID=" + this.groupId + "%24pad-name"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"publicStatus\":" + (this.publicStatus ? "true" : "false") + "}}");

        boolean publicStatus = (boolean) this.client.getPublicStatus(this.padId).get("publicStatus");
        assertEquals(this.publicStatus, publicStatus);
    }

    public void e_setPassword() {
        System.out.println("Set password.");

        mockRequest("setPassword",
                new StringBody("password=password&apikey=" + APIKEY + "&padID=" + this.groupId + "%24pad-name"),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        this.client.setPassword(this.padId, "password");
        this.isPasswordProtected = true;
    }

    public void e_isPasswordProtected() {
        System.out.println("Check if it is protected.");

        mockRequest("isPasswordProtected",
                new StringBody("apikey=" + APIKEY + "&padID=" + this.groupId + "%24pad-name"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"isPasswordProtected\":" + (this.isPasswordProtected ? "true" : "false") + "}}");

        boolean passwordProtected = (boolean) this.client.isPasswordProtected(this.padId).get("isPasswordProtected");
        assertEquals(this.isPasswordProtected, isPasswordProtected);
    }
}