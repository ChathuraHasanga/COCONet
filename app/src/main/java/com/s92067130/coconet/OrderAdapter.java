package com.s92067130.coconet;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private List<Order> orderList;

    public OrderAdapter(List<Order> orderList) {
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        holder.tvBuyerName.setText("Buyer: " + order.buyerName);
        holder.tvQuantity.setText("Amount: " + order.quantity + " Kg");
        holder.tvPrice.setText("Price Per Kg: Rs. " + order.price);
        holder.tvStatus.setText("Status: " + order.status);
        holder.tvType.setText("Type: " + order.type);

        // Hide buttons depending on status
        if ("completed".equals(order.status) || "rejected".equals(order.status)) {
            holder.btnAccept.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);
            holder.btnComplete.setVisibility(View.GONE);
        } else if ("accepted".equals(order.status)) {
            holder.btnAccept.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);
            holder.btnComplete.setVisibility(View.VISIBLE);
        } else { // pending
            holder.btnAccept.setVisibility(View.VISIBLE);
            holder.btnReject.setVisibility(View.VISIBLE);
            holder.btnComplete.setVisibility(View.GONE);
        }

        holder.btnAccept.setOnClickListener(v -> updateOrderStatus(order, "accepted", holder));
        holder.btnReject.setOnClickListener(v -> updateOrderStatus(order, "rejected", holder));

        holder.btnComplete.setOnClickListener(v -> {
            new AlertDialog.Builder(holder.itemView.getContext())
                    .setTitle("Complete Order")
                    .setMessage("Are you sure you want to mark this order as completed?\n" +
                            "Quantity: " + order.quantity + " Kg\n" +
                            "Price Per Kg: Rs. " + order.price + "\n" +
                            "Total Price: Rs. " + (order.quantity * order.price))
                    .setPositiveButton("Yes", (dialog, which) -> completeOrder(order, holder))
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvBuyerName, tvQuantity, tvPrice, tvStatus, tvType;
        Button btnAccept, btnReject, btnComplete;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBuyerName = itemView.findViewById(R.id.tvBuyerName);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvType = itemView.findViewById(R.id.tvType);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnComplete = itemView.findViewById(R.id.btnComplete);
        }
    }

    private void updateOrderStatus(Order order, String newStatus, OrderViewHolder holder) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("orders/" + order.sellerId + "/" + order.orderId + "/status", newStatus);
        updates.put("buyerOrders/" + order.buyerId + "/" + order.orderId + "/status", newStatus);

        FirebaseDatabase.getInstance(
                        "https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference()
                .updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    order.status = newStatus;
                    holder.tvStatus.setText("Status: " + newStatus);
                    Toast.makeText(holder.itemView.getContext(), "Order " + newStatus, Toast.LENGTH_SHORT).show();
                    notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(holder.itemView.getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void completeOrder(Order order, OrderViewHolder holder) {
        DatabaseReference db = FirebaseDatabase.getInstance(
                "https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();

        double qty = order.quantity;
        double totalAmount = qty * order.price;

        // 1. Run transaction for seller stock
        DatabaseReference sellerStockRef = db.child("stock").child(order.sellerId).child("quantity");
        DatabaseReference buyerStockRef = db.child("stock").child(order.buyerId).child("quantity");

        // 2. Run transaction for wallets
        DatabaseReference sellerWalletRef = db.child("wallets").child(order.sellerId).child("balance");
        DatabaseReference buyerWalletRef = db.child("wallets").child(order.buyerId).child("balance");

        if ("sell".equals(order.type)) {
            // Use transactions for concurrency safety
            sellerStockRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
                @NonNull
                @Override
                public com.google.firebase.database.Transaction.Result doTransaction(@NonNull MutableData currentData) {
                    Double value = currentData.getValue(Double.class);
                    if (value == null) value = 0.0;
                    if (value < qty)
                        return com.google.firebase.database.Transaction.abort(); // Not enough stock
                    currentData.setValue(value - qty);
                    return com.google.firebase.database.Transaction.success(currentData);
                }

                @Override
                public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                }
            });

            buyerStockRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
                @NonNull
                @Override
                public com.google.firebase.database.Transaction.Result doTransaction(@NonNull MutableData currentData) {
                    Double value = currentData.getValue(Double.class);
                    if (value == null) value = 0.0;
                    currentData.setValue(value + qty);
                    return com.google.firebase.database.Transaction.success(currentData);
                }

                @Override
                public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                }
            });

            sellerWalletRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
                @NonNull
                @Override
                public com.google.firebase.database.Transaction.Result doTransaction(@NonNull MutableData currentData) {
                    Double value = currentData.getValue(Double.class);
                    if (value == null) value = 0.0;
                    currentData.setValue(value + totalAmount);
                    return com.google.firebase.database.Transaction.success(currentData);
                }

                @Override
                public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                }
            });

            buyerWalletRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
                @NonNull
                @Override
                public com.google.firebase.database.Transaction.Result doTransaction(@NonNull MutableData currentData) {
                    Double value = currentData.getValue(Double.class);
                    if (value == null) value = 0.0;
                    if (value < totalAmount)
                        return com.google.firebase.database.Transaction.abort(); // Not enough balance
                    currentData.setValue(value - totalAmount);
                    return com.google.firebase.database.Transaction.success(currentData);
                }

                @Override
                public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                }
            });

        } else if ("buy".equals(order.type)) {
            sellerStockRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
                @NonNull
                @Override
                public com.google.firebase.database.Transaction.Result doTransaction(@NonNull MutableData currentData) {
                    Double value = currentData.getValue(Double.class);
                    if (value == null) value = 0.0;
                    currentData.setValue(value + qty);
                    return com.google.firebase.database.Transaction.success(currentData);
                }

                @Override
                public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                }
            });

            buyerStockRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
                @NonNull
                @Override
                public com.google.firebase.database.Transaction.Result doTransaction(@NonNull MutableData currentData) {
                    Double value = currentData.getValue(Double.class);
                    if (value == null) value = 0.0;
                    if (value < qty)
                        return com.google.firebase.database.Transaction.abort();
                    currentData.setValue(value - qty);
                    return com.google.firebase.database.Transaction.success(currentData);
                }

                @Override
                public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                }
            });

            sellerWalletRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
                @NonNull
                @Override
                public com.google.firebase.database.Transaction.Result doTransaction(@NonNull MutableData currentData) {
                    Double value = currentData.getValue(Double.class);
                    if (value == null) value = 0.0;
                    currentData.setValue(value + totalAmount);
                    return com.google.firebase.database.Transaction.success(currentData);
                }

                @Override
                public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                }
            });

            buyerWalletRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
                @NonNull
                @Override
                public com.google.firebase.database.Transaction.Result doTransaction(@NonNull MutableData currentData) {
                    Double value = currentData.getValue(Double.class);
                    if (value == null) value = 0.0;
                    if (value < totalAmount)
                        return com.google.firebase.database.Transaction.abort(); // Not enough balance
                    currentData.setValue(value - totalAmount);
                    return com.google.firebase.database.Transaction.success(currentData);
                }

                @Override
                public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                }
            });
        }
        // 3. Update order status to completed
        Map<String, Object> updates = new HashMap<>();
        updates.put("orders/" + order.sellerId + "/" + order.orderId + "/status", "completed");
        updates.put("buyerOrders/" + order.buyerId + "/" + order.orderId + "/status", "completed");

        db.updateChildren(updates).addOnSuccessListener(aVoid -> {
            order.status = "completed";
            holder.tvStatus.setText("Status: completed");
            Toast.makeText(holder.itemView.getContext(), "Order completed successfully!", Toast.LENGTH_SHORT).show();
            notifyDataSetChanged();

            long timestamp = System.currentTimeMillis();

            // Format date
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String formattedDate = sdf.format(new Date(timestamp));

            // 1. Create seller transaction (credit)
            Transaction sellerTxn = new Transaction("credit", totalAmount, timestamp, "Order #" + order.orderId);
            db.child("transactions").child(order.sellerId).push().setValue(sellerTxn);

            // 2. Create buyer transaction (debit)
            Transaction buyerTxn = new Transaction("debit", totalAmount, timestamp, "Order #" + order.orderId);
            db.child("transactions").child(order.buyerId).push().setValue(buyerTxn);

            // Get seller store name and buyer store name from /users/{uid}/storeName
            DatabaseReference usersRef = db.child("users");

            if (order.type != null && order.type.equals("sell")) {
                usersRef.child(order.sellerId).child("storeName").get().addOnSuccessListener(sellerSnap -> {
                    String sellerStoreName = sellerSnap.getValue(String.class);
                    // 4. Update users/{uid}/stock_data for dashboard reflection
                    DatabaseReference userStockRefSeller = db.child("users").child(order.sellerId).child("stock_data").push();
                    userStockRefSeller.child("quantity").setValue(qty);
                    userStockRefSeller.child("timestamp").setValue(timestamp);
                    userStockRefSeller.child("date").setValue(formattedDate);
                    userStockRefSeller.child("storeName").setValue(sellerStoreName);
                });

                usersRef.child(order.buyerId).child("storeName").get().addOnSuccessListener(buyerSnap -> {
                    String buyerStoreName = buyerSnap.getValue(String.class);

                    DatabaseReference userStockRefBuyer = db.child("users").child(order.buyerId).child("stock_data").push();
                    userStockRefBuyer.child("quantity").setValue(-qty);
                    userStockRefBuyer.child("timestamp").setValue(timestamp);
                    userStockRefBuyer.child("date").setValue(formattedDate);
                    userStockRefBuyer.child("storeName").setValue(buyerStoreName);
                });

            } else if (order.type != null && order.type.equals("buy")) {
                usersRef.child(order.sellerId).child("storeName").get().addOnSuccessListener(sellerSnap -> {
                    String sellerStoreName = sellerSnap.getValue(String.class);
                    // 4. Update users/{uid}/stock_data for dashboard reflection
                    DatabaseReference userStockRefSeller = db.child("users").child(order.sellerId).child("stock_data").push();
                    userStockRefSeller.child("quantity").setValue(-qty);
                    userStockRefSeller.child("timestamp").setValue(timestamp);
                    userStockRefSeller.child("date").setValue(formattedDate);
                    userStockRefSeller.child("storeName").setValue(sellerStoreName);
                });

                usersRef.child(order.buyerId).child("storeName").get().addOnSuccessListener(buyerSnap -> {
                    String buyerStoreName = buyerSnap.getValue(String.class);

                    DatabaseReference userStockRefBuyer = db.child("users").child(order.buyerId).child("stock_data").push();
                    userStockRefBuyer.child("quantity").setValue(+qty);
                    userStockRefBuyer.child("timestamp").setValue(timestamp);
                    userStockRefBuyer.child("date").setValue(formattedDate);
                    userStockRefBuyer.child("storeName").setValue(buyerStoreName);
                });
            }
        }).addOnFailureListener(e ->
                Toast.makeText(holder.itemView.getContext(), "Error completing order: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
