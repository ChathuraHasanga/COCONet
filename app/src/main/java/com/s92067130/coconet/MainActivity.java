package com.s92067130.coconet;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.s92067130.coconet.databinding.ActivityMainBinding;

/**
 * MainActivity hosts the main navigation UI of the application.
 * It manages bottom navigation between Home, Map, and Settings fragments.
 * It also handles navigation to StockInputActivity.
 */
public class MainActivity extends AppCompatActivity {

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

    /**
     * Handles click on "Map" button in custom UI.
     * Switches the BottomNavigationView to the Map tab.
     *
     * @param view The button view that was clicked.
     * @return void
     */
    public void OnMapBtnClicked(View view) {
        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setSelectedItemId(R.id.navigation_map); // Switch to Map tab
    }

}