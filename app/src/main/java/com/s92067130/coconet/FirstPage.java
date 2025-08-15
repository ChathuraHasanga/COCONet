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

//This is initial landing page of the app.
public class FirstPage extends AppCompatActivity {

    //oncreate- lifecycle method called when the activity is first created.
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

        //3 seconds late to load the login page
        new Handler(Looper.getMainLooper()).postDelayed(()-> {
            Intent intent = new Intent(FirstPage.this, LoginActivity.class);
            startActivity(intent);

            finish();  //user can't go back to this page
        }, 2000); //2 seconds delay
    }
}