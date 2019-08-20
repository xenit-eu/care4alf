package eu.xenit.care4alf.permissionimport.reader;

import java.util.Arrays;

public class PermissionSetting {

    private String[] path;
    private String group;
    private String permission;

    public boolean isInherit() {
        return inherit;
    }

    public void setInherit(boolean inherit) {
        this.inherit = inherit;
    }

    private boolean inherit;

    public String[] getPath() {
        return path;
    }

    public void setPath(String[] path) {
        this.path = path;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    @Override
    public String toString(){
        StringBuilder builder = new StringBuilder();
        builder.append("Group: "+getGroup()+"\n");
        builder.append("Permission: "+getPermission()+"\n");
        builder.append("Path: "+ Arrays.toString(getPath())+"\n");
        builder.append("Inherit: "+isInherit()+"\n");
        return builder.toString();
    }
}
