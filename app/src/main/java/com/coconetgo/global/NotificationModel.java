package com.coconetgo.global;

public class NotificationModel {

    public String title;
    public String body;
    public long timestamp;
    public String type;
    public boolean read;


    public NotificationModel() {
        // Default constructor required for Firebase
    }

    public NotificationModel(String title, String body, long timestamp, String type, boolean read) {
        this.title = title;
        this.body = body;
        this.timestamp = timestamp;
        this.type = type;
        this.read = read;
    }
}
