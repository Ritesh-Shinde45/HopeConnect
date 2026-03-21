package com.ritesh.hoppeconnect;

public class ActivityLogItem {
    public String text, time, type; // type: "success","warning","danger","info"

    public ActivityLogItem(String text, String time, String type) {
        this.text = text; this.time = time; this.type = type;
    }
}