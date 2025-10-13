package com.s92067130.coconet;

import android.content.Intent;
import android.os.Bundle;
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

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {
    private NetworkHelper networkHelper;
    private TextView offlineBanner;

    private EditText resetEmail;
    private Button resetBtn;
    private ImageView backToLogin;
    private ProgressBar resetProgress;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null){
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_forgot_password);

        offlineBanner = findViewById(R.id.offlineBanner);

        resetEmail = findViewById(R.id.resetEmail);
        resetBtn = findViewById(R.id.resetBtn);
        backToLogin = findViewById(R.id.backToLogin);
        resetProgress = findViewById(R.id.resetProgress);
        mAuth = FirebaseAuth.getInstance();

        networkHelper = new NetworkHelper(this);
        try {
            networkHelper.registerNetworkCallback(offlineBanner);
        }catch (Exception e){
            Toast.makeText(this, "Network monitor error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        //Reset password logic
        resetBtn.setOnClickListener(view -> {
            try {
                String email = resetEmail.getText().toString().trim();

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(ForgotPasswordActivity.this, "Enter your email!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(ForgotPasswordActivity.this, "Enter a valid email!", Toast.LENGTH_SHORT).show();
                    return;
                }

                resetProgress.setVisibility(View.VISIBLE);

                mAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            resetProgress.setVisibility(View.GONE);
                            if (task.isSuccessful()) {
                                Toast.makeText(ForgotPasswordActivity.this, "Reset link sent to your email", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(ForgotPasswordActivity.this, "Error: " + (task.getException() != null ? task.getException().getMessage() : "Unknown"), Toast.LENGTH_LONG).show();
                            }
                        });
            }catch (Exception e){
                resetProgress.setVisibility(View.GONE);
                Toast.makeText(ForgotPasswordActivity.this, "Unexpected error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        //Back to login
        backToLogin.setOnClickListener(view -> {
            try {
                Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }catch (Exception e){
                Toast.makeText(this, "Navigation error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            networkHelper.unregisterNetworkCallback();
        }catch (Exception ignored){

        }
    }
}
