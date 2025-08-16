package com.s92067130.coconet;

import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ContactActivity extends AppCompatActivity {

    private TextView infoContactSnippet;
    private Button contactBtn;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Remove title & hide action bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_contact);

        // Initialize views
        infoContactSnippet = findViewById(R.id.infoContactSnippet);
        contactBtn = findViewById(R.id.contactBtn);

        // Handle edge-to-edge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get UID from Intent
        userId = getIntent().getStringExtra("userId");

        if (userId != null) {
            loadUserContact(userId);
        } else {
            Toast.makeText(this, "No user selected", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Load selected user's details into the existing card in XML
     */
    private void loadUserContact(String uid) {
        DatabaseReference databaseRef = FirebaseDatabase.getInstance(
                        "https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("users")
                .child(uid);

        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(ContactActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Get user details
                String name = snapshot.child("name").getValue(String.class);
                String location = snapshot.child("locationTxt").getValue(String.class);
                Long quantity = snapshot.child("quantity").getValue(Long.class);
                String contact = snapshot.child("contactNumber").getValue(String.class);
                String district = snapshot.child("district").getValue(String.class);
                String email = snapshot.child("email").getValue(String.class);

                // Store basic details
                String storeName = snapshot.child("storeName").getValue(String.class);

                // Get latest stock_data entry (quantity + date)
                String latestStockDate = "N/A";
                Long latestQuantity = null;

                DataSnapshot stockDataNode = snapshot.child("stock_data");
                if (stockDataNode.exists()) {
                    for (DataSnapshot stockSnap : stockDataNode.getChildren()) {

                        storeName = stockSnap.child("storeName").getValue(String.class);
                        latestQuantity = stockSnap.child("quantity").getValue(Long.class);
                        latestStockDate = stockSnap.child("date").getValue(String.class);
                    }
                }

                if (name != null && location != null && storeName != null) {
                    String info = "Owner Name : " + name +
                            "\nStore Name : " + storeName +
                            "\nDistrict : " + district +
                            "\nStock Date : " + latestStockDate +
                            "\nStock Quantity : " + (latestQuantity != null ? latestQuantity : "N/A") +
                            "\nContact : " + contact +
                            "\nEmail : " + email;
                    infoContactSnippet.setText(info);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ContactActivity.this, "Failed to load user", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Back button
    public void OnClickBtnBackContact(View view) {
        finish();
    }

    public void OnClickBtnBackMap(View view) {
        finish();
    }
}
