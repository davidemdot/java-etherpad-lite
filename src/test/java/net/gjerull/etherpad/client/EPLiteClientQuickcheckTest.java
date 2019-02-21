package net.gjerull.etherpad.client;

import java.nio.charset.Charset;

import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

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
    private final String API_VERSION = "1.2.13";
    private final String ENCODING = "UTF-8";

    @Before
    public void setUp() throws Exception {
        this.client = new EPLiteClient("http://localhost:" + PORT, APIKEY, API_VERSION, ENCODING);

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
        this.mockServer
                .when(HttpRequest.request()
                        .withMethod(action == "getText" ? "GET" : "POST")
                        .withPath("/api/" + API_VERSION + "/" + action)
                        .withBody(requestBody),
                        Times.exactly(1))
                .respond(HttpResponse.response()
                        .withStatusCode(200)
                        .withBody(responseBody,
                        Charset.forName(ENCODING)));
    }

    @Property
    public void create_pad_move_and_copy(@From(StringGenerator.class) String keep,
            @From(StringGenerator.class) String change) throws Exception {

        String padId = "test-pad";
        String copyPadId = "test-pad-copy";
        String movePadId = "move-pad-move";

        mockRequest("createPad",
                new StringBody(""),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        mockRequest("copyPad",
                new StringBody("sourceID=" + padId + "&apikey=" + APIKEY + "&force=false&destinationID=" + copyPadId),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"padID\":\"" + copyPadId + "\"}}");

        mockRequest("getText",
                new StringBody("apikey=" + APIKEY + "&padID=" + copyPadId),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"text\":\"" + keep + "\\n\"}}");

        mockRequest("copyPad",
                new StringBody("sourceID=" + padId + "&apikey=" + APIKEY + "&force=false&destinationID=" + movePadId),
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
                new StringBody("apikey=" + APIKEY + "&padID=" + padId),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        this.client.createPad(padId, keep);

        this.client.copyPad(padId, copyPadId);
        String copyPadText = (String) this.client.getText(copyPadId).get("text");
        this.client.movePad(padId, movePadId);
        String movePadText = (String) this.client.getText(movePadId).get("text");

        this.client.setText(movePadId, change);
        this.client.copyPad(movePadId, copyPadId, true);
        String copyPadTextForce = (String) this.client.getText(copyPadId).get("text");
        this.client.movePad(movePadId, copyPadId, true);
        String movePadTextForce = (String) this.client.getText(copyPadId).get("text");

        this.client.deletePad(copyPadId);
        this.client.deletePad(padId);

        assertEquals(keep + "\n", copyPadText);
        assertEquals(keep + "\n", movePadText);

        assertEquals(change + "\n", copyPadTextForce);
        assertEquals(change + "\n", movePadTextForce);
    }
}
