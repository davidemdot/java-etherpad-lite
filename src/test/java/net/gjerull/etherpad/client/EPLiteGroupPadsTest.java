package net.gjerull.etherpad.client;

import org.graphwalker.java.annotation.Model;
import org.graphwalker.java.annotation.Vertex;
import org.graphwalker.java.annotation.Edge;

@Model(file = "net/gjerull/etherpad/client/EPLiteGroupPads.graphml")
public interface EPLiteGroupPadsTest {

    @Vertex()
    void v_PadIntoGroup();

    @Edge()
    void e_connection();

    @Edge()
    void e_setPublicStatus();

    @Edge()
    void e_createGroup();

    @Edge()
    void e_deleteGroup();

    @Edge()
    void e_deletePad();

    @Edge()
    void e_setPassword();

    @Vertex()
    void v_GroupWithoutPads();

    @Edge()
    void e_getPublicStatus();

    @Edge()
    void e_isPasswordProtected();

    @Edge()
    void e_createGroupPad();

    @Vertex()
    void v_NoGroups();
}
