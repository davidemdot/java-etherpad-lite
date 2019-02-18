package net.gjerull.etherpad.client;

import org.graphwalker.core.machine.ExecutionContext;
import org.graphwalker.java.annotation.GraphWalker;

@GraphWalker(value = "random(edge_coverage(100))", start = "e_connection")
public class EPLiteGroupPadsTest extends ExecutionContext implements EPLiteGroupPads {

    public void e_connection() {
        System.out.println("Connecting...");
    }

    public void v_NoGroups() {
        System.out.println("There are no groups.");
    }

    public void v_GroupWithoutPads() {
        System.out.println("There is a group without pads.");
    }

    public void v_PadIntoGroup() {
        System.out.println("There is a group pad!");
    }

    public void e_createGroup() {
        System.out.println("Creating a group...");
    }

    public void e_deleteGroup() {
        System.out.println("Deleting the group...");
    }

    public void e_createGroupPad() {
        System.out.println("Creating a group pad...");
    }

    public void e_deletePad() {
        System.out.println("Deleting the group pad...");
    }

    public void e_setPublicStatus() {
        System.out.println("Set public status.");
    }

    public void e_getPublicStatus() {
        System.out.println("Get public status.");
    }

    public void e_setPassword() {
        System.out.println("Set password.");
    }

    public void e_isPasswordProtected() {
        System.out.println("Check if it is protected.");
    }
}
