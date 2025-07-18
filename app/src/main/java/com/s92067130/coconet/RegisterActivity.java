package com.s92067130.coconet;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.location.Address;
import android.location.Geocoder;
import android.content.Intent;
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

import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class RegisterActivity extends AppCompatActivity {

    // Declare variables for views and Firebase
    EditText editTextEmail, editTextPassword, editTextName, editContactNumber, editTextConfirmPassword;
    Button buttonReg;
    FirebaseAuth mAuth;
    ProgressBar progressBar;
    TextView textView;
    DatabaseReference mDatabase;

    EditText locationTxt;
    Button permissionBtn;
    String district;
    String province;
    String role = "user"; //default role for new users
    String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

    private Double latitude = null;
    private Double longitude = null;

    //Redirect to MainActivity if already signed in
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

    //Main initialization method
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
            ImageView togglePassword = findViewById(R.id.togglePassword);
            ImageView toggleConfirmPassword = findViewById(R.id.toggleConfirmPassword);

            //Request location permission or fetch location if permission already granted
            permissionBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (ContextCompat.checkSelfPermission(RegisterActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                        ActivityCompat.requestPermissions(RegisterActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);;
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

            //Handle register button click
            buttonReg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    progressBar.setVisibility(view.VISIBLE);
                    String email, password, name, contactNumber, confirmPassword;
                    email = String.valueOf(editTextEmail.getText());
                    password = String.valueOf(editTextPassword.getText());
                    name = String.valueOf(editTextName.getText());
                    contactNumber = String.valueOf(editContactNumber.getText());
                    confirmPassword = String.valueOf(editTextConfirmPassword.getText());
                    String locationTxt = String.valueOf(RegisterActivity.this.locationTxt.getText());

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
                    if (!Patterns.PHONE.matcher(contactNumber).matches()){
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
                                        saveUserDetailsToDatabase(user.getUid(), name, email,contactNumber, locationTxt, password, latitude, longitude, district, province, date, role);

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

            //Toggle visibility for password fields.
            togglePassword.setOnClickListener(v->{
                if (editTextPassword.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)){
                    editTextPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    togglePassword.setImageResource(R.drawable.visibility_24dp);
                }else{
                    editTextPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    togglePassword.setImageResource(R.drawable.visibility_off_24dp);
                }
                editTextPassword.setSelection(editTextPassword.getText().length());
            });

            //Toggle visibility for confirm password fields.
            toggleConfirmPassword.setOnClickListener(v->{
                if (editTextConfirmPassword.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)){
                    editTextConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    toggleConfirmPassword.setImageResource(R.drawable.visibility_24dp);
                }else{
                    editTextConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    toggleConfirmPassword.setImageResource(R.drawable.visibility_off_24dp);
                }
                editTextConfirmPassword.setSelection(editTextConfirmPassword.getText().length());
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

    //Send login QR code via email to user
    private void sendEmailWithQRCode(String name, String email, String loginToken) {
        new Thread(() -> {
            try{
                URL url = new URL("https://api.emailjs.com/api/v1.0/email/send");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("origin", "http://localhost");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?data=" + loginToken + "&size=150x150";

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("service_id", "service_vko8muz");
                jsonParam.put("template_id","template_2ufhjpe");;
                jsonParam.put("user_id", "pEb0MjlwafPm6QUFl");

                JSONObject templateParams = new JSONObject();
                templateParams.put("user_name", name);
                templateParams.put("user_email", email);
                templateParams.put("qr_code_url", qrUrl);

                jsonParam.put("template_params", templateParams);

                OutputStream os = conn.getOutputStream();
                os.write(jsonParam.toString().getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK){
                    runOnUiThread(() -> Toast.makeText(this, "Email with QR code sent!", Toast.LENGTH_SHORT).show());
                }else{
                    runOnUiThread(() -> Toast.makeText(this, "Failed to send email."+ responseCode, Toast.LENGTH_SHORT).show());
                }

                conn.disconnect();
            }catch (Exception e){
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error sending email: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    //Method to save user details to Firebase realtime database
    private void saveUserDetailsToDatabase(String userId, String name, String email, String contactNumber, String locationTxt, String password, Double latitude, Double longitude, String district, String province, String date, String role){

        try {
            String loginToken = UUID.randomUUID().toString(); //generate a unique login token
            User user = new User(name, email,contactNumber, locationTxt, loginToken, password, latitude, longitude, district, province, date, this.role);

            //save data under the user's UID
            mDatabase.child("users").child(userId).setValue(user)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()){
                                Toast.makeText(RegisterActivity.this, "User Details saved.", Toast.LENGTH_SHORT).show();
                                sendEmailWithQRCode(name, email, loginToken);
                            }else{
                                Toast.makeText(RegisterActivity.this, "Failed to save User Details", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }catch (Exception e){
            Toast.makeText(RegisterActivity.this, "Error saving user details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    //fetch and display user location
    private void fetchLocation(){
        try {
            FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {

                    latitude = location.getLatitude();
                    longitude = location.getLongitude();

                    //convert location to address
                    Geocoder geocoder = new Geocoder(RegisterActivity.this, Locale.getDefault());
                    try {
                        List<Address> addresses = geocoder.getFromLocation(latitude,longitude,1);
                        if (addresses != null && !addresses.isEmpty()){
                            String address = addresses.get(0).getAddressLine(0);
                            district = addresses.get(0).getSubAdminArea();
                            province = addresses.get(0).getAdminArea();

                            locationTxt.setText(address); //set address in editText
                        }else{
                            locationTxt.setText("Unable to get address.");
                        }
                    }catch (IOException e){
                        e.printStackTrace();
                        locationTxt.setText("Geocoder error");
                    }

                }else {
                    locationTxt.setText("Location not available");
                }
            });
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
        public String loginToken;
        public String password;
        public Double latitude;
        public Double longitude;
        public String district;
        public String province;
        public String date;
        public String role;

        public User(String name, String email, String contactNumber, String locationTxt, String loginToken, String password, Double latitude, Double longitude, String district, String province, String date, String role){
            this.name = name;
            this.contactNumber = contactNumber;
            this.email = email;
            this.locationTxt = locationTxt;
            this.loginToken = loginToken;
            this.password = password;
            this.latitude = latitude;
            this.longitude = longitude;
            this.district = district;
            this.province = province;
            this.date = date;
            this.role = role;
        }
    }
}