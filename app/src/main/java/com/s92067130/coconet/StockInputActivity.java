package com.s92067130.coconet;

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

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public class StockInputActivity extends AppCompatActivity {

    private EditText editTextStoreName, editTextQuantity;
    private Button submitButton;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private LinearLayout stockListContainer;

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
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

            //firebase setup
            mAuth = FirebaseAuth.getInstance();
            databaseReference = FirebaseDatabase.getInstance("https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("users");

            //ui elements
            editTextStoreName = findViewById(R.id.editTextStoreName);
            editTextQuantity = findViewById(R.id.editTextQuantity);
            submitButton = findViewById(R.id.buttonSubmit);
            stockListContainer = findViewById(R.id.stockListContainer);

            //submit button click
            submitButton.setOnClickListener(v -> onClickSubmit(v));

            //load existing stock entries
            loadAllUsersLatestStockEntries();

        }catch (Exception e){
            Toast.makeText(this, "Initialization error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    //Handle stock submission
    public void onClickSubmit(View view){
        try {
            String storeName = editTextStoreName.getText().toString().trim();
            String quantityStr = editTextQuantity.getText().toString().trim();

            if (TextUtils.isEmpty(storeName) || TextUtils.isEmpty(quantityStr)){
                Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int quantity;
            try {
                quantity = Integer.parseInt(quantityStr);
            }catch(NumberFormatException e){
                Toast.makeText(this, "Invalid quantity", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = currentUser.getUid();
            long timestamp = System.currentTimeMillis();
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            Stock stock = new Stock(storeName, quantity, timestamp, date);

            //save inside data under users/uid
            databaseReference.child(userId).child("stock_data").push().setValue(stock)
                    .addOnCompleteListener(task -> {
                        Toast.makeText(this, "Stock submitted successfully", Toast.LENGTH_SHORT).show();
                        editTextStoreName.setText("");
                        editTextQuantity.setText("");
                        loadAllUsersLatestStockEntries(); // Refresh the stock list after submission
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to submit: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }catch (Exception e){
            Toast.makeText(this, "Error submitting stock: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }

    //Load latest stock from each user
    private void loadAllUsersLatestStockEntries() {
        try {
            stockListContainer.removeAllViews(); // Clear old entries

            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    try {
                        ArrayList<Stock> latestStockPerUser = new ArrayList<>();

                        long threeDaysInMillis = 3 * 24 * 60 * 60 * 1000L; // 3 days in milliseconds
                        long currentTime = System.currentTimeMillis();

                        //loop through all users
                        for (DataSnapshot userSnap : snapshot.getChildren()) {
                            DataSnapshot stockSnap = userSnap.child("stock_data");
                            Stock latestStock = null;
                            long latestTimestamp = Long.MIN_VALUE;

                            //Get the most recent stock entry for each user
                            for (DataSnapshot entrySnap : stockSnap.getChildren()) {
                                Stock stock = entrySnap.getValue(Stock.class);
                                if (stock != null && stock.storeName != null && stock.timestamp > latestTimestamp) {
                                    latestTimestamp = stock.timestamp;
                                    latestStock = stock;
                                }
                            }

//                            if (latestStock != null) {
//                                latestStockPerUser.add(latestStock);
//                            }
                            // Check if the latest stock entry is within the last 3 days
                            if (latestStock != null && (currentTime - latestStock.timestamp <= threeDaysInMillis)) {
                                latestStockPerUser.add(latestStock);
                            }
                        }

                        // Sort by most recent across users
                        Collections.sort(latestStockPerUser, (a, b) -> Long.compare(b.timestamp, a.timestamp));

                        LayoutInflater inflater = LayoutInflater.from(StockInputActivity.this);
                        for (Stock stock : latestStockPerUser) {
                            View stockView = inflater.inflate(R.layout.item_stock_entry, stockListContainer, false);

                            TextView dataView = stockView.findViewById(R.id.textViewDate);
                            TextView quantityView = stockView.findViewById(R.id.textViewQuantity);
                            TextView locationView = stockView.findViewById(R.id.textViewLocation);

                            dataView.setText("Date : " + stock.date);
                            quantityView.setText("Quantity : " + stock.quantity);
                            locationView.setText("Store : " + stock.storeName);

                            stockListContainer.addView(stockView);
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

    // Navigate to MainActivity when back is pressed
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

    //Stock model class
    public static class Stock{
        public String storeName;
        public int quantity;
        public long timestamp;
        public String date;

        public Stock() {
            // Needed for Firebase
        }

        public Stock (String storeName, int quantity, long timestamp, String date){
            this.storeName = storeName;
            this.quantity = quantity;
            this.timestamp = timestamp;
            this.date = date;
        }
    }
}