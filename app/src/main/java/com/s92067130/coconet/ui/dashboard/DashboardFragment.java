package com.s92067130.coconet.ui.dashboard;

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
import com.s92067130.coconet.databinding.FragmentDashboardBinding;

import org.w3c.dom.Text;

import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment implements OnMapReadyCallback {

    private FragmentDashboardBinding binding;

    // initialize GoogleMap instance
    private GoogleMap myMap;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textDashboard;
        dashboardViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Access the EditText and ImageView for searching locations
        EditText locationInput = root.findViewById(R.id.editLocationInput);
        ImageView searchButton = root.findViewById(R.id.searchBtn);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String locationName = locationInput.getText().toString().trim();
                if(!locationName.isEmpty()){
                    findLocationByName(locationName);
                }else {
                    Toast.makeText(getContext(), "Please enter a location", Toast.LENGTH_SHORT).show();
                }
            }
        });
        return root;
    }

    private void findLocationByName(String locationName) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(locationName,1);
            if (addresses != null && !addresses.isEmpty()){
                Address address = addresses.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
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

    private void loadUserLocationsFromDatabase() {
        DatabaseReference databaseRef = FirebaseDatabase.getInstance("https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("users");

        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String name = userSnapshot.child("name").getValue(String.class);
                    String locationName = userSnapshot.child("locationTxt").getValue(String.class);
                    Double lat = userSnapshot.child("latitude").getValue(Double.class);
                    Double lng = userSnapshot.child("longitude").getValue(Double.class);

                    // Check if location is available
                    if (lat != null && lng != null && name != null) {
                        DataSnapshot stockDataSnap = userSnapshot.child("stock_data");
                        Stock latestStock = null;
                        long latestTime = Long.MIN_VALUE;

                        // Loop through stock entries to find latest
                        for (DataSnapshot stockSnap : stockDataSnap.getChildren()) {
                            Stock stock = stockSnap.getValue(Stock.class);
                            if (stock != null && stock.timestamp > latestTime) {
                                latestTime = stock.timestamp;
                                latestStock = stock;
                            }
                        }

                        // Only add marker if stock exists
                        if (latestStock != null && latestStock.storeName != null) {
                            LatLng position = new LatLng(lat, lng);
                            String infoText = "Store: " + latestStock.storeName +
                                    "\nCurrent stock: " + latestStock.quantity+
                                    "\nOn: " + latestStock.date;

                            myMap.addMarker(new MarkerOptions()
                                    .position(position)
                                    .title(name)
                                    .snippet(infoText));
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load user stock locations", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        myMap = googleMap;
//        LatLng place1 = new LatLng(7.073074876678307, 80.01608941396255);
//        myMap.addMarker(new MarkerOptions().position(place1).title("Gampaha"));
//        myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(place1, 18.0f));
//        myMap.getUiSettings().setZoomControlsEnabled(true);

        LatLng sriLankaCenter = new LatLng(7.8731, 80.7718);
        myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sriLankaCenter, 7.5f));

        myMap.getUiSettings().setZoomControlsEnabled(true);

        //set custom info
        myMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Nullable
            @Override
            public View getInfoWindow(@NonNull com.google.android.gms.maps.model.Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(@NonNull com.google.android.gms.maps.model.Marker marker) {

                // Inflate custom layout for info window
                View view = LayoutInflater.from(getContext()).inflate(R.layout.custom_mapinfo, null);

                //TextView title = view.findViewById(R.id.infoTitle);
                TextView snippet = view.findViewById(R.id.infoSnippet);

                //title.setText(marker.getTitle());
                snippet.setText(marker.getSnippet());

                return view;
            }
        });

        myMap.setOnInfoWindowClickListener(marker -> {
            Intent intent = new Intent(requireContext(), ContactActivity.class);
            startActivity(intent);
        });

        //load markers from firebase
        loadUserLocationsFromDatabase();
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}