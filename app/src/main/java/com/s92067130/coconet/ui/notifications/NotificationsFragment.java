package com.s92067130.coconet.ui.notifications;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.s92067130.coconet.LoginActivity;
import com.s92067130.coconet.R;
import com.s92067130.coconet.databinding.FragmentNotificationsBinding;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

//Fragment to manage and update user profile including Location, contact, and reset options.
public class NotificationsFragment extends Fragment {

    private EditText nameInput, emailInput, phoneInput, locationInput;
    private Button updateBtn, logoutBtn, resetBtn, permissionBtn;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private DatabaseReference userRef;
    private FusedLocationProviderClient locationClient;
    private FragmentNotificationsBinding binding;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    //called to initialize the fragment view and setup firebase references and UI.
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        try {
            // Firebase
            auth = FirebaseAuth.getInstance();
            user = auth.getCurrentUser();

            if (user == null) {
                startActivity(new Intent(getActivity(), LoginActivity.class));
                requireActivity().finish();
                return root;
            }

            String uid = user.getUid();
            userRef = FirebaseDatabase.getInstance("https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .getReference("users").child(uid);

            // Location client
            locationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

            // View bindings
            nameInput = root.findViewById(R.id.editTextName);
            emailInput = root.findViewById(R.id.editTextEmail);
            phoneInput = root.findViewById(R.id.editTextNumber);
            locationInput = root.findViewById(R.id.locationText);

            updateBtn = root.findViewById(R.id.buttonUpdate);
            logoutBtn = root.findViewById(R.id.logoutBtn);
            resetBtn = root.findViewById(R.id.resetBtn);
            permissionBtn = root.findViewById(R.id.permissionBtn); // "Give Permission"

            // Handle permission result
            requestPermissionLauncher =
                    registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                        if (isGranted) {
                            getLocationAndUpdateAddress();
                        } else {
                            Toast.makeText(getContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
                        }
                    });

            // Button listeners
            updateBtn.setOnClickListener(v -> updateUserData());

            // Logout the user
            logoutBtn.setOnClickListener(v -> {
                auth.signOut();
                startActivity(new Intent(getActivity(), LoginActivity.class));
                requireActivity().finish();
            });

            resetBtn.setOnClickListener(v -> {
                userRef.child("stock_data").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot stockItem : snapshot.getChildren()) {
                            String storeName = stockItem.child("storeName").getValue(String.class);

                            // If storeName exists, remove only that field
                            if (storeName != null) {
                                stockItem.getRef().child("storeName").removeValue();
                            }
                        }

                        Toast.makeText(getContext(), "Reset successful!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Reset failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            });

            permissionBtn.setOnClickListener(v -> requestLocationPermission());

            autoRemoveExpiredStoreNames();
            // Load data
            loadUserData();
        }catch (Exception e){
            Toast.makeText(getContext(), "Initialization error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return root;
    }

    //Request location permission if not already granted.
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

    //Retrieves the current location and updates the address in Firebase
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
                            Toast.makeText(getContext(), "Location is null", Toast.LENGTH_SHORT).show();
                        }
                    });
        }catch (Exception e){
            Toast.makeText(getContext(), "Error getting location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    //converts location coordinates to a human-readable address using Geocoder.
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
     *Loads user profile data from firebase
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

    // Updates the user's profile in Firebase
    private void updateUserData() {
        try {
            String name = nameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String phone = phoneInput.getText().toString().trim();
            String location = locationInput.getText().toString().trim();

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(phone) || TextUtils.isEmpty(location)) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("name", name);
            updates.put("email", email);
            updates.put("contactNumber", phone);
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

    //Removes expired "storeName" fields after 3 days from stock data.
    private void autoRemoveExpiredStoreNames(){
        try {
            long threeDaysMillis = 3L * 24 * 60 * 60 * 1000;
            long now = System.currentTimeMillis();

            userRef.child("stock_data").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot stockItem : snapshot.getChildren()){
                        Long timestamp = stockItem.child("timestamp").getValue(Long.class);
                        if (timestamp !=null && (now - timestamp) > threeDaysMillis){
                            stockItem.getRef().child("storeName").removeValue();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }catch (Exception e){
            Toast.makeText(getContext(), "Error cleaning expired store names: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    //prevent memory leaks.
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
