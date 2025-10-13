package com.coconetgo.global;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * RegisterSuccess Activity is shown to the user after successful registration.
 * It provides a confirmation message and allows the user to continue to MainActivity.
 */
public class RegisterSuccess extends AppCompatActivity {
    private NetworkHelper networkHelper;
    private TextView offlineBanner;

    /**
     * Called when the activity is first created.
     * Initializes the activity UI, hides the action bar, and enables edge-to-edge layout.
     *
     * @param savedInstanceState Bundle containing the activity's previously saved state.
     *                           Can be null if the activity is starting fresh.
     * @return void
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //request to remove the title bar for fullscreen appearance
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);

        //hide the action bar
        getSupportActionBar().hide();

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register_success);

        offlineBanner = findViewById(R.id.offlineBanner);

        networkHelper = new NetworkHelper(this);
        networkHelper.registerNetworkCallback(offlineBanner);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        networkHelper.unregisterNetworkCallback();
    }

    /**
     * Handles the click event of the "Continue" button.
     * Navigates the user to the MainActivity and closes the current RegisterSuccess activity.
     *
     * @param view The View object representing the button that was clicked.
     *             Typically the "Continue" button in the UI.
     * @return void
     */
    public void OnClickBtnContinue(View view) {
        try {
            // Create an intent to navigate to MainActivity
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);

            // Start MainActivity
            startActivity(intent);
            finish(); // Close the RegisterSuccess activity
        }catch (Exception e){
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}