package com.s92067130.coconet;

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

