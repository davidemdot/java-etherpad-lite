package net.gjerull.etherpad.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.graphwalker.core.machine.ExecutionContext;
import org.graphwalker.java.annotation.AfterExecution;
import org.graphwalker.java.annotation.BeforeElement;
import org.graphwalker.java.annotation.BeforeExecution;
import org.graphwalker.java.annotation.GraphWalker;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.StringBody;

@GraphWalker(
        //value = "random(edge_coverage(100) && time_duration(20))",
        value = "random(edge_coverage(100))",
        start = "e_connection")
public class EPLiteGroupPadsTest extends ExecutionContext implements EPLiteGroupPads {

    private final boolean DEBUG = false;

    private final Map<String, List<Pad>> GROUPS = new HashMap<>();

    public class Pad {
        public final String pId;
        public boolean pub;
        public boolean pwd;

        public Pad(String pId) {
            this.pId = pId;
            this.pub = false;
            this.pwd = false;
        }
    }

    public String randomGroup() {
        Random random = new Random();
        List<String> groups = new ArrayList<String>(GROUPS.keySet());
        if (groups.size() == 0) {
            return null;
        }
        return groups.get(random.nextInt(groups.size()));
    }

    public String randomPad() {
        List<String> groups = new ArrayList<String>(GROUPS.keySet());
        List<String> groupsWithPads = new ArrayList<String>();
        for (String group : groups) {
            if (GROUPS.get(group).size() != 0) {
                groupsWithPads.add(group);
            }
        }
        if (groupsWithPads.size() == 0) {
            return null;
        }
        Random random = new Random();
        String randomGroup = groupsWithPads.get(random.nextInt(groupsWithPads.size()));
        List<Pad> pads = GROUPS.get(randomGroup);
        return pads.get(random.nextInt(pads.size())).pId;
    }

    public int numPads() {
        int num = 0;
        List<String> groups = new ArrayList<String>(GROUPS.keySet());
        for (String group : groups) {
            num += GROUPS.get(group).size();
        }
        return num;
    }

    public void addGroup(String gId) {
        GROUPS.put(gId, new LinkedList<Pad>());
    }

    public int delGroup(String gId) {
        int size = GROUPS.get(gId).size();
        GROUPS.remove(gId);
        return size;
    }

    public Pad getGroupPad(String pId) {
        String gId = pId.substring(0, 18);
        List<Pad> pads = GROUPS.get(gId);
        for (Pad pad : pads) {
            if (pad.pId.equals(pId)) {
                return pad;
            }
        }
        return null;
    }

    public void addGroupPad(String gId, String pId) {
        GROUPS.get(gId).add(new Pad(pId));
    }

    public void delGroupPad(String gId, String pId) {
        Pad pad = getGroupPad(pId);
        if (pad != null) {
            GROUPS.get(gId).remove(pad);
        }
    }

    public static class IdGenerator {
        private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghiklmnnopqrstuvwxyz0123456789";
        private static final Random RANDOM = new Random();

        public static String get(int size) {
            StringBuilder string = new StringBuilder(size);
            for (int i = 0; i < size; i++) {
                string.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
            }
            return string.toString();
        }
    }

    private EPLiteClient client;
    private ClientAndServer mockServer;

    private final String APIKEY = "a04f17343b51afaa036a7428171dd873469cd85911ab43be0503d29d2acbbd58";
    private final int PORT = 9001;
    private final List<String> POST_ACTIONS = Arrays.asList("createGroup", "createGroupPad", "deleteGroup", "deletePad",
            "setPassword", "setPublicStatus");

    private void mockRequest(String action, StringBody requestBody, String responseBody) {
        this.mockServer
                .when(HttpRequest.request().withMethod(POST_ACTIONS.contains(action) ? "POST" : "GET")
                        .withPath("/api/1.2.13/" + action).withBody(requestBody), Times.exactly(1))
                .respond(HttpResponse.response().withStatusCode(200).withBody(responseBody));
    }

    private int getIntAttribute(String name) {
        Object value = getAttribute(name);
        if (value instanceof Double) {
            return (int) Math.round((Double) value);
        } else {
            return (Integer) value;
        }
    }

    private void printDebug() {
        System.out.printf("%n*** %s // G: %s // NG_GRAPH: %d // NG_TEST: %d // NP_GRAPH: %d // NP_TEST: %d ***%n",
                getCurrentElement() != null ? getCurrentElement().getName() : "NULL",
                GROUPS.keySet().toString(),
                getIntAttribute("num_groups"),
                GROUPS.size(),
                getIntAttribute("num_pads"),
                numPads());
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

    @BeforeElement
    public void afterElem() {
        assertEquals(GROUPS.size(), getIntAttribute("num_groups"));
        assertEquals(numPads(), getIntAttribute("num_pads"));

        if (DEBUG) {
            printDebug();
        }
    }

    @Override
    public void e_connection() {
        ((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.mockserver.mock"))
                .setLevel(ch.qos.logback.classic.Level.OFF);

        this.client = new EPLiteClient("http://localhost:" + PORT, APIKEY);
        this.mockServer = startClientAndServer(PORT);

        System.out.println("Connecting...");
    }

    @Override
    public void v_NoGroups() {
        assertEquals(0, GROUPS.size());
        assertEquals(0, getIntAttribute("num_groups"));
        assertEquals(GROUPS.size(), getIntAttribute("num_groups"));
    }

    @Override
    public void v_GroupsWithoutPads() {
        assertNotEquals(0, GROUPS.size());
        assertNotEquals(0, getIntAttribute("num_groups"));
        assertEquals(GROUPS.size(), getIntAttribute("num_groups"));

        assertEquals(0, numPads());
        assertEquals(0, getIntAttribute("num_pads"));
        assertEquals(numPads(), getIntAttribute("num_pads"));
    }

    @Override
    public void v_GroupsAndPads() {
        assertNotEquals(0, GROUPS.size());
        assertNotEquals(0, getIntAttribute("num_groups"));
        assertEquals(GROUPS.size(), getIntAttribute("num_groups"));

        assertEquals(numPads(), getIntAttribute("num_pads"));
        // Send to the correct vertex after deleting the group that contains the last pads.
        if (numPads() == 0) {
            setNextElement(getModel().findElements("v_GroupsWithoutPads").get(0));
        }
        else {
            assertNotEquals(0, numPads());
            assertNotEquals(0, getIntAttribute("num_pads"));
        }
    }

    @Override
    public void e_createGroup() {
        String groupId = "g." + IdGenerator.get(16);
        mockRequest("createGroup", new StringBody("apikey=" + APIKEY),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"groupID\":\"" + groupId + "\"}}");

        this.client.createGroup();
        addGroup(groupId);

        System.out.printf("Creating the group %s...%n", groupId);
    }

    @Override
    public void e_deleteGroup() {
        String groupId = randomGroup();
        mockRequest("deleteGroup", new StringBody("apikey=" + APIKEY + "&groupID=" + groupId),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        this.client.deleteGroup(groupId);
        delGroup(groupId);
        setAttribute("num_pads", new Double(numPads()));

        System.out.printf("Deleting the group %s (and its pads)...%n", groupId);
    }

    @Override
    public void e_createGroupPad() {
        String groupId = randomGroup();
        String padName = IdGenerator.get(8);
        String padId = groupId + "$" + padName;
        mockRequest("createGroupPad",
                new StringBody("apikey=" + APIKEY + "&groupID=" + groupId + "&padName=" + padName),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"padID\":\"" + padId + "\"}}");

        this.client.createGroupPad(groupId, padName);
        addGroupPad(groupId, padId);

        System.out.printf("Creating the pad %s...%n", padId);
    }

    @Override
    public void e_deletePad() {
        String padId = randomPad();
        String groupId = padId.substring(0, 18);
        mockRequest("deletePad", new StringBody("apikey=" + APIKEY + "&padID=" + padId.replace("$", "%24")),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        this.client.deletePad(padId);
        delGroupPad(groupId, padId);

        System.out.printf("Deleting the pad %s...%n", padId);
    }

    @Override
    public void e_setPassword() {
        String padId = randomPad();
        mockRequest("setPassword",
                new StringBody("password=password&apikey=" + APIKEY + "&padID=" + padId.replace("$", "%24")),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        this.client.setPassword(padId, "password");
        getGroupPad(padId).pwd = true;

        System.out.printf("Setting a password for the pad %s...%n", padId);
    }

    @Override
    public void e_isPasswordProtected() {
        String padId = randomPad();
        mockRequest("isPasswordProtected", new StringBody("apikey=" + APIKEY + "&padID=" + padId.replace("$", "%24")),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"isPasswordProtected\":"
                        + (getGroupPad(padId).pwd ? "true" : "false") + "}}");

        boolean pwd = (boolean) this.client.isPasswordProtected(padId).get("isPasswordProtected");
        assertEquals(getGroupPad(padId).pwd, pwd);

        System.out.printf("Is the pad %s protected by password? %s.%n", padId, getGroupPad(padId).pwd ? "Yes" : "No");
    }
}
