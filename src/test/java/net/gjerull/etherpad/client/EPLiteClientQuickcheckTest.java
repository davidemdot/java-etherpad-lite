package net.gjerull.etherpad.client;

import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;

import java.util.*;
import java.nio.charset.Charset;

import net.gjerull.etherpad.client.StringGenerator;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.StringBody;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;

@RunWith(JUnitQuickcheck.class)
public class EPLiteClientQuickcheckTest {
    private EPLiteClient client;
    private ClientAndServer mockServer;

    private final String APIKEY = "a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58";
    private final int PORT = 9001;
    private final List<String> POST_ACTIONS = Arrays.asList("appendChatMessage", "appendText", "copyPad",
            "createAuthorIfNotExistsFor", "createGroup", "createGroupIfNotExistsFor", "createGroupPad", "createPad",
            "createSession", "deleteGroup", "deletePad", "deleteSession", "movePad", "saveRevision",
            "sendClientsMessage", "setHTML", "setPassword", "setPublicStatus", "setText");

    @Before
    public void setUp() throws Exception {
        this.client = new EPLiteClient("http://localhost:" + PORT, APIKEY);

        ((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.mockserver.mock"))
                .setLevel(ch.qos.logback.classic.Level.OFF);

        ((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("junit-quickcheck.value-reporting"))
                .setLevel(ch.qos.logback.classic.Level.OFF);

        this.mockServer = startClientAndServer(PORT);
    }

    @After
    public void tearDown() {
        this.mockServer.stop();
    }

    private void mockRequest(String action, StringBody requestBody, String responseBody) {
        String method = POST_ACTIONS.contains(action) || (action == "createAuthor" && requestBody.toString() != "apikey=" + APIKEY)
                        ? "POST"
                        : "GET";

        this.mockServer
                .when(HttpRequest.request()
                        .withMethod(method)
                        .withPath("/api/1.2.13/" + action)
                        .withBody(requestBody),
                        Times.exactly(1))
                .respond(HttpResponse.response()
                        .withStatusCode(200)
                        .withBody(responseBody,
                        Charset.forName("UTF-8")));
    }

    @Property
    public void create_pad_move_and_copy(@From(StringGenerator.class) String keep,
            @From(StringGenerator.class) String change) throws Exception {

        String padID = "integration-test-pad";
        String copyPadId = "integration-test-pad-copy";
        String movePadId = "integration-move-pad-move";

        mockRequest("createPad",
                new StringBody(""),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        mockRequest("copyPad",
                new StringBody("sourceID=" + padID + "&apikey=" + APIKEY + "&force=false&destinationID=" + copyPadId),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"padID\":\"" + copyPadId + "\"}}");

        mockRequest("getText",
                new StringBody("apikey=" + APIKEY + "&padID=" + copyPadId),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"text\":\"" + keep + "\\n\"}}");

        mockRequest("copyPad",
                new StringBody("sourceID=" + padID + "&apikey=" + APIKEY + "&force=false&destinationID=" + movePadId),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"padID\":\"" + movePadId + "\"}}");

        mockRequest("getText",
                new StringBody("apikey=" + APIKEY + "&padID=" + movePadId),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"text\":\"" + keep + "\\n\"}}");

        mockRequest("setText",
                new StringBody(""),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        mockRequest("copyPad",
                new StringBody("sourceID=" + movePadId + "&apikey=" + APIKEY + "&force=true&destinationID=" + copyPadId),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"padID\":\"" + copyPadId + "\"}}");

        mockRequest("getText",
                new StringBody("apikey=" + APIKEY + "&padID=" + copyPadId),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"text\":\"" + change + "\\n\"}}");

        mockRequest("movePad",
                new StringBody("sourceID=" + movePadId + "&apikey=" + APIKEY + "&force=true&destinationID=" + copyPadId),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        mockRequest("getText",
                new StringBody("apikey=" + APIKEY + "&padID=" + copyPadId),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"text\":\"" + change + "\\n\"}}");

        mockRequest("deletePad",
                new StringBody("apikey=" + APIKEY + "&padID=" + copyPadId),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        mockRequest("deletePad",
                new StringBody("apikey=" + APIKEY + "&padID=" + padID),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        client.createPad(padID, keep);

        client.copyPad(padID, copyPadId);
        String copyPadText = (String) client.getText(copyPadId).get("text");
        client.movePad(padID, movePadId);
        String movePadText = (String) client.getText(movePadId).get("text");

        client.setText(movePadId, change);
        client.copyPad(movePadId, copyPadId, true);
        String copyPadTextForce = (String) client.getText(copyPadId).get("text");
        client.movePad(movePadId, copyPadId, true);
        String movePadTextForce = (String) client.getText(copyPadId).get("text");

        client.deletePad(copyPadId);
        client.deletePad(padID);

        assertEquals(keep + "\n", copyPadText);
        assertEquals(keep + "\n", movePadText);

        assertEquals(change + "\n", copyPadTextForce);
        assertEquals(change + "\n", movePadTextForce);
    }
}
