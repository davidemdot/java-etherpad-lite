package net.gjerull.etherpad.client;

import java.util.*;
import java.nio.charset.Charset;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.StringBody;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;

/**
 * Integration test for simple App.
 */
public class EPLiteClientIntegrationTest {
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

    @Test
    public void validate_token() throws Exception {
        mockRequest("checkToken",
                new StringBody("apikey=" + APIKEY),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        client.checkToken();
    }

    @Test
    public void create_and_delete_group() throws Exception {
        mockRequest("createGroup",
                new StringBody("apikey=" + APIKEY),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"groupID\":\"g.i2fG1FoN1FYFG7yO\"}}");

        mockRequest("deleteGroup",
                new StringBody("apikey=" + APIKEY + "&groupID=g.i2fG1FoN1FYFG7yO"),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        Map response = client.createGroup();

        assertTrue(response.containsKey("groupID"));
        String groupId = (String) response.get("groupID");
        assertTrue("Unexpected groupID " + groupId, groupId != null && groupId.startsWith("g."));

        client.deleteGroup(groupId);
    }

    @Test
    public void create_group_if_not_exists_for_and_list_all_groups() throws Exception {
        mockRequest("createGroupIfNotExistsFor",
                new StringBody("apikey=" + APIKEY + "&groupMapper=groupname"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"groupID\":\"g.8VYHxVThUmwIoLGL\"}}");

        mockRequest("listAllGroups",
                new StringBody("apikey=" + APIKEY),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"groupIDs\":[\"g.8VYHxVThUmwIoLGL\"]}}");

        mockRequest("createGroupIfNotExistsFor",
                new StringBody("apikey=" + APIKEY + "&groupMapper=groupname"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"groupID\":\"g.8VYHxVThUmwIoLGL\"}}");

        mockRequest("listAllGroups",
                new StringBody("apikey=" + APIKEY),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"groupIDs\":[\"g.8VYHxVThUmwIoLGL\"]}}");

        mockRequest("deleteGroup",
                new StringBody("apikey=" + APIKEY + "&groupID=g.8VYHxVThUmwIoLGL"),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        String groupMapper = "groupname";

        Map response = client.createGroupIfNotExistsFor(groupMapper);

        assertTrue(response.containsKey("groupID"));
        String groupId = (String) response.get("groupID");
        try {
            Map listResponse = client.listAllGroups();
            assertTrue(listResponse.containsKey("groupIDs"));
            int firstNumGroups = ((List) listResponse.get("groupIDs")).size();

            client.createGroupIfNotExistsFor(groupMapper);

            listResponse = client.listAllGroups();
            int secondNumGroups = ((List) listResponse.get("groupIDs")).size();

            assertEquals(firstNumGroups, secondNumGroups);
        } finally {
            client.deleteGroup(groupId);
        }
    }

    @Test
    public void create_group_pads_and_list_them() throws Exception {
        mockRequest("createGroup",
                new StringBody("apikey=" + APIKEY),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"groupID\":\"g.9v9F9kaoGb6tNzTs\"}}");

        mockRequest("createGroupPad",
                new StringBody("apikey=" + APIKEY + "&groupID=g.9v9F9kaoGb6tNzTs&padName=integration-test-1"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"padID\":\"g.9v9F9kaoGb6tNzTs$integration-test-1\"}}");

        mockRequest("setPublicStatus",
                new StringBody("apikey=" + APIKEY + "&padID=g.9v9F9kaoGb6tNzTs%24integration-test-1&publicStatus=true"),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        mockRequest("getPublicStatus",
                new StringBody("apikey=" + APIKEY + "&padID=g.9v9F9kaoGb6tNzTs%24integration-test-1"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"publicStatus\":true}}");

        mockRequest("setPassword",
                new StringBody("password=integration&apikey=" + APIKEY + "&padID=g.9v9F9kaoGb6tNzTs%24integration-test-1"),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        mockRequest("isPasswordProtected",
                new StringBody("apikey=" + APIKEY + "&padID=g.9v9F9kaoGb6tNzTs%24integration-test-1"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"isPasswordProtected\":true}}");

        mockRequest("createGroupPad",
                new StringBody("apikey=" + APIKEY + "&groupID=g.9v9F9kaoGb6tNzTs&padName=integration-test-2&text=Initial+text"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"padID\":\"g.9v9F9kaoGb6tNzTs$integration-test-2\"}}");

        mockRequest("getText",
                new StringBody("apikey=" + APIKEY + "&padID=g.9v9F9kaoGb6tNzTs%24integration-test-2"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"text\":\"Initial text\\n\"}}");

        mockRequest("listPads",
                new StringBody("apikey=" + APIKEY + "&groupID=g.9v9F9kaoGb6tNzTs"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"padIDs\":[\"g.9v9F9kaoGb6tNzTs$integration-test-1\",\"g.9v9F9kaoGb6tNzTs$integration-test-2\"]}}");

        mockRequest("deleteGroup",
                new StringBody("apikey=" + APIKEY + "&groupID=g.9v9F9kaoGb6tNzTs"),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        Map response = client.createGroup();
        String groupId = (String) response.get("groupID");
        String padName1 = "integration-test-1";
        String padName2 = "integration-test-2";
        try {
            Map padResponse = client.createGroupPad(groupId, padName1);
            assertTrue(padResponse.containsKey("padID"));
            String padId1 = (String) padResponse.get("padID");

            client.setPublicStatus(padId1, true);
            boolean publicStatus = (boolean) client.getPublicStatus(padId1).get("publicStatus");
            assertTrue(publicStatus);

            client.setPassword(padId1, "integration");
            boolean passwordProtected = (boolean) client.isPasswordProtected(padId1).get("isPasswordProtected");
            assertTrue(passwordProtected);

            padResponse = client.createGroupPad(groupId, padName2, "Initial text");
            assertTrue(padResponse.containsKey("padID"));

            String padId = (String) padResponse.get("padID");
            String initialText = (String) client.getText(padId).get("text");
            assertEquals("Initial text\n", initialText);

            Map padListResponse = client.listPads(groupId);

            assertTrue(padListResponse.containsKey("padIDs"));
            List padIds = (List) padListResponse.get("padIDs");

            assertEquals(2, padIds.size());
        } finally {
            client.deleteGroup(groupId);
        }
    }

    @Test
    public void create_author() throws Exception {
        mockRequest("createAuthor",
                new StringBody("apikey=" + APIKEY),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"authorID\":\"a.RaxswhjtGYn1dm4E\"}}");

        mockRequest("createAuthor",
                new StringBody("apikey=" + APIKEY + "&name=integration-author"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"authorID\":\"a.Z2LNAtUV0qmhsin0\"}}");

        mockRequest("getAuthorName",
                new StringBody("apikey=" + APIKEY + "&authorID=a.Z2LNAtUV0qmhsin0"),
                "{\"code\":0,\"message\":\"ok\",\"data\":\"integration-author\"}");

        Map authorResponse = client.createAuthor();
        String authorId = (String) authorResponse.get("authorID");
        assertTrue(authorId != null && !authorId.isEmpty());

        authorResponse = client.createAuthor("integration-author");
        authorId = (String) authorResponse.get("authorID");

        String authorName = client.getAuthorName(authorId);
        assertEquals("integration-author", authorName);
    }

    @Test
    public void create_author_with_author_mapper() throws Exception {
        mockRequest("createAuthorIfNotExistsFor",
                new StringBody("apikey=" + APIKEY + "&name=integration-author-1&authorMapper=username"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"authorID\":\"a.kPrLhmVVGK3Y5j03\"}}");

        mockRequest("getAuthorName",
                new StringBody("apikey=" + APIKEY + "&authorID=a.kPrLhmVVGK3Y5j03"),
                "{\"code\":0,\"message\":\"ok\",\"data\":\"integration-author-1\"}");

        mockRequest("createAuthorIfNotExistsFor",
                new StringBody("apikey=" + APIKEY + "&name=integration-author-2&authorMapper=username"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"authorID\":\"a.kPrLhmVVGK3Y5j03\"}}");

        mockRequest("getAuthorName",
                new StringBody("apikey=" + APIKEY + "&authorID=a.kPrLhmVVGK3Y5j03"),
                "{\"code\":0,\"message\":\"ok\",\"data\":\"integration-author-2\"}");

        mockRequest("createAuthorIfNotExistsFor",
                new StringBody("apikey=" + APIKEY + "&authorMapper=username"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"authorID\":\"a.kPrLhmVVGK3Y5j03\"}}");

        mockRequest("getAuthorName",
                new StringBody("apikey=" + APIKEY + "&authorID=a.kPrLhmVVGK3Y5j03"),
                "{\"code\":0,\"message\":\"ok\",\"data\":\"integration-author-2\"}");

        String authorMapper = "username";

        Map authorResponse = client.createAuthorIfNotExistsFor(authorMapper, "integration-author-1");
        String firstAuthorId = (String) authorResponse.get("authorID");
        assertTrue(firstAuthorId != null && !firstAuthorId.isEmpty());

        String firstAuthorName = client.getAuthorName(firstAuthorId);

        authorResponse = client.createAuthorIfNotExistsFor(authorMapper, "integration-author-2");
        String secondAuthorId = (String) authorResponse.get("authorID");
        assertEquals(firstAuthorId, secondAuthorId);

        String secondAuthorName = client.getAuthorName(secondAuthorId);

        assertNotEquals(firstAuthorName, secondAuthorName);

        authorResponse = client.createAuthorIfNotExistsFor(authorMapper);
        String thirdAuthorId = (String) authorResponse.get("authorID");
        assertEquals(secondAuthorId, thirdAuthorId);
        String thirdAuthorName = client.getAuthorName(thirdAuthorId);

        assertEquals(secondAuthorName, thirdAuthorName);
    }

    @Test
    public void create_and_delete_session() throws Exception {
        mockRequest("createGroupIfNotExistsFor",
                new StringBody("apikey=" + APIKEY + "&groupMapper=groupname"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"groupID\":\"g.8gG2mcJguOEo4std\"}}");

        mockRequest("createAuthorIfNotExistsFor",
                new StringBody("apikey=" + APIKEY + "&name=integration-author-1&authorMapper=username"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"authorID\":\"a.kPrLhmVVGK3Y5j03\"}}");

        Calendar oneYearFromNow = Calendar.getInstance();
        oneYearFromNow.add(Calendar.YEAR, 1);
        Date sessionValidUntil = oneYearFromNow.getTime();
        long validUntil = sessionValidUntil.getTime() / 1000L;

        mockRequest("createSession",
                new StringBody(""),  // apikey=" + APIKEY + "&groupID=g.8gG2mcJguOEo4std&validUntil=1550319910&authorID=a.kPrLhmVVGK3Y5j03"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"sessionID\":\"s.195787a420c128e5827bafa026a2d95a\"}}");

        mockRequest("createSession",
                new StringBody(""),  // apikey=" + APIKEY + "&groupID=g.8gG2mcJguOEo4std&validUntil=" + validUntil + "&authorID=a.kPrLhmVVGK3Y5j03"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"sessionID\":\"s.9409d25a24542516843dc9361e718852\"}}");

        mockRequest("getSessionInfo",
                new StringBody("apikey=" + APIKEY + "&sessionID=s.9409d25a24542516843dc9361e718852"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"groupID\":\"g.8gG2mcJguOEo4std\",\"authorID\":\"a.kPrLhmVVGK3Y5j03\",\"validUntil\":" + validUntil + "}}");

        mockRequest("listSessionsOfGroup",
                new StringBody("apikey=" + APIKEY + "&groupID=g.8gG2mcJguOEo4std"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"s.195787a420c128e5827bafa026a2d95a\":{\"groupID\":\"g.8gG2mcJguOEo4std\",\"authorID\":\"a.kPrLhmVVGK3Y5j03\",\"validUntil\":1550319910},\"s.9409d25a24542516843dc9361e718852\":{\"groupID\":\"g.8gG2mcJguOEo4std\",\"authorID\":\"a.kPrLhmVVGK3Y5j03\",\"validUntil\":" + validUntil + "}}}");

        mockRequest("listSessionsOfAuthor",
                new StringBody("apikey=" + APIKEY + "&authorID=a.kPrLhmVVGK3Y5j03"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"s.195787a420c128e5827bafa026a2d95a\":{\"groupID\":\"g.8gG2mcJguOEo4std\",\"authorID\":\"a.kPrLhmVVGK3Y5j03\",\"validUntil\":1550319910},\"s.9409d25a24542516843dc9361e718852\":{\"groupID\":\"g.8gG2mcJguOEo4std\",\"authorID\":\"a.kPrLhmVVGK3Y5j03\",\"validUntil\":" + validUntil + "}}}");

        mockRequest("deleteSession",
                new StringBody("apikey=" + APIKEY + "&sessionID=s.195787a420c128e5827bafa026a2d95a"),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        mockRequest("deleteSession",
                new StringBody("apikey=" + APIKEY + "&sessionID=s.9409d25a24542516843dc9361e718852"),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        String authorMapper = "username";
        String groupMapper = "groupname";

        Map groupResponse = client.createGroupIfNotExistsFor(groupMapper);
        String groupId = (String) groupResponse.get("groupID");
        Map authorResponse = client.createAuthorIfNotExistsFor(authorMapper, "integration-author-1");
        String authorId = (String) authorResponse.get("authorID");

        int sessionDuration = 8;
        Map sessionResponse = client.createSession(groupId, authorId, sessionDuration);
        String firstSessionId = (String) sessionResponse.get("sessionID");

        sessionResponse = client.createSession(groupId, authorId, sessionValidUntil);
        String secondSessionId = (String) sessionResponse.get("sessionID");
        try {
            assertNotEquals(firstSessionId, secondSessionId);

            Map sessionInfo = client.getSessionInfo(secondSessionId);
            assertEquals(groupId, sessionInfo.get("groupID"));
            assertEquals(authorId, sessionInfo.get("authorID"));
            assertEquals(sessionValidUntil.getTime() / 1000L, (long) sessionInfo.get("validUntil"));

            Map sessionsOfGroup = client.listSessionsOfGroup(groupId);
            sessionInfo = (Map) sessionsOfGroup.get(firstSessionId);
            assertEquals(groupId, sessionInfo.get("groupID"));
            sessionInfo = (Map) sessionsOfGroup.get(secondSessionId);
            assertEquals(groupId, sessionInfo.get("groupID"));

            Map sessionsOfAuthor = client.listSessionsOfAuthor(authorId);
            sessionInfo = (Map) sessionsOfAuthor.get(firstSessionId);
            assertEquals(authorId, sessionInfo.get("authorID"));
            sessionInfo = (Map) sessionsOfAuthor.get(secondSessionId);
            assertEquals(authorId, sessionInfo.get("authorID"));
        } finally {
            client.deleteSession(firstSessionId);
            client.deleteSession(secondSessionId);
        }
    }

    @Test
    public void create_pad_set_and_get_content() {
        mockRequest("createPad",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad"),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        mockRequest("setText",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad&text=g%C3%A5+%C3%A5+gj%C3%B8r+et+%C3%A6rend"),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        mockRequest("getText",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"text\":\"gå å gjør et ærend\\n\"}}");

        mockRequest("setHTML",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad&html=%3C%21DOCTYPE+HTML%3E%3Chtml%3E%3Cbody%3E%3Cp%3Eg%C3%A5+og+gj%C3%B8re+et+%C3%A6rend+igjen%3C%2Fp%3E%3C%2Fbody%3E%3C%2Fhtml%3E"),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        mockRequest("getHTML",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"html\":\"<!DOCTYPE HTML><html><body>g&#229; og gj&#248;re et &#230;rend igjen<br><br></body></html>\"}}");

        mockRequest("getHTML",
                new StringBody("rev=2&apikey=" + APIKEY + "&padID=integration-test-pad"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"html\":\"<!DOCTYPE HTML><html><body><br></body></html>\"}}");

        mockRequest("getText",
                new StringBody("rev=2&apikey=" + APIKEY + "&padID=integration-test-pad"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"text\":\"\\n\"}}");

        mockRequest("getRevisionsCount",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"revisions\":3}}");

        mockRequest("getRevisionChangeset",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad"),
                "{\"code\":0,\"message\":\"ok\",\"data\":\"Z:1>r|1+r$gå og gjøre et ærend igjen\\n\"}");

        mockRequest("getRevisionChangeset",
                new StringBody("rev=2&apikey=" + APIKEY + "&padID=integration-test-pad"),
                "{\"code\":0,\"message\":\"ok\",\"data\":\"Z:j<i|1-j|1+1$\\n\"}");

        mockRequest("createDiffHTML",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad&startRev=1&endRev=2"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"html\":\"<style>\\n.removed {text-decoration: line-through; -ms-filter:'progid:DXImageTransform.Microsoft.Alpha(Opacity=80)'; filter: alpha(opacity=80); opacity: 0.8; }\\n</style><span class=\\\"removed\\\">g&#229; &#229; gj&#248;r et &#230;rend</span><br><br>\",\"authors\":[\"\"]}}");

        mockRequest("appendText",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad&text=lagt+til+n%C3%A5"),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        mockRequest("getText",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"text\":\"gå og gjøre et ærend igjen\\nlagt til nå\\n\"}}");

        mockRequest("getAttributePool",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"pool\":{\"numToAttrib\":{\"0\":[\"author\",\"\"],\"1\":[\"removed\",\"true\"]},\"attribToNum\":{\"author,\":0,\"removed,true\":1},\"nextNum\":2}}}");

        mockRequest("saveRevision",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad"),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        mockRequest("saveRevision",
                new StringBody("rev=2&apikey=" + APIKEY + "&padID=integration-test-pad"),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        mockRequest("getSavedRevisionsCount",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"savedRevisions\":2}}");

        mockRequest("listSavedRevisions",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"savedRevisions\":[2,4]}}");

        mockRequest("padUsersCount",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"padUsersCount\":0}}");

        mockRequest("padUsers",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"padUsers\":[]}}");

        mockRequest("getReadOnlyID",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"readOnlyID\":\"r.c6f3444a5940eef10c9ac6572a30490f\"}}");

        mockRequest("getPadID",
                new StringBody("apikey=" + APIKEY + "&roID=r.c6f3444a5940eef10c9ac6572a30490f"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"padID\":\"integration-test-pad\"}}");

        mockRequest("listAuthorsOfPad",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"authorIDs\":[]}}");

        mockRequest("getLastEdited",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"lastEdited\":1550291109893}}");

        mockRequest("sendClientsMessage",
                new StringBody("msg=test+message&apikey=" + APIKEY + "&padID=integration-test-pad"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{}}");

        mockRequest("deletePad",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad"),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        String padID = "integration-test-pad";
        client.createPad(padID);
        try {
            client.setText(padID, "gå å gjør et ærend");
            String text = (String) client.getText(padID).get("text");
            assertEquals("gå å gjør et ærend\n", text);

            client.setHTML(
                    padID,
                   "<!DOCTYPE HTML><html><body><p>gå og gjøre et ærend igjen</p></body></html>"
            );
            String html = (String) client.getHTML(padID).get("html");
            assertTrue(html, html.contains("g&#229; og gj&#248;re et &#230;rend igjen<br><br>"));

            html = (String) client.getHTML(padID, 2).get("html");
            assertEquals("<!DOCTYPE HTML><html><body><br></body></html>", html);
            text = (String) client.getText(padID, 2).get("text");
            assertEquals("\n", text);

            long revisionCount = (long) client.getRevisionsCount(padID).get("revisions");
            assertEquals(3L, revisionCount);

            String revisionChangeset = client.getRevisionChangeset(padID);
            assertTrue(revisionChangeset, revisionChangeset.contains("gå og gjøre et ærend igjen"));

            revisionChangeset = client.getRevisionChangeset(padID, 2);
            assertTrue(revisionChangeset, revisionChangeset.contains("|1-j|1+1$\n"));

            String diffHTML = (String) client.createDiffHTML(padID, 1, 2).get("html");
            assertTrue(diffHTML, diffHTML.contains(
                    "<span class=\"removed\">g&#229; &#229; gj&#248;r et &#230;rend</span>"
            ));

            client.appendText(padID, "lagt til nå");
            text = (String) client.getText(padID).get("text");
            assertEquals("gå og gjøre et ærend igjen\nlagt til nå\n", text);

            Map attributePool = (Map) client.getAttributePool(padID).get("pool");
            assertTrue(attributePool.containsKey("attribToNum"));
            assertTrue(attributePool.containsKey("nextNum"));
            assertTrue(attributePool.containsKey("numToAttrib"));

            client.saveRevision(padID);
            client.saveRevision(padID, 2);

            long savedRevisionCount = (long) client.getSavedRevisionsCount(padID).get("savedRevisions");
            assertEquals(2L, savedRevisionCount);

            List savedRevisions = (List) client.listSavedRevisions(padID).get("savedRevisions");
            assertEquals(2, savedRevisions.size());
            assertEquals(2L, savedRevisions.get(0));
            assertEquals(4L, savedRevisions.get(1));

            long padUsersCount = (long) client.padUsersCount(padID).get("padUsersCount");
            assertEquals(0, padUsersCount);

            List padUsers = (List) client.padUsers(padID).get("padUsers");
            assertEquals(0, padUsers.size());

            String readOnlyId = (String) client.getReadOnlyID(padID).get("readOnlyID");
            String padIdFromROId = (String) client.getPadID(readOnlyId).get("padID");
            assertEquals(padID, padIdFromROId);

            List authorsOfPad = (List) client.listAuthorsOfPad(padID).get("authorIDs");
            assertEquals(0, authorsOfPad.size());

            long lastEditedTimeStamp = (long) client.getLastEdited(padID).get("lastEdited");
            Calendar lastEdited = Calendar.getInstance();
            lastEdited.setTimeInMillis(lastEditedTimeStamp);
            Calendar now = Calendar.getInstance();
            assertTrue(lastEdited.before(now));

            client.sendClientsMessage(padID, "test message");
        } finally {
            client.deletePad(padID);
        }
    }

    @Test
    public void create_pad_move_and_copy() throws Exception {
        mockRequest("createPad",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad&text=should+be+kept"),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        mockRequest("copyPad",
                new StringBody("sourceID=integration-test-pad&apikey=" + APIKEY + "&force=false&destinationID=integration-test-pad-copy"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"padID\":\"integration-test-pad-copy\"}}");

        mockRequest("getText",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad-copy"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"text\":\"should be kept\\n\"}}");

        mockRequest("copyPad",
                new StringBody("sourceID=integration-test-pad&apikey=" + APIKEY + "&force=false&destinationID=integration-move-pad-move"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"padID\":\"integration-move-pad-move\"}}");

        mockRequest("getText",
                new StringBody("apikey=" + APIKEY + "&padID=integration-move-pad-move"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"text\":\"should be kept\\n\"}}");

        mockRequest("setText",
                new StringBody("apikey=" + APIKEY + "&padID=integration-move-pad-move&text=should+be+changed"),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        mockRequest("copyPad",
                new StringBody("sourceID=integration-move-pad-move&apikey=" + APIKEY + "&force=true&destinationID=integration-test-pad-copy"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"padID\":\"integration-test-pad-copy\"}}");

        mockRequest("getText",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad-copy"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"text\":\"should be changed\\n\"}}");

        mockRequest("movePad",
                new StringBody("sourceID=integration-move-pad-move&apikey=" + APIKEY + "&force=true&destinationID=integration-test-pad-copy"),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        mockRequest("getText",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad-copy"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"text\":\"should be changed\\n\"}}");

        mockRequest("deletePad",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad-copy"),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        mockRequest("deletePad",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad"),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        String padID = "integration-test-pad";
        String copyPadId = "integration-test-pad-copy";
        String movePadId = "integration-move-pad-move";
        String keep = "should be kept";
        String change = "should be changed";
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

    @Test
    public void create_pads_and_list_them() throws InterruptedException {
        mockRequest("createPad",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad-1"),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        mockRequest("createPad",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad-2"),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        mockRequest("listAllPads",
                new StringBody("apikey=" + APIKEY),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"padIDs\":[\"g.F2Tl1IeT8UXipnEw$integration-test-1\",\"g.F2Tl1IeT8UXipnEw$integration-test-2\",\"g.HbhIwSZnNYDMQG9W$integration-test-1\",\"g.HbhIwSZnNYDMQG9W$integration-test-2\",\"g.mcgj0W3s5jqoujtZ$integration-test-1\",\"g.mcgj0W3s5jqoujtZ$integration-test-2\",\"integration-move-pad-move\",\"integration-test-pad\",\"integration-test-pad-1\",\"integration-test-pad-2\",\"integration-test-pad-copy\"]}}");

        mockRequest("deletePad",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad-1"),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        mockRequest("deletePad",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad-2"),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        String pad1 = "integration-test-pad-1";
        String pad2 = "integration-test-pad-2";
        client.createPad(pad1);
        client.createPad(pad2);
        Thread.sleep(100);
        List padIDs = (List) client.listAllPads().get("padIDs");
        client.deletePad(pad1);
        client.deletePad(pad2);

        assertTrue(String.format("Size was %d", padIDs.size()),padIDs.size() >= 2);
        assertTrue(padIDs.contains(pad1));
        assertTrue(padIDs.contains(pad2));
    }

    @Test
    public void create_pad_and_chat_about_it() {
        long time1 = System.currentTimeMillis() / 1000L;
        long time2 = System.currentTimeMillis() / 1000L;

        mockRequest("createAuthorIfNotExistsFor",
                new StringBody("apikey=" + APIKEY + "&name=integration-author-1&authorMapper=user1"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"authorID\":\"a.W3CerL3LH1xgvu3m\"}}");

        mockRequest("createAuthorIfNotExistsFor",
                new StringBody("apikey=" + APIKEY + "&name=integration-author-2&authorMapper=user2"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"authorID\":\"a.yUKBa4lV71Rmj8C8\"}}");

        mockRequest("createPad",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad-1"),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        mockRequest("appendChatMessage",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad-1&text=hi+from+user1&authorID=a.W3CerL3LH1xgvu3m"),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        mockRequest("listPadsOfAuthor",
                new StringBody("apikey=" + APIKEY + "&authorID=a.W3CerL3LH1xgvu3m"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"padIDs\":[\"integration-test-pad-1\"]}}");

        mockRequest("appendChatMessage",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad-1&text=hi+from+user2&time=" + time1 + "&authorID=a.yUKBa4lV71Rmj8C8"),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        mockRequest("appendChatMessage",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad-1&text=g%C3%A5+%C3%A5+gj%C3%B8r+et+%C3%A6rend&time=" + time1 + "&authorID=a.W3CerL3LH1xgvu3m"),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        mockRequest("getChatHead",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad-1"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"chatHead\":2}}");

        mockRequest("getChatHistory",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad-1"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"messages\":[{\"text\":\"hi from user1\",\"userId\":\"a.W3CerL3LH1xgvu3m\",\"time\":" + time1 +",\"userName\":\"integration-author-1\"},{\"text\":\"hi from user2\",\"userId\":\"a.yUKBa4lV71Rmj8C8\",\"time\":" + time2 +",\"userName\":\"integration-author-2\"},{\"text\":\"gå å gjør et ærend\",\"userId\":\"a.W3CerL3LH1xgvu3m\",\"time\":" + time2 + ",\"userName\":\"integration-author-1\"}]}}");

        mockRequest("getChatHistory",
                new StringBody("apikey=" + APIKEY + "&start=0&padID=integration-test-pad-1&end=1"),
                "{\"code\":0,\"message\":\"ok\",\"data\":{\"messages\":[{\"text\":\"hi from user1\",\"userId\":\"a.W3CerL3LH1xgvu3m\",\"time\":" + time1 +",\"userName\":\"integration-author-1\"},{\"text\":\"hi from user2\",\"userId\":\"a.yUKBa4lV71Rmj8C8\",\"time\":" + time2 +",\"userName\":\"integration-author-2\"}]}}");

        mockRequest("deletePad",
                new StringBody("apikey=" + APIKEY + "&padID=integration-test-pad-1"),
                "{\"code\":0,\"message\":\"ok\",\"data\":null}");

        String padID = "integration-test-pad-1";
        String user1 = "user1";
        String user2 = "user2";
        Map response = client.createAuthorIfNotExistsFor(user1, "integration-author-1");
        String author1Id = (String) response.get("authorID");
        response = client.createAuthorIfNotExistsFor(user2, "integration-author-2");
        String author2Id = (String) response.get("authorID");

        client.createPad(padID);
        try {
            client.appendChatMessage(padID, "hi from user1", author1Id);

            response = client.listPadsOfAuthor(author1Id);
            List padsAuthor1 = (List) response.get("padIDs");
            assertEquals(1, padsAuthor1.size());

            client.appendChatMessage(padID, "hi from user2", author2Id, time1);
            client.appendChatMessage(padID, "gå å gjør et ærend", author1Id, time2);
            response = client.getChatHead(padID);
            long chatHead = (long) response.get("chatHead");
            assertEquals(2, chatHead);

            response = client.getChatHistory(padID);
            List chatHistory = (List) response.get("messages");
            assertEquals(3, chatHistory.size());
            assertEquals("gå å gjør et ærend", ((Map)chatHistory.get(2)).get("text"));

            response = client.getChatHistory(padID, 0, 1);
            chatHistory = (List) response.get("messages");
            assertEquals(2, chatHistory.size());
            assertEquals("hi from user2", ((Map)chatHistory.get(1)).get("text"));
        } finally {
            client.deletePad(padID);
        }
    }

    @Test
    public void is_secure_or_not() {
        assertFalse(client.isSecure());

        EPLiteClient secureClient = new EPLiteClient("http://localhost:443", APIKEY);
        assertTrue(secureClient.isSecure());
    }
}
