package com.ritesh.hoppeconnect;

public class NotificationModel {
    public static final int TYPE_ANNOUNCEMENT = 0;
    public static final int TYPE_REPORT       = 1;
    public static final int TYPE_MY_REPORT    = 2;

    public String id, title, body, time, photoUrl;
    public boolean isRead;
    public int type;

    public NotificationModel(String id, String title, String body,
                             String time, String photoUrl,
                             boolean isRead, int type) {
        this.id       = id;
        this.title    = title;
        this.body     = body;
        this.time     = time;
        this.photoUrl = photoUrl;
        this.isRead   = isRead;
        this.type     = type;
    }
}