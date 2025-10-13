package com.coconetgo.global;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.coconetgo.global.ui.settings.SettingsFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

/**
 * StockInputActivity handles stock entry submission and displays the latest stock entries
 * from all users (within last 3 days).
 *
 * Users can enter store name and quantity, which are stored under Firebase Realtime Database
 * under each user's UID.
 */
public class StockInputActivity extends AppCompatActivity {
    private NetworkHelper networkHelper;
    private TextView offlineBanner;

    private EditText editTextQuantity;
    private Button submitButton;

    // Firebase authentication and database references
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;

    private LinearLayout stockListContainer;

    //store name of current user
    private String currentStoreName = null;

    /**
     * Called when the activity is first created.
     * Initializes UI, Firebase, and loads existing stock entries.
     *
     * @param savedInstanceState Bundle containing saved activity state (can be null)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        try {
            //hide tool bar
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            super.onCreate(savedInstanceState);

            //hide the action bar
            if (getSupportActionBar() != null){
                getSupportActionBar().hide();
            }

            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_stock_input);

            offlineBanner = findViewById(R.id.offlineBanner);

            networkHelper = new NetworkHelper(this);
            networkHelper.registerNetworkCallback(offlineBanner);

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

            // Initialize Firebase authentication and database reference
            mAuth = FirebaseAuth.getInstance();
            databaseReference = FirebaseDatabase.getInstance("https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("users");

            // Initialize UI elements
            editTextQuantity = findViewById(R.id.editTextQuantity);
            submitButton = findViewById(R.id.buttonSubmit);
            stockListContainer = findViewById(R.id.stockListContainer);

            //load user's store name
            fetchUserStoreName();

            //submit button click
            submitButton.setOnClickListener(this::onClickSubmit);

            //load existing stock entries
            loadOwnStockEntries();

            getSupportFragmentManager().addOnBackStackChangedListener(() ->{
                fetchUserStoreName();
            });

        }catch (Exception e){
            Toast.makeText(this, "Initialization error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        networkHelper.unregisterNetworkCallback();
    }

    @Override
    protected void onResume(){
        super.onResume();
        fetchUserStoreName();
    }

    private void fetchUserStoreName(){
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null){
            String userId = currentUser.getUid();
            databaseReference.child(userId).child("storeName")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()){
                                currentStoreName = snapshot.getValue(String.class);
                            }else{
                                currentStoreName = null;
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(StockInputActivity.this, "Failed to load store name.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    /**
     * Handles the stock submission process.
     * Validates input fields, saves stock to Firebase under the current user's UID,
     * and refreshes the displayed stock list.
     *
     * @param view The view that triggered this click event (submit button)
     */
    public void onClickSubmit(View view){
        try {

            if (currentStoreName == null || currentStoreName.trim().isEmpty()){
                new AlertDialog.Builder(this)
                        .setTitle("Store Name Required")
                        .setMessage("You must set your store name in Settings before submitting stock. Do you want to go there now?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(android.R.id.content, new SettingsFragment())
                                    .addToBackStack(null)
                                    .commit();
                        })
                        .setNegativeButton("No", null)
                        .show();
                return;
            }
                String quantityStr = editTextQuantity.getText().toString().trim();

                // Validate input fields
                if (TextUtils.isEmpty(quantityStr)){
                    Toast.makeText(this, "Please enter your coconut amount", Toast.LENGTH_SHORT).show();
                    return;
                }

                int quantity;
                try {
                    quantity = Integer.parseInt(quantityStr);
                }catch(NumberFormatException e){
                    Toast.makeText(this, "Invalid quantity", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (quantity == 0){
                    Toast.makeText(this, "Amount cannot be zero", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Ensure user is logged in
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser == null) {
                    Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
                    return;
                }

                String userId = currentUser.getUid();
                long timestamp = System.currentTimeMillis();
                String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                Stock stock = new Stock(quantity, timestamp, date, currentStoreName);

                //save inside data under users/uid
                databaseReference.child(userId).child("stock_data").push().setValue(stock)
                        .addOnCompleteListener(task -> {
                            Toast.makeText(this, "Stock submitted successfully", Toast.LENGTH_SHORT).show();
                            // Clear input fields
                            editTextQuantity.setText("");
                            // Refresh stock list
                            loadOwnStockEntries(); // Refresh the stock list after submission

                            DatabaseReference stockRef = FirebaseDatabase.getInstance("https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app")
                                    .getReference().child("notifications")
                                    .push();

                            NotificationModel stockNotif = new NotificationModel(
                                    "Fresh Stock Available! \uD83E\uDD65",
                                    currentStoreName+ " just added " + quantity + " coconuts.",
                                    System.currentTimeMillis(),
                                    "stock_update",
                                    false
                            );
                            stockRef.setValue(stockNotif);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to submit: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });

        }catch (Exception e){
            Toast.makeText(this, "Error submitting stock: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Loads the latest stock entry from each user (within the last 3 days)
     * and displays them in the LinearLayout container.
     */
    private void loadOwnStockEntries() {
        try {
            LinearLayout container = findViewById(R.id.stockListContainer);
            container.removeAllViews(); // Clear old entries

            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) return;
            String userId = currentUser.getUid();

            databaseReference.child(userId).child("stock_data")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    try {
                        ArrayList<Stock> stockList = new ArrayList<>();

                        long threeDaysInMillis = 3 * 24 * 60 * 60 * 1000L; // 3 days in milliseconds
                        long currentTime = System.currentTimeMillis();

                            //Get the most recent stock entry for each user
                            for (DataSnapshot entrySnap : snapshot.getChildren()) {
                                Stock stock = entrySnap.getValue(Stock.class);
                                if (stock != null && (currentTime - stock.timestamp <= threeDaysInMillis)) {
                                    stockList.add(stock);
                                }
                            }

                        // Sort by most recent across users
                        Collections.sort(stockList, (a, b) -> Long.compare(b.timestamp, a.timestamp));

                        // Inflate and display each stock entry
                        LayoutInflater inflater = LayoutInflater.from(StockInputActivity.this);
                        for (Stock stock : stockList) {
                            View stockView = inflater.inflate(R.layout.item_stock_entry, stockListContainer, false);

                            TextView dataView = stockView.findViewById(R.id.textViewDate);
                            TextView quantityView = stockView.findViewById(R.id.textViewQuantity);
                            TextView locationView = stockView.findViewById(R.id.textViewLocation);

                            dataView.setText("Date : " + stock.date);
                            quantityView.setText("Amount(Kg) : " + stock.quantity);
                            locationView.setText("Store : " + stock.storeName);

                            container.addView(stockView);
                        }
                    }catch (Exception e){
                        Toast.makeText(StockInputActivity.this, "Error processing stock data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(StockInputActivity.this, "Failed to load stock entries.", Toast.LENGTH_SHORT).show();
                }
            });

        }catch (Exception e){
            Toast.makeText(this, "Error loading stock data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handles the back button click to navigate to MainActivity.
     *
     * @param view The view that triggered this click (back button)
     */
    public void OnClickBtnBack(View view) {
        try {
            // Navigate to MainActivity when the back arrow is clicked
            Intent intent = new Intent(StockInputActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }catch (Exception e){
            Toast.makeText(this, "Error navigating back: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Model class representing a stock entry.
     */
    public static class Stock{
        public int quantity;
        public long timestamp;
        public String date;
        public String storeName;

        public Stock() {
            // Needed for Firebase
        }

        // Constructor to create a stock object
        public Stock (int quantity, long timestamp, String date, String storeName){
            this.quantity = quantity;
            this.timestamp = timestamp;
            this.date = date;
            this.storeName = storeName;
        }

    }
}