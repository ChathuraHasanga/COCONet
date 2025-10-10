package com.s92067130.coconet;

public class Store {
    private String name;
    private String ownerName;
    private String district;
    private String contactNumber;
    private String note;

    public Store(String storeId, String name, String ownerName,String district, String contactNumber, String note) {
        this.name = name;
        this.ownerName = ownerName;
        this.district = district;
        this.contactNumber = contactNumber;
        this.note = note;
    }

    public String getName() { return name; }
    public String getOwnerName() {
        return ownerName;
    }
    public String getNote() { return note; }
    public String getDistrict() { return district; }
    public String getContactNumber() { return contactNumber; }
}

