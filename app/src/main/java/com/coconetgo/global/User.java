package com.coconetgo.global;

public class User {
    private String uid;
    private String name;
    private String email;
    private String role;
    private String status;
    private String province; // Add this line

    public User(){}

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status;
    }

    public String getProvince() {
        return province;
    }
    public void setProvince(String province) {
        this.province = province;
    }
}
