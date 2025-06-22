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

        //hide tool bar
        //call requestWindowFeature
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);

        //hide the action bar
        getSupportActionBar().hide();

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_scan);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance("https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("users");

        barcodeView = findViewById(R.id.previewCamera);
        barcodeView.getStatusView().setVisibility(View.GONE);

        barcodeView.decodeContinuous(callback);

        // Request camera permission first
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        } else {
            startScanner();
        }
    }
    private void startScanner() {
        barcodeView.decodeContinuous(callback);
        barcodeView.resume();
    }

    private final BarcodeCallback callback= new BarcodeCallback() {

        public void barcodeResult(BarcodeResult result) {
            if (!scanned && result.getText() != null) {
                scanned = true;
                String scannedToken = result.getText();

                //seacrch Firebase for user with maching token
                mDatabase.get().addOnCompleteListener(task -> {
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
                                            if (authTask.isSuccessful()) {
                                                Toast.makeText(ScanActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(ScanActivity.this, MainActivity.class));
                                                finish();
                                            } else {
                                                Toast.makeText(ScanActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                                                finish();
                                            }
                                        });
                            } else {
                                Toast.makeText(ScanActivity.this, "Missing login credentials", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                            break;
                        }
                    }
                    if (!matched) {
                        Toast.makeText(ScanActivity.this, "Invalid QR code", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }
        }
        @Override
        public void possibleResultPoints(java.util.List<com.google.zxing.ResultPoint> resultPoints) {
            // Optional: highlight possible points
        }
};

    @Override
    protected void onResume() {
        super.onResume();
        barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }

    // Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanner();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    public void onClickBtnBack(View view) {
        // Navigate to LoginPage when the back arrow is clicked
        Intent intent = new Intent(ScanActivity.this, LoginActivity.class);
        startActivity(intent);
    }
}