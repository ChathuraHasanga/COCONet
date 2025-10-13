package com.coconetgo.global;

public class Order {
    public String orderId;
    public String buyerId;
    public String buyerName;
    public String sellerId;
    public String sellerName;
    public int quantity;
    public double price;
    public String status; // pending, accepted, rejected, completed
    public long timestamp;
    public String notes ,type;

    public Order() { } // Firebase empty constructor

    public Order(String orderId, String buyerId, String buyerName, String sellerId, String sellerName, int quantity, double price, String status, long timestamp, String notes, String type) {
        this.orderId = orderId;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.quantity = quantity;
        this.price = price;
        this.status = status;
        this.timestamp = timestamp;
        this.notes = notes;
        this.type = type;
    }
}
