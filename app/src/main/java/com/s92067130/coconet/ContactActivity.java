package com.s92067130.coconet;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * ContactActivity is responsible for displaying the contact details of a selected user
 * retrieved from Firebase Realtime Database. It shows details such as owner's name,
 * store name, district, contact number, email, and stock information.
 */
public class ContactActivity extends AppCompatActivity {

    private TextView infoContactSnippet;
    private Button contactBtn, btnFavorite, btnSell, btnRequestBuy;
    private String userId, storeName,ownerName, district, contactNumber;

    /**
     * Called when the activity is first created.
     * Initializes UI, retrieves intent extras, and loads user contact information.
     *
     * @param savedInstanceState The saved state of the activity (if any).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Remove title and hide action bar for fullscreen layout
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_contact);

        // Initialize views from XML
        infoContactSnippet = findViewById(R.id.infoContactSnippet);
        contactBtn = findViewById(R.id.contactBtn);
        btnFavorite = findViewById(R.id.btnFavourite);
        btnSell = findViewById(R.id.btnSell);
        btnRequestBuy = findViewById(R.id.btnRequestBuy);

        // Get UID from Intent
        userId = getIntent().getStringExtra("userId");

        // If a user is selected, load details, otherwise show an error toast
        if (userId != null) {
            loadUserContact(userId);
        } else {
            Toast.makeText(this, "No user selected", Toast.LENGTH_SHORT).show();
        }

        btnFavorite.setOnClickListener(v -> addToFavorites());
        btnRequestBuy.setOnClickListener(v -> showOrderDialog());

        btnSell.setOnClickListener(v -> {
            Intent intent = new Intent(ContactActivity.this, SellActivity.class);
            intent.putExtra("userId", userId);   // pass store/seller id
            intent.putExtra("storeName", storeName);
            startActivity(intent);
        });

        // Handle edge-to-edge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

    }

    /**
     * Loads the contact information of the selected user from Firebase Realtime Database.
     *
     * @param uid The unique user ID to fetch details from the database.
     * @return void (nothing is returned, UI is updated directly).
     */
    private void loadUserContact(String uid) {
        // Reference to the user's node in Firebase
        DatabaseReference databaseRef = FirebaseDatabase.getInstance(
                        "https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("users")
                .child(uid);

        // Fetch data once using addListenerForSingleValueEvent
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {

            /**
             * Called when the data is successfully retrieved from Firebase.
             *
             * @param snapshot DataSnapshot containing the user's information.
             */
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(ContactActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Assign values to class-level variables for favorites
                storeName = snapshot.child("storeName").getValue(String.class);
                ownerName = snapshot.child("name").getValue(String.class);
                district = snapshot.child("district").getValue(String.class);
                contactNumber = snapshot.child("contactNumber").getValue(String.class);

                // Extract basic user details
                String name = snapshot.child("name").getValue(String.class);
                String location = snapshot.child("locationTxt").getValue(String.class);
                Long quantity = snapshot.child("quantity").getValue(Long.class);
                String contact = snapshot.child("contactNumber").getValue(String.class);
                String district = snapshot.child("district").getValue(String.class);
                String email = snapshot.child("email").getValue(String.class);

                // Get latest stock_data entry (quantity + date)
                long totalQuantity =0;
                String latestStockDate = "N/A";
                long latestTimestamp = Long.MIN_VALUE;

                DataSnapshot stockDataNode = snapshot.child("stock_data");
                if (stockDataNode.exists()) {

                    for (DataSnapshot stockSnap : stockDataNode.getChildren()) {

                        String name1 = stockSnap.child("storeName").getValue(String.class);
                        Long quantity1 = stockSnap.child("quantity").getValue(Long.class);
                        String date = stockSnap.child("date").getValue(String.class);

                        if (name1 != null) storeName = name1;

                        if (quantity1 != null) totalQuantity += quantity1;

                        // Assuming date is stored as a timestamp string
                        if (date != null) {
                            try {
                                long time = Long.parseLong(date);
                                if (time > latestTimestamp) {
                                    latestTimestamp = time;
                                    latestStockDate = date;
                                }
                            } catch (NumberFormatException e) {
                                // If not timestamp, just take last string
                                latestStockDate = date;
                            }
                        }
                    }
                }

                // Final values
                String totalQtyStr = totalQuantity > 0 ? totalQuantity + " Kg" : "N/A";

                // Update UI only if important details exist
                    String info = "Owner Name : " + (name != null ? name : "N/A") +
                            "\nStore Name : " + (storeName != null ? storeName : "N/A") +
                            "\nDistrict : " + (district != null ? district : "N/A") +
                            "\nStock Date : " + latestStockDate +
                            "\nStock Amount(Kg) : " + totalQtyStr +
                            "\nContact : " + (contact != null ? contact : "N/A") +
                            "\nEmail : " + email;
                    infoContactSnippet.setText(info);

            }

            /**
             * Called when there is an error retrieving the data from Firebase.
             *
             * @param error DatabaseError containing the failure reason.
             */
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ContactActivity.this, "Failed to load user", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addToFavorites(){
        if (userId != null){

            // First show a dialog to ask for a note
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_input_note, null);
            EditText etNote = dialogView.findViewById(R.id.etNote);
            Button btnSave = dialogView.findViewById(R.id.btnSaveNote);
            Button btnCancel = dialogView.findViewById(R.id.btnCancelNote);

            builder.setView(dialogView);
            AlertDialog dialog = builder.create();
            dialog.show();

            btnSave.setOnClickListener(v -> {
                String note = etNote.getText().toString().trim();

                DatabaseReference favRef = FirebaseDatabase.getInstance("https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app")
                        .getReference("users")
                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .child("favorites")
                        .child(userId);
                ;

                favRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {

                            Toast.makeText(ContactActivity.this, "Already in favorites", Toast.LENGTH_SHORT).show();
                        } else {
                            Map<String, Object> favData = new HashMap<>();
                            favData.put("storeId", userId);
                            favData.put("storeName", storeName != null ? storeName : "");
                            favData.put("name", ownerName != null ? ownerName : "");
                            favData.put("district", district != null ? district : "");
                            favData.put("contactNumber", contactNumber != null ? contactNumber : "");
                            favData.put("note", note);

                            favRef.setValue(favData)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(ContactActivity.this, "Added to favorites", Toast.LENGTH_SHORT).show();

                                        //Log action
                                        logUserAction("favorite_added", userId, storeName);
                                        dialog.dismiss();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(ContactActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ContactActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            });
            btnCancel.setOnClickListener(v-> dialog.dismiss());
        }
    }

    private void logUserAction(String action, String storeId, String storeName){
        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference logsRef = FirebaseDatabase.getInstance("https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("logs").child("users").child(currentUid);

        String logId = logsRef.push().getKey();

        Map<String, Object> logData = new HashMap<>();
        logData.put("action", action);
        logData.put("storeId", storeId);
        logData.put("storeName", storeName);
        logData.put("timestamp", System.currentTimeMillis());

        logsRef.child(logId).setValue(logData);
    }

    private void showOrderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_order_request, null);
        EditText etQuantity = view.findViewById(R.id.etQuantity);
        EditText etPrice = view.findViewById(R.id.etPrice);
        Button btnSubmit = view.findViewById(R.id.btnSubmitOrder);

        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.show();

        btnSubmit.setOnClickListener(v -> {
            String qtyStr = etQuantity.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();

            if (qtyStr.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Enter all details", Toast.LENGTH_SHORT).show();
                return;
            }

            int qty = Integer.parseInt(qtyStr);
            double price = Double.parseDouble(priceStr);
            if (qty <= 0 || price <= 0) {
                Toast.makeText(this, "Quantity and Price must be greater than zero", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check seller stock before creating order
            DatabaseReference stockRef = FirebaseDatabase.getInstance(
                            "https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .getReference("users").child(userId).child("stock_data");

            stockRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    long totalStock = 0;
                    for (DataSnapshot stockSnap : snapshot.getChildren()) {
                        Long q = stockSnap.child("quantity").getValue(Long.class);
                        if (q != null) totalStock += q;
                    }

                    if (qty > totalStock) {
                        Toast.makeText(ContactActivity.this,
                                "Requested amount exceeds seller’s stock. Available: " + totalStock + " Kg",
                                Toast.LENGTH_LONG).show();
                    } else {
                        createOrder(qty, price); // only create if valid
                        dialog.dismiss();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(ContactActivity.this, "Failed to check stock: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void createOrder(int quantity, double price) {
        String buyerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String sellerId = userId;
        String sellerName = storeName;
        DatabaseReference ordersRef = FirebaseDatabase.getInstance(
                        "https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference();

        ordersRef.child("users").child(buyerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String buyerName = snapshot.child("storeName").getValue(String.class);

                String orderId = ordersRef.child("orders").child(sellerId).push().getKey();

                if (orderId == null){
                    Toast.makeText(ContactActivity.this, "order failed: could not create ID", Toast.LENGTH_SHORT).show();
                    return;
                }
                Order order = new Order(orderId, buyerId, buyerName,sellerId, sellerName, quantity, price, "pending", System.currentTimeMillis(),"", "buy");

                Map<String, Object> updates= new HashMap<>();
                updates.put("orders/" + sellerId + "/" + orderId, order);
                updates.put("buyerOrders/" + buyerId + "/" + orderId, order);

                ordersRef.updateChildren(updates).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ContactActivity.this, "Order sent to seller!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ContactActivity.this, "Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ContactActivity.this, "Failed to get buyer info: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }



    /**
     * Handles the back button click on Contact screen.
     *
     * @param view The view that triggered this method.
     * @return void (closes the activity).
     */
    public void OnClickBtnBackContact(View view) {
        finish();
    }

    /**
     * Handles the back button click from Map screen (if navigated here).
     *
     * @param view The view that triggered this method.
     * @return void (closes the activity).
     */
    public void OnClickBtnBackMap(View view) {
        finish();
    }
}
