package com.s92067130.coconet;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.s92067130.coconet.databinding.ActivityMainBinding;
import com.s92067130.coconet.ui.home.HomeFragment;

/**
 * MainActivity hosts the main navigation UI of the application.
 * It manages bottom navigation between Home, Map, and Settings fragments.
 * It also handles navigation to StockInputActivity.
 */
public class MainActivity extends AppCompatActivity {
    private ChildEventListener notifListener;
    private DatabaseReference notifRef;

    // Declare the binding variable for the layout
    private ActivityMainBinding binding;

    // Firebase Authentication instance
    FirebaseAuth mAuth;

    // Reference to Firebase Realtime Database for the current user
    DatabaseReference mDatabase;

    /**
     * Called when the activity is first created.
     * Initializes Firebase, sets up view binding, configures bottom navigation.
     *
     * @param savedInstanceState Bundle containing previously saved state. Can be null.
     * @return void
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //request to hide tool bar for fullscreen UI
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        //hide the action bar if not null
        if (getSupportActionBar() != null){
            getSupportActionBar().hide();
        }

        //inflate the view using view binding.
        binding = ActivityMainBinding.inflate(getLayoutInflater());

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        setContentView(binding.getRoot());

        // Initialize Firebase authentication and database reference
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance("https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("users").child(mAuth.getUid());

        // Setup BottomNavigationView to switch between fragments
        BottomNavigationView navView = findViewById(R.id.nav_view);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_map, R.id.navigation_settings)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        //retry button for connection errors
        Button retryButton = findViewById(R.id.retryBtn);
        retryButton.setOnClickListener(v-> {
            HomeFragment homeFragment = (HomeFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment_activity_main)
                    .getChildFragmentManager()
                    .getFragments().get(0);
            if (homeFragment != null) homeFragment.onResume(); //reload dashboard
        });

        notifRef = FirebaseDatabase.getInstance().getReference("notifications");

        notifListener =new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                NotificationModel notif = snapshot.getValue(NotificationModel.class);
                if (notif != null && !notif.read){
                    showLocalNotification(notif.title, notif.body);
                    snapshot.getRef().child("read").setValue(true);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        notifRef.addChildEventListener(notifListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notifListener != null) {
            notifRef.removeEventListener(notifListener);
        }
    }

    private void showLocalNotification(String title, String body){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default")
                .setSmallIcon(R.drawable.baseline_add_alert_24)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body)) // full text
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }


    /**
     * Handles click on "Add Stock" button.
     * Navigates the user to StockInputActivity to input new stock details.
     *
     * @param view The button view that was clicked.
     * @return void
     */
    public void onClickAddStock(View view) {
        try {
            Intent intent = new Intent(MainActivity.this, StockInputActivity.class);
            startActivity(intent);
        }catch (Exception e){
            Toast.makeText(this, "Navigation Error : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void onClickFavorites(View view) {
        try {
            Intent intent = new Intent(MainActivity.this, FavoritesActivity.class);
            startActivity(intent);
        }catch (Exception e){
            Toast.makeText(this, "Navigation Error : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void OnOrdersBtnClicked(View view) {
        try {
            Intent intent = new Intent(MainActivity.this, SellerOrdersActivity.class);
            startActivity(intent);
        }catch (Exception e){
            Toast.makeText(this, "Navigation Error : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void OnPurchasesBtnClicked(View view) {
        try {
            Intent intent = new Intent(MainActivity.this, BuyerOrdersActivity.class);
            startActivity(intent);
        }catch (Exception e){
            Toast.makeText(this, "Navigation Error : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void OnWalletBtnClicked(View view) {
        try {
            Intent intent = new Intent(MainActivity.this, WalletActivity.class);
            startActivity(intent);
        }catch (Exception e){
            Toast.makeText(this, "Navigation Error : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void btnUserManagement(View view) {
        try {

            Intent intent = new Intent(MainActivity.this, UserManagementActivity.class);
            startActivity(intent);
            finish();
        }catch (Exception e){
            Toast.makeText(this, "Error navigating: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

}