package com.s92067130.coconet;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WalletActivity extends AppCompatActivity {

    private TextView tvBalance;
    private RecyclerView rvTransactions;
    private TransactionAdapter adapter;
    private List<Transaction> transactionList;

    private DatabaseReference walletRef;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //hide the action bar if not null
        if (getSupportActionBar() != null){
            getSupportActionBar().hide();
        }
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_wallet);

        tvBalance = findViewById(R.id.tvBalanceAmount);
        rvTransactions = findViewById(R.id.rvTransactions);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        walletRef = FirebaseDatabase.getInstance(
                        "https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference();

        transactionList = new ArrayList<>();
        adapter = new TransactionAdapter(transactionList);

        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(adapter);

        loadWalletBalance();
        loadTransactions();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadWalletBalance() {
        walletRef.child("wallets").child(currentUserId).child("balance")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Double balance = snapshot.getValue(Double.class);
                        tvBalance.setText("Rs. " + (balance != null ? balance : 0.0));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(WalletActivity.this, "Failed to load balance", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadTransactions() {
        walletRef.child("transactions").child(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        transactionList.clear();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            Transaction t = snap.getValue(Transaction.class);
                            if (t != null) transactionList.add(t);
                        }
                        // show latest first
                        Collections.reverse(transactionList);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(WalletActivity.this, "Failed to load transactions", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void OnClickBtnBackWallet(View view) {
        try {
            Intent intent = new Intent(WalletActivity.this, MainActivity.class);
            startActivity(intent);
        }catch (Exception e){
            Toast.makeText(this, "Navigation Error : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}