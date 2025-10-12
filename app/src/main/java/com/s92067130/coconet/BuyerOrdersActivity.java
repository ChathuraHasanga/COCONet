package com.s92067130.coconet;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class BuyerOrdersActivity extends AppCompatActivity {
    private NetworkHelper networkHelper;
    private TextView offlineBanner;

    private RecyclerView recyclerView;
    private BuyerOrderAdapter adapter;
    private List<Order> orderList = new ArrayList<>();
    private DatabaseReference buyerOrdersRef;
    private String buyerUid;

    private ValueEventListener buyerOrdersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //hide the action bar
        if (getSupportActionBar() != null){
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_buyer_orders);

        offlineBanner = findViewById(R.id.offlineBanner);

        networkHelper = new NetworkHelper(this);
        networkHelper.registerNetworkCallback(offlineBanner);

        recyclerView = findViewById(R.id.recyclerViewBuyerOrders);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        buyerUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        buyerOrdersRef = FirebaseDatabase.getInstance("https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("buyerOrders")
                .child(buyerUid);

        adapter = new BuyerOrderAdapter(orderList);
        recyclerView.setAdapter(adapter);

        loadBuyerOrders();
    }

    private void loadBuyerOrders(){
        buyerOrdersListener =new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                orderList.clear();
                for (DataSnapshot orderSnapshot : snapshot.getChildren()){
                    Order order = orderSnapshot.getValue(Order.class);
                    if (order != null) {
                        orderList.add(order);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(BuyerOrdersActivity.this, "Failed to load orders: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        buyerOrdersRef.addValueEventListener(buyerOrdersListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove Firebase listener to avoid leaks
        if (buyerOrdersRef != null && buyerOrdersListener != null) {
            buyerOrdersRef.removeEventListener(buyerOrdersListener);
            buyerOrdersListener = null;
        }
        networkHelper.unregisterNetworkCallback();
    }

    public void OnClickBtnBackPurchases(View view) {
        try {
            finish();
        }catch (Exception e){
            Toast.makeText(this, "Navigation Error : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
