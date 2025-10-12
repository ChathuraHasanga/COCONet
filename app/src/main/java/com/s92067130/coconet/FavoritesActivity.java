package com.s92067130.coconet;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {
    private NetworkHelper networkHelper;
    private TextView offlineBanner;

    private RecyclerView recyclerFavourites;
    private FavouriteAdapter adapter;
    private List<Store> favouriteStores;
    private List<String> favouriteIds;
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);
        EdgeToEdge.enable(this);

        //hide the action bar if not null
        if (getSupportActionBar() != null){
            getSupportActionBar().hide();
        }
        offlineBanner = findViewById(R.id.offlineBanner);

        networkHelper = new NetworkHelper(this);
        networkHelper.registerNetworkCallback(offlineBanner);

        recyclerFavourites = findViewById(R.id.recyclerFavourites);
        recyclerFavourites.setLayoutManager(new LinearLayoutManager(this));

        favouriteStores = new ArrayList<>();
        favouriteIds = new ArrayList<>();
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        adapter = new FavouriteAdapter(favouriteIds, favouriteStores, this, currentUid);
        recyclerFavourites.setAdapter(adapter);

        loadFavourites();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        networkHelper.unregisterNetworkCallback();
    }

    private void loadFavourites() {
        DatabaseReference favRef = FirebaseDatabase.getInstance(
                        "https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("users")
                .child(currentUid)
                .child("favorites");

        TextView tvNoFavorites = findViewById(R.id.tvNoFavorites);

        favRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                favouriteStores.clear();
                favouriteIds.clear();

                if (!snapshot.exists()){

                    tvNoFavorites.setVisibility(View.VISIBLE);
                }else {
                    tvNoFavorites.setVisibility(View.GONE);
                    for (DataSnapshot favSnapshot : snapshot.getChildren()) {
                        String storeId = favSnapshot.child("storeId").getValue(String.class);
                        String storeName = favSnapshot.child("storeName").getValue(String.class);
                        String ownerName = favSnapshot.child("name").getValue(String.class);
                        String district = favSnapshot.child("district").getValue(String.class);
                        String contactNumber = favSnapshot.child("contactNumber").getValue(String.class);
                        String note = favSnapshot.child("note").getValue(String.class);

                        favouriteIds.add(storeId);
                        Store store = new Store(storeId, storeName,ownerName, district, contactNumber,note);
                        favouriteStores.add(store);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }


    public void OnClickBtnBackFavorite(View view) {
        try {
            Intent intent = new Intent(FavoritesActivity.this, MainActivity.class);
            startActivity(intent);
        }catch (Exception e){
            Toast.makeText(this, "Navigation Error : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}

