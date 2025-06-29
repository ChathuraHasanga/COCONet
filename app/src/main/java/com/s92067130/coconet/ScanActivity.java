package com.s92067130.coconet;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

public class ScanActivity extends AppCompatActivity {

    DatabaseReference mDatabase;
    FirebaseAuth mAuth;
    private DecoratedBarcodeView barcodeView;
    private boolean scanned = false;
    private static final int CAMERA_PERMISSION_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        try {
            //hide the toolbar and title bar for a fullscreen camera view
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            super.onCreate(savedInstanceState);

            //hide the action bar
            if (getSupportActionBar() != null){
                getSupportActionBar().hide();
            }

            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_scan);
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

            //firebase initialization
            mAuth = FirebaseAuth.getInstance();
            mDatabase = FirebaseDatabase.getInstance("https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("users");

            //setup barcode scanner
            barcodeView = findViewById(R.id.previewCamera);
            if (barcodeView != null){
                barcodeView.getStatusView().setVisibility(View.GONE);
                barcodeView.decodeContinuous(callback);
            }

            // Request camera permission first
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
            } else {
                startScanner();
            }
        }catch (Exception e){
            Toast.makeText(this, "Initialization error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * start camera and barcode scanner
     */
    private void startScanner() {
        try {
            if (barcodeView != null){
                barcodeView.decodeContinuous(callback);
                barcodeView.resume();
            }
        }catch (Exception e){
            Toast.makeText(this, "Error starting scanner: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * callback triggered when a barcode is detected.
     */
    private final BarcodeCallback callback= new BarcodeCallback() {
        public void barcodeResult(BarcodeResult result) {
            try {
                if (!scanned && result.getText() != null) {
                    scanned = true;
                    String scannedToken = result.getText();

                    //search Firebase for user with matching token
                    mDatabase.get().addOnCompleteListener(task -> {
                        try {
                            boolean matched = false;
                            for (DataSnapshot snapshot : task.getResult().getChildren()) {
                                String token = snapshot.child("loginToken").getValue(String.class);
                                if (token != null && token.equals(scannedToken)) {
                                    matched = true;
                                    String email = snapshot.child("email").getValue(String.class);
                                    String password = snapshot.child("password").getValue(String.class);

                                    //sign in user with email and password
                                    if (email != null && password != null) {
                                        mAuth.signInWithEmailAndPassword(email, password)
                                                .addOnCompleteListener(authTask -> {
                                                    try {
                                                        if (authTask.isSuccessful()) {
                                                            Toast.makeText(ScanActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                                                            startActivity(new Intent(ScanActivity.this, MainActivity.class));
                                                            finish();
                                                        } else {
                                                            Toast.makeText(ScanActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                                                            finish();
                                                        }
                                                    }catch (Exception e){
                                                        Toast.makeText(ScanActivity.this, "Login exception: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    } else {
                                        Toast.makeText(ScanActivity.this, "Missing credentials", Toast.LENGTH_SHORT).show();
                                        finish();
                                    }
                                    break;
                                }
                            }
                            if (!matched) {
                                Toast.makeText(ScanActivity.this, "Invalid QR code", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        }catch (Exception e){
                            Toast.makeText(ScanActivity.this, "Error reading data: ", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                }
            }catch (Exception e){
                Toast.makeText(ScanActivity.this, "Scanning error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
        @Override
        public void possibleResultPoints(java.util.List<com.google.zxing.ResultPoint> resultPoints) {

        }
};

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (barcodeView != null){
                barcodeView.resume();
            }
        }catch (Exception e){
            Toast.makeText(this, "Resume error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (barcodeView != null){
                barcodeView.pause();
            }
        }catch (Exception e){
            Toast.makeText(this, "Pause error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Handle camera permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        try {
            if (requestCode == CAMERA_PERMISSION_REQUEST) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startScanner();
                } else {
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }catch (Exception e){
            Toast.makeText(this, "Permission error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    //Back button click handler
    public void onClickBtnBack(View view) {
        try {
            // Navigate to LoginPage when the back arrow is clicked
            Intent intent = new Intent(ScanActivity.this, LoginActivity.class);
            startActivity(intent);
        }catch (Exception e){
            Toast.makeText(this, "Navigation error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}