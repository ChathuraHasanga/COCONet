package com.coconetgo.global;

public class AuditLog {
    private String adminId;
    private String userId;
    private String action;
    private long timestamp;

    public AuditLog() {}

    public AuditLog(String adminId, String userId, String action, long timestamp) {
        this.adminId = adminId;
        this.userId = userId;
        this.action = action;
        this.timestamp = timestamp;
    }

    public String getAdminId() {
        return adminId;
    }

    public String getUserId() {
        return userId;
    }

    public String getAction() {
        return action;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
