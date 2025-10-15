package com.coconetgo.global.ui.home;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import com.coconetgo.global.AdminDashActivity;
import com.coconetgo.global.NetworkHelper;
import com.coconetgo.global.R;
import com.coconetgo.global.databinding.FragmentHomeBinding;

import java.util.Calendar;

// Home fragment displays the welcome message and dashboard summery data.
public class HomeFragment extends Fragment {
    private NetworkHelper networkHelper;
    private TextView offlineBanner;

    // View Binding & UI Elements
    private FragmentHomeBinding binding;

    //Admin  section
    private View adminSection;
    private TextView welcomeText, totalStockText, activeUsersText, newSuppliersText, newSignupsText;

    //Personal section
    private View personalSection;
    private TextView personalStockText, weekAmountText, favoriteStoreText, pendingOrdersAmountText;

    private Button adminDashboardBtn, btnManageUsers;
    private Button toggleDashboardBtn;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ScrollView scrollView;

    // Firebase authentication & DB reference
    FirebaseAuth mAuth;
    DatabaseReference mDatabase;

    // Current logged-in user details
    public String currentUserId;
    private String currentUserDistrict, favoriteStoreName;

    // Track which dashboard is shown
    private boolean showingAdminDashboard = false;

    /**
     * Called to have the fragment instantiate its user interface view.
     * Initializes Firebase authentication, fetches user details, and sets up UI components.
     *
     * @param inflater  LayoutInflater object to inflate views in the fragment.
     * @param container Parent view to attach the fragment UI.
     * @param savedInstanceState Bundle with saved state data.
     * @return The root View of the fragment UI, or null if initialization fails.
     */
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        try {
            // Initialize view model
            HomeViewModel homeViewModel =
                    new ViewModelProvider(this).get(HomeViewModel.class);

            binding = FragmentHomeBinding.inflate(inflater, container, false);
            View root = binding.getRoot();

            final TextView textView = binding.textHome;
            homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

            Context context = getContext();

            // Network helper
            offlineBanner = root.findViewById(R.id.offlineBanner);
            if (context != null) {
                networkHelper = new NetworkHelper(context);
                networkHelper.registerNetworkCallback(offlineBanner);
            }

            //initialize views
            welcomeText = root.findViewById(R.id.welcomeTxt);

            //Admin section
            adminSection = root.findViewById(R.id.adminSection);
            totalStockText = root.findViewById(R.id.card1).findViewById(R.id.textCurrentStock);
            activeUsersText = root.findViewById(R.id.card2).findViewById(R.id.textActiveUsers);
            newSuppliersText = root.findViewById(R.id.card3).findViewById(R.id.textNewSuppliers);
            newSignupsText = root.findViewById(R.id.card4).findViewById(R.id.textNewSignUps);

            //Personal section
            personalSection = root.findViewById(R.id.personalSection);
            personalStockText = root.findViewById(R.id.card5).findViewById(R.id.textPersonalStock);
            weekAmountText = root.findViewById(R.id.card6).findViewById(R.id.textWeekAmount);
            favoriteStoreText = root.findViewById(R.id.card7).findViewById(R.id.textFavoriteStore);
            pendingOrdersAmountText =root.findViewById(R.id.card8).findViewById(R.id.pendingOrdersAmount);

            swipeRefreshLayout = root.findViewById(R.id.swipeRefreshLayout);

            //Find the admin dashboard button
            adminDashboardBtn = root.findViewById(R.id.adminDashboardBtn);
            adminDashboardBtn.setVisibility(View.GONE);

            btnManageUsers = root.findViewById(R.id.btnManageUsers);
            btnManageUsers.setVisibility(View.GONE);

            // Toggle dashboard button
            toggleDashboardBtn = root.findViewById(R.id.toggleDashboardBtn);
            toggleDashboardBtn.setVisibility(View.GONE);

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
                        if ("admin".equalsIgnoreCase(role)){

                            toggleDashboardBtn.setVisibility(View.VISIBLE);
                            showingAdminDashboard = true;
                            adminSection.setVisibility(View.VISIBLE);
                            personalSection.setVisibility(View.GONE);
                            toggleDashboardBtn.setText("Switch to Personal Dashboard");
                            adminDashboardBtn.setVisibility(View.VISIBLE);
                            btnManageUsers.setVisibility(View.VISIBLE);
                            loadAdminDashboard();
                        }else {
                            adminSection.setVisibility(View.GONE);
                            personalSection.setVisibility(View.VISIBLE);
                            toggleDashboardBtn.setVisibility(View.GONE);
                            showingAdminDashboard = false;
                            adminDashboardBtn.setVisibility(View.GONE);
                            btnManageUsers.setVisibility(View.GONE);
                            loadPersonalDashboard();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (isAdded() && getContext() != null) {
                            Toast.makeText(getContext(), "Failed to load role: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                // Navigate to Admin Dashboard if button clicked
                adminDashboardBtn.setOnClickListener(View -> {
                    Intent intent = new Intent(getActivity(), AdminDashActivity.class);
                    startActivity(intent);
                });

                //fetch current user's name from database for welcome message
                mDatabase.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = snapshot.getValue(String.class);
                        welcomeText.setText("Welcome, " + (name != null ? name : "User") + "!");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }

            swipeRefreshLayout.setOnRefreshListener(() -> {
                if (adminSection.getVisibility() == View.VISIBLE){
                    loadAdminDashboard();
                }else {
                    loadPersonalDashboard();
                }
            });

            //Toggle button click
            toggleDashboardBtn.setOnClickListener(v-> {
                if (showingAdminDashboard){

                    //switch to personal
                    adminSection.setVisibility(View.GONE);
                    personalSection.setVisibility(View.VISIBLE);
                    toggleDashboardBtn.setText("Switch to Admin Dashboard");
                    loadPersonalDashboard();
                }else {

                    //switch to admin
                    personalSection.setVisibility(View.GONE);
                    adminSection.setVisibility(View.VISIBLE);
                    toggleDashboardBtn.setText("Switch to Personal Dashboard");
                    loadAdminDashboard();
                }
                showingAdminDashboard = !showingAdminDashboard;
            });

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
        if (adminSection.getVisibility() == View.VISIBLE){
            loadAdminDashboard();
        }else {
            loadPersonalDashboard();
        }
    }

    /**
     * Loads and processes the dashboard summary data for the logged-in user and others
     * from Firebase Realtime Database.
     *
     * This includes:
     * - Total stock added today.
     * - Pending stock entries (users with incomplete stock data).
     * - New suppliers who added stock for the first time today.
     * - Nearby stock entries (other users in the same district).
     *
     */

    //loading/error using activity overlays
    private void showLoading(boolean show){
        if (getActivity() == null) return;
        View loadingLoayout = getActivity().findViewById(R.id.loadingLayout);
        View errorLayout = getActivity().findViewById(R.id.errorLayout);
        View fragmentView = getView();
        if (loadingLoayout != null) loadingLoayout.setVisibility(show ? View.VISIBLE : View.GONE);
        if (errorLayout != null) errorLayout.setVisibility(View.GONE);
        if (fragmentView != null) fragmentView.setVisibility(show ? View.GONE : View.VISIBLE);
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
    }

    private void showError(String message) {
        if (getActivity() == null) return;
        View loadingLayout = getActivity().findViewById(R.id.loadingLayout);
        View errorLayout = getActivity().findViewById(R.id.errorLayout);
        TextView errorText = getActivity().findViewById(R.id.errorText);
        View fragmentView = getView();

        if (loadingLayout != null) loadingLayout.setVisibility(View.GONE);
        if (errorLayout != null) errorLayout.setVisibility(View.VISIBLE);
        if (errorText != null) errorText.setText(message);
        if (fragmentView != null) fragmentView.setVisibility(View.GONE);
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
    }

    private void loadAdminDashboard() {
        showLoading(true);
        swipeRefreshLayout.setRefreshing(true);
        try {
            DatabaseReference usersRef = FirebaseDatabase.getInstance("https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .getReference("users");

            long todayStartMillis = getStartOfDayMillis();

            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {

                int totalStock = 0;
                int activeUsers = 0;
                int newSuppliers = 0;
                int newSignUps = 0;

                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    try {
                        //loop through all users to analyze their stock data
                        for (DataSnapshot userSnap : snapshot.getChildren()) {

                            boolean userAddedStockToday = false;
                            boolean userHasPreviousStock = false;
                            long firstStockTime = Long.MAX_VALUE;

                            //count new signups
                            Long createdTime = userSnap.child("timestamp").getValue(Long.class);
                            if (createdTime != null && createdTime>=  todayStartMillis) newSignUps++;

                            // loop through each user's stock entries
                            for (DataSnapshot stockSnap : userSnap.child("stock_data").getChildren()) {
                                Long ts = stockSnap.child("timestamp").getValue(Long.class);
                                Integer qty = stockSnap.child("quantity").getValue(Integer.class);

                                if (ts != null && qty != null) {

                                        //check today's stock entries
                                        totalStock += qty;

                                        //only stock added today
                                        if (ts >= todayStartMillis) {
                                            userAddedStockToday = true;
                                        }
                                        // Track first stock entry time.
                                        if (ts < todayStartMillis) {
                                            userHasPreviousStock = true;
                                        }
                                    }
                                }

                                if (userAddedStockToday) activeUsers++;
                                if (userAddedStockToday && !userHasPreviousStock) newSuppliers++;
                            }

                        //UI updates
                        totalStockText.setText(String.valueOf(totalStock));
                        activeUsersText.setText(String.valueOf(activeUsers));
                        newSuppliersText.setText(String.valueOf(newSuppliers));
                        newSignupsText.setText(String.valueOf(newSignUps));

                        showLoading(false);

                    }catch (Exception e) {
                        Toast.makeText(getContext(), "Error processing dashboard" + e.getMessage(),Toast.LENGTH_SHORT).show();
                        showError("Error processing dashboard: " + e.getMessage());
                    }finally {
                        swipeRefreshLayout.setRefreshing(false); //Hide refresh
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    showError("Database error: " + error.getMessage());
                    swipeRefreshLayout.setRefreshing(false);
                }
            });

            new Handler().postDelayed(() -> {
                if (getActivity() != null && getActivity().findViewById(R.id.loadingLayout).getVisibility() == View.VISIBLE)
                    showError("Connection timeout. Please try again.");
            },15000); //15 seconds timeout
        }catch (Exception e){
            Toast.makeText(getContext(), "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            showError("Database error: " + e.getMessage());
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    //Personal Dashboard
    private void loadPersonalDashboard(){
        showLoading(true);
        swipeRefreshLayout.setRefreshing(true);
        try {
            mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    int totalPersonalStock =0;
                    int weekAmount =0;
                    int favoriteStoreCount = 0;

                    DataSnapshot favSnap = snapshot.child("favorites");
                    if (favSnap.exists()){
                        favoriteStoreCount = (int) favSnap.getChildrenCount();
                    }

                    currentUserDistrict = snapshot.child("district").getValue(String.class);

                    long weekStart = getStartOfWeekMillis();

                    for (DataSnapshot stockSnap : snapshot.child("stock_data").getChildren()){
                        Integer qty = stockSnap.child("quantity").getValue(Integer.class);
                        Long ts = stockSnap.child("timestamp").getValue(Long.class);

                        if (qty != null) {
                            totalPersonalStock += qty;

                            if (ts != null && ts >= weekStart) {
                                weekAmount += qty;
                            }
                        }
                    }

                    DatabaseReference orderRef = FirebaseDatabase.getInstance("https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app")
                            .getReference("buyerOrders").child(currentUserId);

                    int finalTotalPersonalStock = totalPersonalStock;
                    int finalWeekAmount = weekAmount;
                    int finalFavoriteStoreCount = favoriteStoreCount;

                    orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        int pendingOrdersAmountSum = 0;
                        int finalWeekAmountAdjusted = finalWeekAmount;

                        @Override
                        public void onDataChange(@NonNull DataSnapshot ordersSnap) {
                            for (DataSnapshot sellerSnap : ordersSnap.getChildren()){
                                for (DataSnapshot orderSnap : sellerSnap.getChildren()) {
                                    String sellerId = orderSnap.child("sellerId").getValue(String.class);
                                    String status = orderSnap.child("status").getValue(String.class);
                                    String type = orderSnap.child("type").getValue(String.class);
                                    Integer quantity = orderSnap.child("quantity").getValue(Integer.class);
                                    Long ts = orderSnap.child("timestamp").getValue(Long.class);
                                    String buyerId = orderSnap.child("buyerId").getValue(String.class);

                                    // Subtract sales from week amount
                                    if (sellerId != null && sellerId.equals(currentUserId)
                                            && type != null && type.equals("sell")
                                            && ts != null && ts >= getStartOfWeekMillis()
                                            && quantity != null) {
                                        finalWeekAmountAdjusted -= quantity;
                                    }

                                    if (buyerId != null && buyerId.equals(currentUserId)
                                            && type != null && type.equals("buy")
                                            && ts != null && ts >= getStartOfWeekMillis()
                                            && quantity != null) {
                                        finalWeekAmountAdjusted += quantity;   // add bought qty
                                    }


                                    Double price = orderSnap.child("price").getValue(Double.class);

                                    if (sellerId != null && status != null && "pending".equals(status)
                                            && price != null && quantity != null && sellerId.equals(currentUserId)) {
                                        pendingOrdersAmountSum++;
                                    }
                                }
                            }

                            personalStockText.setText(String.valueOf(finalTotalPersonalStock));
                            weekAmountText.setText(String.valueOf(Math.max(0, finalWeekAmountAdjusted)));
                            favoriteStoreText.setText(String.valueOf(finalFavoriteStoreCount));
                            pendingOrdersAmountText.setText(String.valueOf(pendingOrdersAmountSum));
                            showLoading(false);

                            swipeRefreshLayout.setRefreshing(false);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            showError("Error loading pending stocks: "+error.getMessage());
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    showError("Database error: " + error.getMessage());
                    swipeRefreshLayout.setRefreshing(false);
                }
            });
        }catch (Exception e){
            showError("Error: "+e.getMessage());
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    /**
     * Gets the timestamp for the start of the current day in milliseconds.
     *
     * @return long representing today's start time (00:00:00.000) in epoch millis.
     */
    private long getStartOfDayMillis(){
        Calendar calender = Calendar.getInstance();
        calender.set(Calendar.HOUR_OF_DAY,0);
        calender.set(Calendar.MINUTE, 0);
        calender.set(Calendar.SECOND,0);
        calender.set(Calendar.MILLISECOND,0);
        return calender.getTimeInMillis();
    }

    private long getStartOfWeekMillis(){
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        cal.set(Calendar.HOUR_OF_DAY,0);
        cal.set(Calendar.MINUTE,0);
        cal.set(Calendar.SECOND,0);
        cal.set(Calendar.MILLISECOND,0);
        return cal.getTimeInMillis();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Avoid memory leaks from ViewBinding
        binding = null;
    }
}

