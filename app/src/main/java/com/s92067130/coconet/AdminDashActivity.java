package com.s92067130.coconet;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.time.Month;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This activity is the **Admin Dashboard screen** that displays system statistics
 * including stock data, active users, and new signups. It fetches data from Firebase
 * and visualizes it using bar and pie charts.
 */
public class AdminDashActivity extends AppCompatActivity {

    // Firebase references
    DatabaseReference mDatabase;
    FirebaseAuth mAuth;

    // UI components
    ImageView calenderIcon;
    private BarChart barChart;
    private PieChart pieChartProvince;
    private TextView stockDate, stockQuantity, activeUsers, newSignups;


    /**
     * Called when the activity is starting. Initializes UI components,
     * hides action bars, and sets up event listeners.
     *
     * @param savedInstanceState Bundle: If the activity is being re-initialized,
     *                           this contains the most recent data (nullable).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        try {
            // Hide the default title bar
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            super.onCreate(savedInstanceState);

            // Hide the support action bar if available
            if(getSupportActionBar() != null){
                getSupportActionBar().hide();
            }

            //enable edge-to-edge layout.
            EdgeToEdge.enable(this);

            //set layout from resource file.
            setContentView(R.layout.activity_admin_dash);

            // Initialize all view components
            stockDate = findViewById(R.id.stockDate);
            stockQuantity = findViewById(R.id.stockQuantity);
            activeUsers =findViewById(R.id.activeUsers);
            newSignups = findViewById(R.id.newSignUps);
            barChart = findViewById(R.id.barChart);
            pieChartProvince = findViewById(R.id.pieChart);
            calenderIcon = findViewById(R.id.calenderIcon);

            // Open date picker when calendar icon is clicked
            calenderIcon.setOnClickListener( v-> showDatePicker());

            //Apply system bar insets to the root view with padding.
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }catch (Exception e){
            Toast.makeText(this, "Initialization error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handles back button click. Navigates the admin back to the Main Dashboard screen.
     *
     * @param view View: The button view that triggered this method.
     */
    public void OnClickBtnBackDash(View view) {
        try {
            // Navigate to MainActivity when the back arrow is clicked
            Intent intent = new Intent(AdminDashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }catch (Exception e){
            Toast.makeText(this, "Error navigating back: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Displays a DatePickerDialog for selecting a date.
     * Once a date is chosen, it updates the statistics for that day.
     */
    private void showDatePicker(){
        try {
            // Get the current date
            Calendar calender = Calendar.getInstance();
            int year = calender.get(Calendar.YEAR);
            int month = calender.get(Calendar.MONTH);
            int day = calender.get(Calendar.DAY_OF_MONTH);

            // Create and show date picker dialog
            DatePickerDialog picker = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
                String selectedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth+1, selectedDay);
                stockDate.setText("Date: " + selectedDate);

                // Load stats for selected day
                updateStatsForDay(selectedYear, selectedMonth, selectedDay);
            }, year, month,day);

            picker.setTitle("Select Date");
            picker.show();
        }catch (Exception e){
            Toast.makeText(this, "Error showing date picker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Loads and updates dashboard statistics (stock, active users, new signups) for a given day.
     *
     * @param year  int: Year of the selected date.
     * @param month int: Month (0-based index, i.e., January = 0).
     * @param day   int: Day of the selected date.
     *
     * @return void: This method updates the UI directly and does not return anything.
     */
    private void updateStatsForDay(int year, int month, int day) {
        try {
            // Reference to "users" node in Firebase
            mDatabase = FirebaseDatabase.getInstance("https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("users");

            // Fetch data from Firebase once
            mDatabase.get().addOnSuccessListener(dataSnapshot -> {
                try {
                    // Variables to store statistics
                    int totalQuantity = 0;
                    int activeUsersCount = 0;
                    int newSignupsCount = 0;
                    String selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day);

                    // Province-wise stock data
                    Map<String, Integer> provinceTotals = new HashMap<>();

                    // Iterate over each user in the database
                    for (DataSnapshot userSnap : dataSnapshot.getChildren()){

                        // Get user province and registration date
                        String province = userSnap.child("province").getValue(String.class);
                        String ownDate = userSnap.child("date").getValue(String.class);
                        if (province == null) province = "Unknown";

                        // Count new signups for this date
                        if (ownDate != null && ownDate.equals(selectedDate)) {
                            newSignupsCount++;
                        }
                        // Fetch user's stock data
                        DataSnapshot stockDataSnap = userSnap.child("stock_data");
                        if(!stockDataSnap.exists()) continue;

                        boolean isActiveToday = false;

                        // Loop through each stock entry
                        for (DataSnapshot stockEntry : stockDataSnap.getChildren()){
                            String date = stockEntry.child("date").getValue(String.class);
                            Long quantity = stockEntry.child("quantity").getValue(Long.class);
                            String storeName = stockEntry.child("storeName").getValue(String.class);

                            // If stock entry matches selected date, update totals
                            if (date != null && quantity != null && storeName != null && date.equals(selectedDate)) {
                                totalQuantity += quantity;

                                // Aggregate by province
                                int currentQty = provinceTotals.getOrDefault(province,0);
                                provinceTotals.put(province, currentQty+ quantity.intValue());
                            }

                            // Mark user as active if they had stock entry today
                            if (date != null && quantity != null && date.equals(selectedDate)) {
                                isActiveToday = true;
                            }
                        }
                        // Count user as active if stock was updated today
                        if (isActiveToday) activeUsersCount++;
                    }

                    // Update text views with calculated statistics
                    stockQuantity.setText("All Quantity: " + totalQuantity);
                    activeUsers.setText("Active Users: " + activeUsersCount);
                    newSignups.setText("New Sign ups: " + newSignupsCount);

                    //Add data to bar chart
                    List<BarEntry> entries = new ArrayList<>();
                    entries.add(new BarEntry(day, totalQuantity));
                    BarDataSet dataSet= new BarDataSet(entries, "Stock on " +selectedDate);
                    dataSet.setColor(Color.parseColor("#2196F3")); // Light Blue
                    barChart.setData(new BarData(dataSet));
                    barChart.getDescription().setEnabled(false);
                    barChart.invalidate();  //refresh chart

                    //pie chart for district distribution
                    List<PieEntry> provinceEntries = new ArrayList<>();
                    for (Map.Entry<String, Integer> entry : provinceTotals.entrySet()){
                        provinceEntries.add(new PieEntry(entry.getValue(), entry.getKey()));
                    }

                    PieDataSet provinceDataSet = new PieDataSet(provinceEntries, "Province stock (" + selectedDate + ")");
                    provinceDataSet.setColors(ColorTemplate.JOYFUL_COLORS);
                    pieChartProvince.setData(new PieData(provinceDataSet));
                    pieChartProvince.getDescription().setEnabled(false);
                    pieChartProvince.invalidate(); // refresh the chart

                }catch (Exception e){
                    Toast.makeText(this,"Error processing stats: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }

            }).addOnFailureListener(e -> {
                // Handle Firebase read failure
                stockQuantity.setText("All Quantity: Error");
                activeUsers.setText("Active Users: Error");
                newSignups.setText("New Sign ups: Error");
                Toast.makeText(this,"Failed to fetch data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }catch (Exception e){
            Toast.makeText(this,"Error loading stats: "+ e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}