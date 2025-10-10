package com.s92067130.coconet.ui.map;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.Marker;
import com.s92067130.coconet.ContactActivity;
import com.s92067130.coconet.StockInputActivity.Stock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.s92067130.coconet.R;
import com.s92067130.coconet.databinding.FragmentMapBinding;

import java.util.List;
import java.util.Locale;

/**
 * MapFragment displays a Google Map with markers for users' stock data.
 * <p>
 * Features:
 * - Displays a map centered on Sri Lanka.
 * - Allows searching for a location by name.
 * - Fetches and displays user stock data from Firebase as map markers.
 * - Opens a ContactActivity when a marker’s info window is clicked.
 */
public class MapFragment extends Fragment implements OnMapReadyCallback {

    private FragmentMapBinding binding;
    private GoogleMap myMap;    // initialize GoogleMap instance

    /**
     * Called when the fragment creates its view.
     * Initializes UI bindings, sets up map fragment, and handles location search input.
     *
     * @param inflater           LayoutInflater object to inflate fragment views.
     * @param container          Parent container in which the fragment UI is placed.
     * @param savedInstanceState Previously saved state for restoring fragment state.
     * @return Root view of the fragment layout, or null if initialization fails.
     */
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        try {
            MapViewModel dashboardViewModel =
                    new ViewModelProvider(this).get(MapViewModel.class);

            binding = FragmentMapBinding.inflate(inflater, container, false);
            View root = binding.getRoot();

            //set dashboard text
            final TextView textView = binding.textDashboard;
            dashboardViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

            //set up the map fragment and register callback
            SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);

            //Access the EditText and ImageView for searching locations
            EditText locationInput = root.findViewById(R.id.editLocationInput);
            ImageView searchButton = root.findViewById(R.id.searchBtn);

            // set click listener to search for user-entered location
            searchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        String locationName = locationInput.getText().toString().trim();
                        if (!locationName.isEmpty()) {
                            findLocationByName(locationName);
                        } else {
                            Toast.makeText(getContext(), "Please enter a location", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Search error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
            return root;
        }catch (Exception e){
            Toast.makeText(getContext(), "Initializing error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    /**
     * Finds a location by name using Geocoder and places a marker on the map.
     * Also zooms into the found location.
     *
     * @param locationName Name of the location entered by user.
     */
    private void findLocationByName(String locationName) {
        try {
            // Convert location name into coordinates
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocationName(locationName,1);

            if (addresses != null && !addresses.isEmpty()){
                Address address = addresses.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

                // Add marker and zoom in to the location
                myMap.addMarker(new MarkerOptions().position(latLng).title(locationName));
                myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,15));

            }else {
                Toast.makeText(getContext(), "Location not found", Toast.LENGTH_SHORT).show();
            }
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(getContext(), "Error finding location: ", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Loads user locations and their latest stock data from Firebase.
     * Each user with valid latitude, longitude, and stock data will be shown as a marker.
     * Clicking on the info window will open ContactActivity for that user.
     */
    private void loadUserLocationsFromDatabase() {
        try {
            // Reference to Firebase users node
            DatabaseReference databaseRef = FirebaseDatabase.getInstance("https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .getReference("users");

            databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // Iterate through all users
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        String name = userSnapshot.child("name").getValue(String.class);
                        Double lat = userSnapshot.child("latitude").getValue(Double.class);
                        Double lng = userSnapshot.child("longitude").getValue(Double.class);

                        // Check if location is available
                        if (lat != null && lng != null && name != null) {
                            DataSnapshot stockDataSnap = userSnapshot.child("stock_data");

                            double totalQuantity = 0;
                            Stock latestStock = null;
                            String storeName = null;

                            // Loop through stock entries to find latest
                            for (DataSnapshot stockSnap : stockDataSnap.getChildren()) {
                                Stock stock = stockSnap.getValue(Stock.class);
                                if (stock != null) {
                                    totalQuantity += stock.quantity;
                                    if (storeName == null && stock.storeName !=null){
                                        storeName =stock.storeName;
                                    }

                                    if (latestStock == null || (stock.date != null && latestStock.date != null &&
                                            stock.date.compareTo(latestStock.date) > 0)) {
                                        latestStock = stock;
                                    }
                                }
                            }

                            // Only add marker if stock exists
                            if (totalQuantity >0) {
                                LatLng position = new LatLng(lat, lng);
                                String infoText = "Name: " + latestStock.storeName +
                                        "\nCurrent stock: " + totalQuantity+ " Kg" +
                                        "\nOn: " + latestStock.date;

                                Marker marker = myMap.addMarker(new MarkerOptions()
                                        .position(position)
                                        .title(name)
                                        .snippet(infoText));

                                if (marker != null){
                                    marker.setTag(userSnapshot.getKey()); // Set user ID as tag for contact activity
                                }
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getContext(), "Failed to load user stock locations", Toast.LENGTH_SHORT).show();
                }
            });
        }catch (Exception e){
            Toast.makeText(getContext(), "Error loading markers: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Called when Google Map is ready to use.
     * Configures UI settings, sets default location, loads user stock markers,
     * and handles marker info window click events.
     *
     * @param googleMap Instance of GoogleMap that is ready to be manipulated.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        try {
            myMap = googleMap;

            //center on Sri Lanka with default zoom
            LatLng sriLankaCenter = new LatLng(7.8731, 80.7718);
            myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sriLankaCenter, 7.5f));
            myMap.getUiSettings().setZoomControlsEnabled(true);

            // Custom info window layout for markers
            myMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                @Nullable
                @Override
                public View getInfoWindow(@NonNull com.google.android.gms.maps.model.Marker marker) {
                    return null;    // Use default frame
                }

                @Override
                public View getInfoContents(@NonNull com.google.android.gms.maps.model.Marker marker) {

                    if (marker.getTag() != null) {
                        // Inflate custom layout for info window
                        View view = LayoutInflater.from(getContext()).inflate(R.layout.custom_mapinfo, null);

                        //TextView title = view.findViewById(R.id.infoTitle);
                        TextView snippet = view.findViewById(R.id.infoSnippet);

                        //title.setText(marker.getTitle());
                        snippet.setText(marker.getSnippet());

                        return view;
                    }else {
                        return null;
                    }

                }
            });

            // Set a click listener for info windows
            myMap.setOnInfoWindowClickListener(marker -> {
                Object tag = marker.getTag();
                if (tag != null) {
                    try {
                        String userId = tag.toString();
                        Intent intent = new Intent(requireContext(), ContactActivity.class);
                        intent.putExtra("userId", userId);
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Error opening contact activity", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            //load markers from firebase
            loadUserLocationsFromDatabase();
        }catch (Exception e){
            Toast.makeText(getContext(), "Map initialization error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Cleans up binding reference when the view is destroyed to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}