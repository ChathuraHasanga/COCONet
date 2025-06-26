package com.s92067130.coconet.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.s92067130.coconet.MainActivity;
import com.s92067130.coconet.R;
import com.s92067130.coconet.StockInputActivity;
import com.s92067130.coconet.databinding.FragmentHomeBinding;
import com.s92067130.coconet.ui.dashboard.DashboardFragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    FirebaseAuth mAuth;
    TextView textView;
    DatabaseReference mDatabase;
    private FragmentHomeBinding binding;
    private TextView stockLevels, pendingStock, newSuppliers, nearbyStock;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        textView = root.findViewById(R.id.welcomeTxt);
        stockLevels = root.findViewById(R.id.card1).findViewById(R.id.textCurrentStock);
        pendingStock = root.findViewById(R.id.card2).findViewById(R.id.textPendingStock);
        newSuppliers = root.findViewById(R.id.card3).findViewById(R.id.textNewSuppliers);
        nearbyStock = root.findViewById(R.id.card4).findViewById(R.id.textNearbyStock);
        mAuth = FirebaseAuth.getInstance();

        FirebaseUser user= mAuth.getCurrentUser();

        if (user != null) {
            String uid = user.getUid();

            mDatabase = FirebaseDatabase.getInstance("https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("users").child(uid);

            mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()){
                        String name = snapshot.child("name").getValue(String.class);
                        textView.setText("Welcome, " + name + "!");
                    }else {
                        textView.setText("Welcome, User!");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

            loadDashboard(uid);
        }

        final TextView textView = binding.textHome;
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    private void loadDashboard(String uid) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance("https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("users");

        long todayStartMillis = getStartOfDayMillis();

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {

            int totalStockToday = 0;
            int pendingUserCount = 0;
            int newUserToday = 0;
            int nearbyStockQty = 0;

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String district = userSnap.child("district").getValue(String.class);

                    boolean hasValidStoreToday = false;
                    boolean hasAnyValidStoreBefore = false;
                    boolean hasNullStoreName = false;

                    long firstStockTimestamp = Long.MAX_VALUE;

                    for (DataSnapshot stockSnap : userSnap.child("stock_data").getChildren()) {
                        Long ts = stockSnap.child("timestamp").getValue(Long.class);
                        Integer qty = stockSnap.child("quantity").getValue(Integer.class);
                        String storeName = stockSnap.child("storeName").getValue(String.class);

                        if (ts != null && qty != null) {
                            if (storeName != null && !storeName.isEmpty()) {
                                if (ts < firstStockTimestamp) {
                                    firstStockTimestamp = ts;
                                }

                                if (ts >= todayStartMillis) {
                                    totalStockToday += qty;
                                    hasValidStoreToday = true;
                                    
                                    if (!userSnap.getKey().equals(uid) && storeName != null){
                                        nearbyStockQty++;
                                        break;
                                    }

                                }
                                else {
                                    hasAnyValidStoreBefore = true;
                                }
                            } else {
                                hasNullStoreName = true;
                            }
                        }
                    }

                    //  Count as NEW SUPPLIER only if their stock is from today
                    if (hasValidStoreToday && firstStockTimestamp >= todayStartMillis) {
                        newUserToday++;
                    }

                    // Pending = user only has null storeName stock entries
                    if (hasNullStoreName && !hasValidStoreToday && !hasAnyValidStoreBefore) {
                        pendingUserCount++;
                    }
                }

                stockLevels.setText(String.valueOf(totalStockToday));
                HomeFragment.this.pendingStock.setText(String.valueOf(pendingUserCount));
                newSuppliers.setText(String.valueOf(newUserToday));
                nearbyStock.setText(String.valueOf(nearbyStockQty)); // Exclude self from nearby stock count
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }


    private long getStartOfDayMillis(){
        Calendar calender = Calendar.getInstance();
        calender.set(Calendar.HOUR_OF_DAY,0);
        calender.set(Calendar.MINUTE, 0);
        calender.set(Calendar.SECOND,0);
        calender.set(Calendar.MILLISECOND,0);
        return calender.getTimeInMillis();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}