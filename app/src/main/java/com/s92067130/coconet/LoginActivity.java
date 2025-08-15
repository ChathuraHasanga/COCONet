package com.s92067130.coconet;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

// LoginActivity- Handles user authentication through firebase.
public class LoginActivity extends AppCompatActivity {

    //Declare variables for UI components.
    EditText editTextEmail, editTextPassword;
    Button buttonLogin;
    FirebaseAuth mAuth;
    ProgressBar progressBar;
    TextView textView;

    //check if a user already logged in when activity starts.
    @Override
    public void onStart() {
        super.onStart();
        try {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if(currentUser != null){
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish(); //prevent going back to the login screen.
            }
        }catch (Exception e){
            Toast.makeText(this, "Startup error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    //called when the activity is first created.
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //hide title bar and action bar for clean login screen.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null){
            getSupportActionBar().hide();
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        try {
            //Initialize firebase authentication and UI components.
            mAuth = FirebaseAuth.getInstance();
            editTextEmail = findViewById(R.id.email);
            editTextPassword = findViewById(R.id.password);
            buttonLogin = findViewById(R.id.loginbtn);
            progressBar = findViewById(R.id.progressbar);
            textView = findViewById(R.id.registerTxt);
            ImageView togglePassword = findViewById(R.id.togglePassword);

            //Set click listeners for register text.
            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
                        startActivity(intent);
                        finish();
                    }catch (Exception e){
                        Toast.makeText(LoginActivity.this, "Navigation Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

            //Login button logic with full validation.
            buttonLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    progressBar.setVisibility(view.VISIBLE);

                    try {
                        String email, password;
                        email = String.valueOf(editTextEmail.getText()).trim();
                        password = String.valueOf(editTextPassword.getText()).trim();

                        //check email field is empty or not
                        if (TextUtils.isEmpty(email)){
                            Toast.makeText(LoginActivity.this, "Enter Email", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                            return;
                        }

                        //check email format correct or not
                        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                            Toast.makeText(LoginActivity.this, "Invalid email format", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                            return;
                        }

                        //check password field is empty or not
                        if (TextUtils.isEmpty(password)){
                            Toast.makeText(LoginActivity.this, "Enter Password", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                            return;
                        }

                        //check password length is less than 6 characters or not
                        if (password.length() < 6){
                            Toast.makeText(LoginActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                            return;
                        }

                        //Firebase sign-in logic
                        mAuth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        progressBar.setVisibility(View.GONE);
                                        if (task.isSuccessful()) {
                                            FirebaseUser user = mAuth.getCurrentUser();
                                            if (user != null){
                                                DatabaseReference userRef = FirebaseDatabase.getInstance("https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app")
                                                        .getReference("users").child(user.getUid());

                                                userRef.child("role").get().addOnSuccessListener(roleSnapshot -> {
                                                    String role= roleSnapshot.getValue(String.class);
                                                    if (role != null){
                                                        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                                                        prefs.edit().putString("role",role).apply();
                                                    }
                                                    Toast.makeText(getApplicationContext(), "Login Successful", Toast.LENGTH_SHORT).show();
                                                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                                    startActivity(intent);
                                                    finish();
                                                }).addOnFailureListener(e -> {
                                                    Toast.makeText(LoginActivity.this, "Failed to load user role: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                });
                                            }

                                        } else {
                                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    }catch (Exception e){
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(LoginActivity.this, "Login Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

            //system bar inset handling.
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }catch (Exception e){
            Toast.makeText(this, "Initialization error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    //navigate to ScanActivity user interface when scan qr button is clicked
    public void onScanBtnClick(View view) {
        try {
            Intent intent = new Intent(LoginActivity.this, ScanActivity.class);
            startActivity(intent);
        }catch (Exception e){
            Toast.makeText(this, "Navigation error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    //navigate to RegisterActivity when register button is clicked
    public void onRegisterClick(View view) {
        try {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        }catch (Exception e){
            Toast.makeText(this, "Navigation error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}