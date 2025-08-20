package com.s92067130.coconet;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * FirstPage is the initial landing screen of the app.
 * It displays a splash page for a short duration before navigating to the LoginActivity.
 */
public class FirstPage extends AppCompatActivity {

    /**
     * Called when the activity is first created.
     * Initializes the UI, sets fullscreen, applies edge-to-edge layout,
     * and schedules a delayed transition to LoginActivity.
     *
     * @param savedInstanceState A Bundle object containing the activity's previously saved state.
     *                           Can be null if the activity is newly created.
     * @return void (does not return any value)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //request to remove the title bar for fullscreen appearance
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);

        //hide the action bar for a cleaner look
        if (getSupportActionBar() != null){
            getSupportActionBar().hide();
        }

        //Enable edge-to-edge content rendering
        EdgeToEdge.enable(this);

        //set the layout for first page.
        setContentView(R.layout.activity_first_page);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        /*
         * Schedule a delayed task using Handler to navigate to LoginActivity
         * after a short splash screen duration (2 seconds)
         */
        new Handler(Looper.getMainLooper()).postDelayed(()-> {
            // Create an intent to start LoginActivity
            Intent intent = new Intent(FirstPage.this, LoginActivity.class);
            startActivity(intent);

            finish();  //user can't go back to this page
        }, 2000); //2 seconds delay
    }
}