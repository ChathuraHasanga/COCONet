package com.s92067130.coconet.ui.settings;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.util.Calendar;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.s92067130.coconet.LoginActivity;
import com.s92067130.coconet.NetworkHelper;
import com.s92067130.coconet.R;
import com.s92067130.coconet.UserLogger;
import com.s92067130.coconet.databinding.FragmentSettingsBinding;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;


/**
 * Fragment to manage and update user profile including:
 * - Name, email, contact, location
 * - Reset daily stock storeName
 * - Location permission and update
 * - Logout functionality
 */
public class SettingsFragment extends Fragment {
    private NetworkHelper networkHelper;
    private TextView offlineBanner;

    private EditText nameInput, emailInput, phoneInput, locationInput, storeNameText;
    private Button updateBtn, logoutBtn, resetBtn, permissionBtn;

    // Firebase references
    private FirebaseAuth auth;
    private FirebaseUser user;
    private DatabaseReference userRef;

    // Location services
    private FusedLocationProviderClient locationClient;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    private ImageView imageViewProfile;
    private Button buttonChoose, buttonUpload;
    private Uri selectedImageUri;


    // UI Elements
    private FragmentSettingsBinding binding;

    /**
     * Called to initialize the fragment view and setup Firebase, UI elements, and listeners.
     *
     * @param inflater  LayoutInflater to inflate the fragment layout
     * @param container Parent view that the fragment's UI should attach to
     * @param savedInstanceState Previously saved state of the fragment
     * @return The root view of the fragment
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();


        Context context = getContext();

        // Network helper
        offlineBanner = root.findViewById(R.id.offlineBanner);
        if (context != null) {
            networkHelper = new NetworkHelper(context);
            networkHelper.registerNetworkCallback(offlineBanner);
        }

        try {
            // Initialize Firebase authentication and user reference
            auth = FirebaseAuth.getInstance();
            user = auth.getCurrentUser();

            if (user == null) {
                // If no user is logged in, redirect to LoginActivity
                startActivity(new Intent(getActivity(), LoginActivity.class));
                requireActivity().finish();
                return root;
            }

            String uid = user.getUid();
            userRef = FirebaseDatabase.getInstance("https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .getReference("users").child(uid);

            // Setup location services client
            locationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

            // View bindings
            nameInput = root.findViewById(R.id.editTextName);
            emailInput = root.findViewById(R.id.editTextEmail);
            phoneInput = root.findViewById(R.id.editTextNumber);
            locationInput = root.findViewById(R.id.locationText);
            storeNameText =root.findViewById(R.id.editTextStore);

            buttonChoose = root.findViewById(R.id.buttonChoose);
            buttonUpload = root.findViewById(R.id.buttonUpload);
            updateBtn = root.findViewById(R.id.buttonUpdate);
            logoutBtn = root.findViewById(R.id.logoutBtn);
            resetBtn = root.findViewById(R.id.resetBtn);
            permissionBtn = root.findViewById(R.id.permissionBtn); // "Give Permission"

            imageViewProfile = root.findViewById(R.id.imageViewProfile);

            buttonChoose.setOnClickListener(v-> {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, 101);
            });

            buttonUpload.setOnClickListener(v -> {
                if (selectedImageUri != null) {
                        uploadImageToCloudinary(selectedImageUri);
                }else{
                    Toast.makeText(getContext(), "Please select an image first", Toast.LENGTH_SHORT).show();
                }
            });

            // Handle location permission request result
            requestPermissionLauncher =
                    registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                        if (isGranted) {
                            getLocationAndUpdateAddress();
                        } else {
                            Toast.makeText(getContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
                        }
                    });

            // Update button click → save profile updates
            updateBtn.setOnClickListener(v -> updateUserData());

            // Logout button click → sign out user and redirect to LoginActivity
            logoutBtn.setOnClickListener(v -> {

                // Log logout
                UserLogger.logUserAction("logout");
                auth.signOut();

                startActivity(new Intent(getActivity(), LoginActivity.class));
                requireActivity().finish();
            });

            // Permission button click → request location access
            permissionBtn.setOnClickListener(v -> requestLocationPermission());

            // Load user data from Firebase
            loadUserData();
        }catch (Exception e){
            Toast.makeText(getContext(), "Initialization error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return root;
    }

    /**
     * Request fine location permission if not already granted.
     */
    private void requestLocationPermission() {
        try {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                getLocationAndUpdateAddress();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }catch (Exception e){
            Toast.makeText(getContext(), "Permission error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Retrieves the current location and updates Firebase with the resolved address.
     */
    private void getLocationAndUpdateAddress() {
        try {
            locationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            String address = getAddressFromLocation(location);
                            if (address != null) {
                                locationInput.setText(address);
                                userRef.child("locationTxt").setValue(address)
                                        .addOnSuccessListener(aVoid ->
                                                Toast.makeText(getContext(), "Location updated", Toast.LENGTH_SHORT).show())
                                        .addOnFailureListener(e ->
                                                Toast.makeText(getContext(), "Location update failed", Toast.LENGTH_SHORT).show());
                            } else {
                                Toast.makeText(getContext(), "Could not fetch address", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getContext(), "Please turn ON GPS to get location", Toast.LENGTH_SHORT).show();
                        }
                    });
        }catch (Exception e){
            Toast.makeText(getContext(), "Error getting location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Converts latitude/longitude into a human-readable address.
     *
     * @param location The location object containing latitude and longitude
     * @return Address string if resolved, otherwise null
     */
    private String getAddressFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (!addresses.isEmpty()) {
                String addr = addresses.get(0).getAddressLine(0);

                return addr;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Utility function to return today's start time in milliseconds.
     *
     * @return Milliseconds representing today's 00:00 time
     */
    private long getStartOfDayMillis() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * Load user profile data from Firebase and populate input fields.
     */
    private void loadUserData() {
        try {
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    nameInput.setText(snapshot.child("name").getValue(String.class));
                    emailInput.setText(snapshot.child("email").getValue(String.class));
                    phoneInput.setText(snapshot.child("contactNumber").getValue(String.class));
                    locationInput.setText(snapshot.child("locationTxt").getValue(String.class));
                    storeNameText.setText(snapshot.child("storeName").getValue(String.class));

                    String profileUrl = snapshot.child("profileImageUrl").getValue(String.class);
                    if (profileUrl != null && !profileUrl.isEmpty()){
                        Glide.with(SettingsFragment.this)
                                .load(profileUrl)
                                .into(imageViewProfile);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
                }
            });
        }catch (Exception e){
            Toast.makeText(getContext(), "Error loading user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Update user profile data in Firebase Realtime Database.
     * Fields updated: name, email, contactNumber, locationTxt, lastUpdated.
     */
    private void updateUserData() {
        try {
            String name = nameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String phone = phoneInput.getText().toString().trim();
            String location = locationInput.getText().toString().trim();
            String storeName = storeNameText.getText().toString().trim();

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(phone) || phone.length() !=10 || TextUtils.isEmpty(location)) {
                Toast.makeText(getContext(), "Please fill all fields with correct contact number", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("name", name);
            updates.put("email", email);
            updates.put("contactNumber", phone);
            updates.put("storeName", storeName);
            updates.put("locationTxt", location);
            updates.put("lastUpdated", System.currentTimeMillis());

            userRef.updateChildren(updates).addOnSuccessListener(aVoid ->
                            Toast.makeText(getContext(), "Profile updated", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }catch (Exception e){
            Toast.makeText(getContext(), "Error updating user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,@Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            imageViewProfile.setImageURI(selectedImageUri);
        }
    }

    public void uploadImageToCloudinary(Uri imageUri){

        if (imageUri== null){
            Toast.makeText(getContext(), "No image selected", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "Uploading image...", Toast.LENGTH_SHORT).show();
        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dsthe3jtx",
                "api_key", "982556741147415",
                "api_secret" , "LB-4F76_1NU8345AUyok-SaDxds"
        ));

        new Thread(()-> {
            try {

                InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
                Map uploadResult = cloudinary.uploader().upload(inputStream, ObjectUtils.emptyMap());
                String imageUrl = (String) uploadResult.get("secure_url");

                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        saveImageUrlToFirebase(imageUrl);
                        Toast.makeText(getContext(), "Profile image uploaded successfully", Toast.LENGTH_SHORT).show();

                        //update Imagevies
                        Glide.with(requireContext()).load(imageUri).into(imageViewProfile);
                    });
                }
            }catch (Exception e){
                e.printStackTrace();
                if (isAdded()){
                    requireActivity().runOnUiThread(()->{
                        Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
                    }
            }
        }).start();
    }

    private void saveImageUrlToFirebase(String url){
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase.getInstance("https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("users").child(userId).child("profileImageUrl").setValue(url)
                .addOnSuccessListener(aVoid -> {

                })
                .addOnFailureListener(e ->
                    Toast.makeText(getContext(), "Failed to save image URL: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    /**
     * Prevent memory leaks by nullifying binding reference when view is destroyed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
