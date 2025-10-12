package com.s92067130.coconet;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class UserManagementActivity  extends AppCompatActivity {
    private NetworkHelper networkHelper;
    private TextView offlineBanner;

    RecyclerView recyclerView;
    DatabaseReference userRef;
    Spinner spinnerProvince, spinnerStatus;
    UserAdapter userAdapter;
    EditText editSearch;
    Button btnLoadMore;
    final List<User> userList = new ArrayList<>();

    private String selectedProvince = "All";
    private String selectedStatus = "All";

    private static final int PAGE_SIZE = 50;
    private String lastkey = null;
    private boolean isLoading = false;
    private boolean endReached = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        // Hide the support action bar if available
        if(getSupportActionBar() != null){
            getSupportActionBar().hide();
        }

        offlineBanner = findViewById(R.id.offlineBanner);

        networkHelper = new NetworkHelper(this);
        networkHelper.registerNetworkCallback(offlineBanner);

        // Initialize RecyclerView and DatabaseReference here
        recyclerView = findViewById(R.id.recyclerViewUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        spinnerProvince = findViewById(R.id.spinnerProvince);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        editSearch = findViewById(R.id.editSearch);
        btnLoadMore = findViewById(R.id.btnLoadMore);

        userRef = FirebaseDatabase.getInstance("https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("users");

        //init adapter once
        userAdapter = new UserAdapter(userList, userRef, this);
        recyclerView.setAdapter(userAdapter);

        //province spinner
        String[] provinces = {"All", "Western Province", "Central Province", "Southern Province",
                "Northern Province", "Eastern Province", "North Western Province",
                "North Central Province", "Uva Province", "Sabaragamuwa Province"};

        //Status Spinner
        ArrayAdapter<String> provinceAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, provinces);
        spinnerProvince.setAdapter(provinceAdapter);

        spinnerProvince.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                selectedProvince = provinces[position];
                resetAndLoad();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //status spinner
        String[] statuses = {"All", "active", "suspended"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, statuses);
        spinnerStatus.setAdapter(statusAdapter);

        spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                selectedStatus = statuses[position];
                resetAndLoad();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //search
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                resetAndLoad();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        //load more
        btnLoadMore.setOnClickListener(v-> loadNextPage());

        // Initial load
        resetAndLoad();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        networkHelper.unregisterNetworkCallback();
    }

    private void resetAndLoad(){
        userList.clear();
        userAdapter.notifyDataSetChanged();
        lastkey = null;
        endReached = false;
        btnLoadMore.setVisibility(View.GONE);
        loadNextPage();
    }

    private void loadNextPage(){
        if (isLoading || endReached) return;
        isLoading = true;

        Query q = userRef.orderByKey();
        int fetchSize = PAGE_SIZE * 3;
        if (lastkey == null){
            q = q.limitToFirst(fetchSize);
        }else {
            q = q.startAt(lastkey).limitToFirst(fetchSize + 1);
        }

        q.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int received = 0;
                boolean skipFirst = lastkey != null;

                String search = editSearch.getText().toString().trim().toLowerCase();

                for (DataSnapshot snap : snapshot.getChildren()){
                    String key = snap.getKey();
                    if (skipFirst){
                        skipFirst = false;
                        if (key != null && key.equals(lastkey)) {
                            continue;
                        }
                    }

                    User u = snap.getValue(User.class);
                    if (u == null) continue;
                    u.setUid(key);

                    //province filter
                    if (!"All".equals(selectedProvince)){
                        if (u.getProvince() == null || !u.getProvince().equalsIgnoreCase(selectedProvince)){
                            continue;
                        }
                    }

                    //status filter
                    if (!"All".equals(selectedStatus)){
                        if (u.getStatus() == null || !u.getStatus().equalsIgnoreCase(selectedStatus)){
                            continue;
                        }
                    }

                    //search by email
                    if (!search.isEmpty()){
                        String email = u.getEmail() == null ?"": u.getEmail().toLowerCase();
                        if (!email.contains(search)){
                            continue;
                        }
                    }

                    userList.add(u);
                    userAdapter.notifyItemInserted(userList.size() -1);
                    lastkey = key;
                    received++;
                    if (received == PAGE_SIZE) break;
                }

                if (received < PAGE_SIZE) endReached = true;
                // Show Load More if there are potentially more pages
                btnLoadMore.setVisibility(received == PAGE_SIZE ? View.VISIBLE : View.GONE);

                isLoading = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                isLoading = false;
            }
        });
    }

    public void OnClickBtnBackRole(View view) {
        try {
            Intent intent = new Intent(UserManagementActivity.this, MainActivity.class);
            startActivity(intent);
        }catch (Exception e){
            Toast.makeText(this, "Navigation Error : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
