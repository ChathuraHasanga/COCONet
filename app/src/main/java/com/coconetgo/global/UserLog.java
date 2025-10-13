package com.coconetgo.global;

public class UserLog {
    private String action; // "login" or "logout"
    private long timestamp;

    public UserLog() { } // Needed for Firebase

    public UserLog(String action, long timestamp) {
        this.action = action;
        this.timestamp = timestamp;
    }

    public String getAction() { return action; }
    public long getTimestamp() { return timestamp; }
}

