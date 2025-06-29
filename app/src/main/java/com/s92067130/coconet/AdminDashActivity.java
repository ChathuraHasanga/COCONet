package com.s92067130.coconet;

import android.os.Bundle;
import android.view.Window;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

//Activity class for Admin Dashboard screen
public class AdminDashActivity extends AppCompatActivity {

    //called when the activity is starting.
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //hide tool bar
        //call requestWindowFeature
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);

        //hide the action bar
        if(getSupportActionBar() != null){
            getSupportActionBar().hide();
        }

        //enable edge-to-edge layout.
        EdgeToEdge.enable(this);

        //set layout from resource file.
        setContentView(R.layout.activity_admin_dash);

        //Apply system bar insets to the root view with padding.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}