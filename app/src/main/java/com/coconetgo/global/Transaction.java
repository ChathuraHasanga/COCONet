package com.coconetgo.global;

public class Transaction {
    public String type; // "credit" or "debit"
    public double amount;
    public long timestamp;
    public String note;

    public Transaction() {} // Default constructor for Firebase

    public Transaction(String type, double amount, long timestamp, String note) {
        this.type = type;
        this.amount = amount;
        this.timestamp = timestamp;
        this.note = note;
    }
}
