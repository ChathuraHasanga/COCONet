package com.s92067130.coconet;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class SellActivity extends AppCompatActivity {

    private EditText etQuantity, etPrice, etNotes;
    private Button btnSubmit;

    private String buyerId;
    private DatabaseReference dbRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        //hide the action bar if not null
        if (getSupportActionBar() != null){
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_sell);

        etQuantity = findViewById(R.id.etQuantity);
        etPrice = findViewById(R.id.etPrice);
        etNotes = findViewById(R.id.etNotes);
        btnSubmit = findViewById(R.id.btnSubmit);

        // Get store info from intent
        buyerId = getIntent().getStringExtra("userId");
        dbRef = FirebaseDatabase.getInstance(
                "https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app"
        ).getReference();

        btnSubmit.setOnClickListener(v -> submitSale());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void submitSale() {
        String quantityStr = etQuantity.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        if (TextUtils.isEmpty(quantityStr) || TextUtils.isEmpty(priceStr)) {
            Toast.makeText(this, "Please enter quantity and price", Toast.LENGTH_SHORT).show();
            return;
        }

        int quantity;
        double price;
        try {
            quantity = Integer.parseInt(quantityStr);
            price = Double.parseDouble(priceStr);
        }catch (NumberFormatException e){
            Toast.makeText(this, "Invalid number format or price.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (quantity <= 0 || price <= 0) {
            Toast.makeText(this, "Quantity and price must be greater than zero", Toast.LENGTH_SHORT).show();
            return;
        }

        String sellerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference stockRef = dbRef.child("users").child(sellerId).child("stock_data");

        stockRef.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                Toast.makeText(this, "No stock record found for you!", Toast.LENGTH_SHORT).show();
                return;
            }
                int availableStock = 0;

                for (DataSnapshot stockSnap : snapshot.getChildren()) {
                    Integer qty = stockSnap.child("quantity").getValue(Integer.class);
                    if (qty != null) {
                        availableStock += qty;
                    }
                }

                if (availableStock < quantity) {
                    Toast.makeText(this, "Not enough coconuts in stock. Available: " + availableStock + " kg", Toast.LENGTH_LONG).show();
                    return;
                }
            // Fetch seller storeName
            dbRef.child("users").child(sellerId).child("storeName").get()
                    .addOnSuccessListener(sellerSnap -> {
                        String sellerStoreName = sellerSnap.getValue(String.class);

                        // Fetch buyer storeName
                        dbRef.child("users").child(buyerId).child("storeName").get()
                                .addOnSuccessListener(buyerSnap -> {
                                    String buyerStoreName = buyerSnap.getValue(String.class);

                                    String orderId = dbRef.child("orders").child(buyerId).push().getKey();
                                    if (orderId == null) {
                                        Toast.makeText(this, "Failed to create order ID", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    Order order = new Order(orderId, sellerId, sellerStoreName, buyerId, buyerStoreName, quantity, price, "pending", System.currentTimeMillis(), notes, "sell");

                                    Map<String, Object> updates = new HashMap<>();
                                    updates.put("orders/" + buyerId + "/" + orderId, order);
                                    updates.put("buyerOrders/" + sellerId + "/" + orderId, order);

                                    dbRef.updateChildren(updates)
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(this, "Sale sent as order!", Toast.LENGTH_SHORT).show();
                                                finish(); // close activity after success
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                });
                    });
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error Checking stock: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}