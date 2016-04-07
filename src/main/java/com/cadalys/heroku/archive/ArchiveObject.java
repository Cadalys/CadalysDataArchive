package com.cadalys.heroku.archive;

/**
 * Created by dzmitrykalachou on 05.04.16.
 */
public class ArchiveObject {

    private String parentObj;
    private String childObj;
    private String refField;

    public String getRefField() {
        return refField;
    }

    public void setRefField(String refField) {
        this.refField = refField;
    }

    public String getParentObj() {
        return parentObj;
    }

    public void setParentObj(String parentObj) {
        this.parentObj = parentObj;
    }

    public String getChildObj() {
        return childObj;
    }

    public void setChildObj(String childObj) {
        this.childObj = childObj;
    }
}
