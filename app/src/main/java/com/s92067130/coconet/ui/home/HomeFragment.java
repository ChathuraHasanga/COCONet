package com.s92067130.coconet.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.s92067130.coconet.AdminDashActivity;
import com.s92067130.coconet.R;
import com.s92067130.coconet.databinding.FragmentHomeBinding;

import java.util.Calendar;

// Home fragment displays the welcome message and dashboard summery data.
public class HomeFragment extends Fragment {

    // Firebase authentication & DB reference
    FirebaseAuth mAuth;
    DatabaseReference mDatabase;

    // View Binding & UI Elements
    private FragmentHomeBinding binding;
    private TextView welcomeText, stockLevels, pendingStock, newSuppliers, nearbyStock;
    private Button adminDashboardBtn;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ScrollView scrollView;

    // Current logged-in user details
    public String currentUserId;
    private String currentUserDistrict;



    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        try {
            // Initialize view model
            HomeViewModel homeViewModel =
                    new ViewModelProvider(this).get(HomeViewModel.class);

            binding = FragmentHomeBinding.inflate(inflater, container, false);
            View root = binding.getRoot();

            //initialize views
            welcomeText = root.findViewById(R.id.welcomeTxt);
            stockLevels = root.findViewById(R.id.card1).findViewById(R.id.textCurrentStock);
            pendingStock = root.findViewById(R.id.card2).findViewById(R.id.textPendingStock);
            newSuppliers = root.findViewById(R.id.card3).findViewById(R.id.textNewSuppliers);
            nearbyStock = root.findViewById(R.id.card4).findViewById(R.id.textNearbyStock);
            swipeRefreshLayout = root.findViewById(R.id.swipeRefreshLayout);
            scrollView = root.findViewById(R.id.scrollHomePage);

            //Find the admin dashboard button
            adminDashboardBtn = root.findViewById(R.id.adminDashboardBtn);
            adminDashboardBtn.setVisibility(View.GONE);

            //firebase authentication
            mAuth = FirebaseAuth.getInstance();
            FirebaseUser currentUser= mAuth.getCurrentUser();

            //If a user is logged in, load their data and dashboard info
            if (currentUser != null) {
                currentUserId = currentUser.getUid();

                // Reference to logged-in user's node in "users"
                mDatabase = FirebaseDatabase.getInstance("https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("users").child(currentUserId);

                // Check if user is admin -> show Admin Dashboard button
                mDatabase.child("role").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String role = snapshot.getValue(String.class);
                        if (role != null && role.trim().equalsIgnoreCase("admin")){
                            adminDashboardBtn.setVisibility(View.VISIBLE);
                        }else {
                            adminDashboardBtn.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Failed to load role: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

                // Navigate to Admin Dashboard if button clicked
                adminDashboardBtn.setOnClickListener(View -> {
                    Intent intent = new Intent(getActivity(), AdminDashActivity.class);
                    startActivity(intent);
                });

                //fetch current user's name from database for welcome message
                mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        try {
                            if (snapshot.exists()){
                                String name = snapshot.child("name").getValue(String.class);
                                welcomeText.setText("Welcome, " + name + "!");
                            }else {
                                welcomeText.setText("Welcome, User!");
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                            Toast.makeText(getContext(), "Error loading user info", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

                // Fetch user district (needed for filtering nearby stock)
                mDatabase.child("district").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        currentUserDistrict = snapshot.getValue(String.class);
                        if (currentUserDistrict != null){
                            // Load dashboard only after district is known
                            loadDashboard(currentUserId,currentUserDistrict);
                        }else {
                            Toast.makeText(getContext(), "District not set for user", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Failed to load district", Toast.LENGTH_SHORT).show();
                    }
                });

            }

            //pull to refresh to reload all data
            swipeRefreshLayout.setOnRefreshListener(() ->{
                if (currentUserId != null && currentUserDistrict != null){
                    loadDashboard(currentUserId, currentUserDistrict);
                }else {
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(getContext(), "Data not ready. Please try again later.", Toast.LENGTH_SHORT).show();
                }
            });

            //Detect scroll to bottom and refresh
            scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
                View view = scrollView.getChildAt(scrollView.getChildCount() - 1);
                int diff = (view.getBottom() - (scrollView.getHeight() + scrollView.getScrollY()));

                if (diff == 0 && currentUserId != null && currentUserDistrict != null){
                    Toast.makeText(getContext(), "Refreshing data...", Toast.LENGTH_SHORT).show();

                    //Refresh dashboard data
                    loadDashboard(currentUserId, currentUserDistrict);
                }
            });

            final TextView textView = binding.textHome;
            homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
            return root;
        }catch (Exception e){
            Toast.makeText(getContext(), "Initialization error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    //Auto refresh when user returns to this fragment


    @Override
    public void onResume() {
        super.onResume();
        // Auto-refresh when returning to fragment
        if (currentUserId != null && currentUserDistrict != null){
            loadDashboard(currentUserId, currentUserDistrict);
        }
    }

    /**
     * Fetch dashboard summery data for the user and others from firebase
     * includes total stock today, pending entries, new suppliers, and nearby stock count.
     */
    private void loadDashboard(String uid, String district) {
        swipeRefreshLayout.setRefreshing(true);
        try {
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
                    try {
                        //loop through all users to analyze their stock data
                        for (DataSnapshot userSnap : snapshot.getChildren()) {
                            String userDistrict = userSnap.child("district").getValue(String.class);

                            boolean hasValidStoreToday = false;
                            boolean hasAnyValidStoreBefore = false;
                            boolean hasNullStoreName = false;
                            long firstStockTimestamp = Long.MAX_VALUE;

                            // loop through each user's stock entries
                            for (DataSnapshot stockSnap : userSnap.child("stock_data").getChildren()) {
                                Long ts = stockSnap.child("timestamp").getValue(Long.class);
                                Integer qty = stockSnap.child("quantity").getValue(Integer.class);
                                String storeName = stockSnap.child("storeName").getValue(String.class);

                                if (ts != null && qty != null) {
                                    if (storeName != null && !storeName.isEmpty()) {

                                        // Track first stock entry time.
                                        if (ts < firstStockTimestamp) {
                                            firstStockTimestamp = ts;
                                        }

                                        //check today's stock entries
                                        if (ts >= todayStartMillis) {
                                            totalStockToday += qty;
                                            hasValidStoreToday = true;

                                            // Count as "nearby stock" if another user in same district
                                            if (!userSnap.getKey().equals(uid) && storeName != null && userDistrict !=null && userDistrict.equalsIgnoreCase(district)){
                                                nearbyStockQty++;
                                                break;   // avoid double-counting same user
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

                            //  Count as new suppliers only if their stock is from today
                            if (hasValidStoreToday && firstStockTimestamp >= todayStartMillis) {
                                newUserToday++;
                            }

                            // Pending = user only has null storeName stock entries
                            if (hasNullStoreName && !hasValidStoreToday && !hasAnyValidStoreBefore) {
                                pendingUserCount++;
                            }
                        }

                        //UI updates
                        stockLevels.setText(String.valueOf(totalStockToday));
                        HomeFragment.this.pendingStock.setText(String.valueOf(pendingUserCount));
                        newSuppliers.setText(String.valueOf(newUserToday));
                        nearbyStock.setText(String.valueOf(nearbyStockQty)); // Exclude self from nearby stock count

                    }catch (Exception e) {
                        Toast.makeText(getContext(), "Error processing dashboard" + e.getMessage(),Toast.LENGTH_SHORT).show();
                    }finally {
                        swipeRefreshLayout.setRefreshing(false); //Hide refresh
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getContext(), "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                }
            });
        }catch (Exception e){
            Toast.makeText(getContext(), "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    //Gets the start of the current day in milliseconds.
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
        // Avoid memory leaks from ViewBinding
        binding = null;
    }
}