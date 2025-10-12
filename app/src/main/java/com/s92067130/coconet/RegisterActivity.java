package com.s92067130.coconet;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import android.location.Address;
import android.location.Geocoder;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RegisterActivity handles user registration, location fetching, and saving user data to Firebase.
 * It also sends a QR-code login email to the user after registration.
 */
public class RegisterActivity extends AppCompatActivity {
    private NetworkHelper networkHelper;
    private TextView offlineBanner;


    // Declare variables for views and Firebase
    EditText editTextEmail, editTextPassword, editTextName, editContactNumber, editTextConfirmPassword, editTextStoreName;
    Button buttonReg;
    FirebaseAuth mAuth;
    ProgressBar progressBar;
    TextView textView;
    DatabaseReference mDatabase;

    EditText locationTxt;
    Button permissionBtn;

    // Location and user info
    String district;
    String province;
    String role = "user"; //default role for new users
    String status = "active"; //default status
    String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    private Double latitude = null;
    private Double longitude = null;

    /**
     * Redirect to MainActivity if a user is already signed in.
     *
     * @return void
     */
    @Override
    public void onStart() {
        super.onStart();
        try {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if(currentUser != null){
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        }catch (Exception e){
            Toast.makeText(this, "Startup error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Main initialization method for the activity.
     * Initializes views, Firebase, edge-to-edge UI, and sets up click listeners.
     *
     * @param savedInstanceState Bundle containing saved state. Can be null.
     * @return void
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //hide tool bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        //hide the action bar
        if (getSupportActionBar() != null){
            getSupportActionBar().hide();
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        offlineBanner = findViewById(R.id.offlineBanner);

        networkHelper = new NetworkHelper(this);
        networkHelper.registerNetworkCallback(offlineBanner);

        try {
            //initialize firebase
            mAuth = FirebaseAuth.getInstance();
            mDatabase = FirebaseDatabase.getInstance("https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();

            // Initialize views
            editTextEmail = findViewById(R.id.email);
            editTextPassword = findViewById(R.id.password);
            buttonReg = findViewById(R.id.registerbtn);
            editTextName = findViewById(R.id.editName);
            editContactNumber = findViewById(R.id.editTextPhone);
            editTextConfirmPassword = findViewById(R.id.confirm_password);
            progressBar = findViewById(R.id.progressbar);
            textView = findViewById(R.id.loginTxt);
            locationTxt = findViewById(R.id.locationText);
            permissionBtn = findViewById(R.id.permissionbtn);
            editTextStoreName = findViewById(R.id.editTextStore);

            //Request location permission or fetch location if permission already granted
            permissionBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (ContextCompat.checkSelfPermission(RegisterActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                        ActivityCompat.requestPermissions(RegisterActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
                    }else {
                        fetchLocation();
                    }
                }
            });

            //Navigate to login page
            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            });

            //Create notification channel for Android 8+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
                android.app.NotificationChannel channel = new android.app.NotificationChannel(
                        "default",
                        "Default Channel",
                        android.app.NotificationManager.IMPORTANCE_HIGH
                );
                android.app.NotificationManager manager = getSystemService(android.app.NotificationManager.class);
                manager.createNotificationChannel(channel);
            }

            //Handle register button click
            buttonReg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    progressBar.setVisibility(view.VISIBLE);
                    String email, password, name, contactNumber, confirmPassword, storeName;
                    email = String.valueOf(editTextEmail.getText());
                    password = String.valueOf(editTextPassword.getText());
                    name = String.valueOf(editTextName.getText());
                    contactNumber = String.valueOf(editContactNumber.getText());
                    confirmPassword = String.valueOf(editTextConfirmPassword.getText());
                    storeName = String.valueOf(editTextStoreName.getText());
                    String locationTxt = String.valueOf(RegisterActivity.this.locationTxt.getText());
                    Long timestamp = System.currentTimeMillis();

                    //check name field empty or not
                    if (TextUtils.isEmpty(name)){
                        Toast.makeText(RegisterActivity.this, "Enter Name", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        return;
                    }

                    //check email field empty or not
                    if (TextUtils.isEmpty(email)){
                        Toast.makeText(RegisterActivity.this, "Enter Email", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        return;
                    }

                    //check email format correct or not
                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                        Toast.makeText(RegisterActivity.this, "Invalid email format", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        return;
                    }

                    //check contact number field empty or not
                    if (TextUtils.isEmpty(contactNumber)){
                        Toast.makeText(RegisterActivity.this, "Enter Contact Number", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        return;
                    }

                    //check if it's a valid phone number format
                    if (!Patterns.PHONE.matcher(contactNumber).matches() || contactNumber.length() != 10){
                        Toast.makeText(RegisterActivity.this, "Enter valid contact number", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        return;
                    }

                    //check location field is empty or not
                    if (TextUtils.isEmpty(locationTxt)){
                        Toast.makeText(RegisterActivity.this, "Enter Location", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        return;
                    }

                    //check password field empty or not
                    if (TextUtils.isEmpty(password)){
                        Toast.makeText(RegisterActivity.this, "Enter Password", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        return;
                    }

                    //check password length is less than 6 characters or not
                    if (password.length() < 6){
                        Toast.makeText(RegisterActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        return;
                    }

                    //check password strength
                    if (!password.matches(".*[A-Z].*") || !password.matches(".*\\d.*") || !password.matches(".*[!@#$%^&*()].*")){
                        Toast.makeText(RegisterActivity.this, "Password must include uppercase, number, and symbol", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        return;
                    }

                    //check confirm password field empty or not
                    if (TextUtils.isEmpty(confirmPassword)){
                        Toast.makeText(RegisterActivity.this, "Enter Confirm Password", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        return;
                    }

                    //check confirm password length is less than 6 characters or not
                    if (confirmPassword.length() < 6){
                        Toast.makeText(RegisterActivity.this, "Confirm Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        return;
                    }

                    //check password and confirm password match or not
                    if (!password.equals(confirmPassword)){
                        Toast.makeText(RegisterActivity.this, "Password and Confirm password should same", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        return;
                    }

                    if (district == null || province == null || latitude == null || longitude == null) {
                        Toast.makeText(RegisterActivity.this, "Please GPS ON before registering", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        return;
                    }

                    //Register user with email and password
                    mAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    progressBar.setVisibility(View.GONE);
                                    if (task.isSuccessful()) {
                                        //Account created Successfully
                                        FirebaseUser user = mAuth.getCurrentUser();

                                        //save user data to realtime database
                                        saveUserDetailsToDatabase(user.getUid(), name, email,contactNumber, locationTxt, latitude, longitude, district, province, date, role,status, storeName, timestamp);

                                        Toast.makeText(RegisterActivity.this, "Account Created.",
                                                Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(getApplicationContext(), RegisterSuccess.class);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        // If sign in fails, display a message to the user.
                                        Toast.makeText(RegisterActivity.this, "Authentication failed.",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            });

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }catch (Exception e){
            Toast.makeText(this, "Initialization error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        networkHelper.unregisterNetworkCallback();
    }

    /**
     * Saves user details to Firebase Realtime Database and sends QR code via email.
     *
     * @param userId User's UID from Firebase Authentication
     * @param name User's full name
     * @param email User's email
     * @param contactNumber User's phone number
     * @param locationTxt Address/location of the user
     * @param latitude GPS latitude
     * @param longitude GPS longitude
     * @param district User's district
     * @param province User's province
     * @param date Registration date
     * @param role User role (default "user")
     * @return void
     */
    private void saveUserDetailsToDatabase(String userId, String name, String email, String contactNumber, String locationTxt, Double latitude, Double longitude, String district, String province, String date, String role, String status, String storeName, Long timestamp){

        try {
            User user = new User(name, email,contactNumber, locationTxt, latitude, longitude, district, province, date, this.role, status, storeName, timestamp);

            //save data under the user's UID
            mDatabase.child("users").child(userId).setValue(user)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()){
                                Toast.makeText(RegisterActivity.this, "User Details saved.", Toast.LENGTH_SHORT).show();

                                // Add notification automatically to /notifications node
                                DatabaseReference notifRef = mDatabase.child("notifications").push();
                                NotificationModel notif = new NotificationModel(
                                        "New User Registered" ,
                                        name + " from " + province + " just signed up.",
                                        System.currentTimeMillis(),
                                        "user_signup",
                                        false
                                );
                                notifRef.setValue(notif);
                            }else{
                                Toast.makeText(RegisterActivity.this, "Failed to save User Details", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }catch (Exception e){
            Toast.makeText(RegisterActivity.this, "Error saving user details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Fetch user's current GPS location and convert it to an address.
     *
     * @return void
     */
    private void fetchLocation(){
        try {
            FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
                return;
            }

            com.google.android.gms.location.LocationRequest locationRequest = com.google.android.gms.location.LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(1000); //every 1 second
            locationRequest.setFastestInterval(500);
            locationRequest.setNumUpdates(1); //only once

            fusedLocationProviderClient.requestLocationUpdates(locationRequest, new com.google.android.gms.location.LocationCallback(){
                @Override
                public void onLocationResult(com.google.android.gms.location.LocationResult locationResult) {
                    if (locationResult == null) {
                        Toast.makeText(RegisterActivity.this, "Failed to get location.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    android.location.Location location = locationResult.getLastLocation();

                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();

                        // Convert GPS to human-readable address
                        Geocoder geocoder = new Geocoder(RegisterActivity.this, Locale.getDefault());
                        try {
                            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                            if (addresses != null && !addresses.isEmpty()) {
                                String address = addresses.get(0).getAddressLine(0);
                                district = addresses.get(0).getSubAdminArea();
                                province = addresses.get(0).getAdminArea();
                                locationTxt.setText(address); //set address in editText
                            } else {
                                locationTxt.setText("Lat: " + latitude + ", Lon: " + longitude);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            locationTxt.setText("Lat: " + latitude + ", Lon: " + longitude);
                        }

                    } else {
                        locationTxt.setText("Location not available");
                        Toast.makeText(RegisterActivity.this, "GPS is off. Please turn it on.", Toast.LENGTH_LONG).show();
                    }
                }
            }, getMainLooper());
        }catch (Exception e){
            Toast.makeText(this, "Error fetching location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    //User model class for saving details in the database
    public static class User{
        public String locationTxt;
        public String email;
        public String name;
        public String contactNumber;
        public Double latitude;
        public Double longitude;
        public String district;
        public String province;
        public String date;
        public String role;
        public String status;
        public String storeName;
        public Long timestamp;

        /**
         * User model class representing all the fields stored in Firebase Realtime Database.
         */
        public User(String name, String email, String contactNumber, String locationTxt, Double latitude, Double longitude, String district, String province, String date, String role, String status, String storeName, Long timestamp) {
            this.name = name;
            this.contactNumber = contactNumber;
            this.email = email;
            this.locationTxt = locationTxt;
            this.latitude = latitude;
            this.longitude = longitude;
            this.district = district;
            this.province = province;
            this.date = date;
            this.role = role;
            this.status = "active"; //default status
            this.storeName = storeName;
            this.timestamp = timestamp;
        }
    }

    public static class NotificationModel {

        public String title;
        public String body;
        public long timestamp;
        public String type;
        public boolean read;

        public NotificationModel(){
            // Default constructor required for calls to DataSnapshot.getValue(User.class)
        }

        public NotificationModel(String title, String body, long timestamp, String type, boolean read) {
            this.title = title;
            this.body = body;
            this.timestamp = timestamp;
            this.type = type;
            this.read = read;
        }
    }
}