package com.s92067130.coconet;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BuyerOrderAdapter extends RecyclerView.Adapter<BuyerOrderAdapter.BuyerOrderViewHolder>{

    private List<Order> orderList;

    public BuyerOrderAdapter(List<Order> orderList){
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public BuyerOrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate your item layout and create ViewHolder
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_buyer_order, parent, false);
        return new BuyerOrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BuyerOrderViewHolder holder, int position) {
        // Bind data to your ViewHolder
        Order order = orderList.get(position);
        if (order != null) {
            holder.tvSellerName.setText("Seller: " + (order.sellerName != null ? order.sellerName : "N/A"));
            holder.tvQuantity.setText("Quantity: " + order.quantity + " Kg");
            holder.tvPrice.setText("Price Per Kg: Rs. " + order.price);
            holder.tvStatus.setText("Status: " + (order.status != null ? order.status : "Unknown"));
            holder.tvType.setText("Type: " + (order.type != null ? order.type : "N/A"));
        }
        
        //status color
        switch (order.status){
            case "accepted":
                holder.tvStatus.setTextColor(Color.parseColor("#4CAF50")); // Green
                break;
            case "rejected":
                holder.tvStatus.setTextColor(Color.parseColor("#F44336")); // Red
                break;
            case "completed":
                holder.tvStatus.setTextColor(Color.parseColor("#2196F3")); // Blue
                break;
            default:
                holder.tvStatus.setTextColor(Color.parseColor("#FF9800")); // Orange for pending
                break;
        }
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    static class BuyerOrderViewHolder extends RecyclerView.ViewHolder{
        TextView tvSellerName, tvQuantity, tvPrice, tvStatus, tvType;

        public BuyerOrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSellerName = itemView.findViewById(R.id.tvSellerName);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvType = itemView.findViewById(R.id.tvType);
        }
    }
}
