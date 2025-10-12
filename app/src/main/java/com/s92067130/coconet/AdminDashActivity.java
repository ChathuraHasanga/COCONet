package com.s92067130.coconet;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * This activity is the **Admin Dashboard screen** that displays system statistics
 * including stock data, active users, and new signups. It fetches data from Firebase
 * and visualizes it using bar and pie charts.
 */
public class AdminDashActivity extends AppCompatActivity {

    private NetworkHelper networkHelper;
    private TextView offlineBanner;

    // Firebase references
    DatabaseReference mDatabase;
    FirebaseAuth mAuth;

    // UI components
    ImageView calenderIcon;
    private BarChart barChart;
    private PieChart pieChartProvince;
    private TextView stockDate, stockQuantity, activeUsers, newSignups;

    // Date format for parsing Firebase dates
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    // Selected date range
    private Date startDate = null, endDate = null;


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

            //set layout from resource file.
            setContentView(R.layout.activity_admin_dash);

            offlineBanner = findViewById(R.id.offlineBanner);

            networkHelper = new NetworkHelper(this);
            networkHelper.registerNetworkCallback(offlineBanner);

            //enable edge-to-edge layout.
            EdgeToEdge.enable(this);

            // Initialize all view components
            stockDate = findViewById(R.id.stockDate);
            stockQuantity = findViewById(R.id.stockQuantity);
            activeUsers =findViewById(R.id.activeUsers);
            newSignups = findViewById(R.id.newSignUps);
            barChart = findViewById(R.id.barChart);
            pieChartProvince = findViewById(R.id.pieChart);
            calenderIcon = findViewById(R.id.calenderIcon);

            // Open date picker when calendar icon is clicked
            calenderIcon.setOnClickListener( v-> showDateRangePicker());

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
    @Override
    protected void onDestroy() {
        super.onDestroy();
        networkHelper.unregisterNetworkCallback();
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
     * showDateRangePicker
     * Opens a DatePickerDialog to select a start and end date.
     * Once dates are selected, updates the statistics for the chosen period.
     */
    private void showDateRangePicker() {
        try {
            // Get the current date
            Calendar calender = Calendar.getInstance();

            // Start Date Picker
            DatePickerDialog startpicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                String start = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);

                try {
                    startDate = dateFormat.parse(start);

                    // End Date Picker
                    DatePickerDialog endpicker = new DatePickerDialog(this, (view2, year2, month2, day2) -> {
                        String end = String.format(Locale.getDefault(), "%04d-%02d-%02d", year2, month2 + 1, day2);
                        try {
                            endDate = dateFormat.parse(end);

                            // Update UI and charts
                            stockDate.setText("Period: " + start + " to " + end);
                            updateStatsForPeriod(startDate, endDate);
                        } catch (ParseException e) {
                            Toast.makeText(this, "Invalid end date", Toast.LENGTH_SHORT).show();
                        }
                    }, year, month, dayOfMonth);
                    endpicker.setTitle("Select End Date");
                    endpicker.show();

                } catch (ParseException e) {
                    Toast.makeText(this, "Invalid start date: ", Toast.LENGTH_SHORT).show();
                }
            }, calender.get(Calendar.YEAR), calender.get(Calendar.MONTH), calender.get(Calendar.DAY_OF_MONTH));

            startpicker.setTitle("Select Start Date");
            startpicker.show();
        } catch (Exception e) {
            Toast.makeText(this, "Error showing date range picker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * updateStatsForPeriod
     * Fetches stock and user data from Firebase within the selected date range,
     * aggregates statistics, and updates both text views and charts.
     */
    private void updateStatsForPeriod(Date start, Date end) {
        try {
            // Reference to "users" node in Firebase
            mDatabase = FirebaseDatabase.getInstance("https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("users");

            // Fetch data from Firebase once
            mDatabase.get().addOnSuccessListener(dataSnapshot -> {
                try {
                    // Variables to store statistics
                    int totalQuantity = 0;          // Sum of stock quantities
                    int activeUsersCount = 0;       // Users with stock activity
                    int newSignupsCount = 0;        // Users who signed up in range

                    // Province-wise stock data
                    Map<String, Integer> provinceTotals = new HashMap<>();   // Stock per province
                    Map<String, Integer> dailyTotals = new TreeMap<>();      // Stock per day (sorted)

                    // Iterate over each user in the database
                    for (DataSnapshot userSnap : dataSnapshot.getChildren()){

                        // Get user province and registration date
                        String province = userSnap.child("province").getValue(String.class);
                        String signupDateStr = userSnap.child("date").getValue(String.class);
                        if (province == null) province = "Unknown";

                        // Count new signups within range
                        if (signupDateStr != null){
                            Date signupDate = dateFormat.parse(signupDateStr);
                            if (signupDate != null && !signupDate.before(start) && !signupDate.after(end)){
                                newSignupsCount++;
                            }
                        }

                        // Fetch user's stock data
                        DataSnapshot stockDataSnap = userSnap.child("stock_data");
                        if(!stockDataSnap.exists()) continue;

                        boolean activeInPeriod = false;    // Track if user was active in selected range

                        // Loop through each stock entry
                        for (DataSnapshot stockEntry : stockDataSnap.getChildren()){
                            String dateStr = stockEntry.child("date").getValue(String.class);
                            Long quantity = stockEntry.child("quantity").getValue(Long.class);

                            // If stock entry matches selected date, update totals
                            if (dateStr == null || quantity == null) continue;

                            Date entryDate = dateFormat.parse(dateStr);

                            // Aggregate totals if entry is in range
                            if (entryDate != null && !entryDate.before(start) && !entryDate.after(end)) {
                                totalQuantity += quantity.intValue();
                                activeInPeriod = true;

                                provinceTotals.put(province, provinceTotals.getOrDefault(province, 0) + quantity.intValue());
                                dailyTotals.put(dateStr, dailyTotals.getOrDefault(dateStr, 0) + quantity.intValue());
                        }
                    }
                        // Count active users
                        if (activeInPeriod) activeUsersCount++;
                    }

                    // Update text views with calculated statistics
                    stockQuantity.setText("All Quantity: " + totalQuantity);
                    activeUsers.setText("Active Users: " + activeUsersCount);
                    newSignups.setText("New Sign ups: " + newSignupsCount);

                    //Add data to bar chart
                    List<BarEntry> barEntries = new ArrayList<>();
                    List<String> xLabeles = new ArrayList<>();
                    int xIndex = 0;
                    for (Map.Entry<String, Integer> entry : dailyTotals.entrySet()){
                        barEntries.add(new BarEntry(xIndex, entry.getValue()));
                        xLabeles.add(entry.getKey());
                        xIndex++;
                    }

                    BarDataSet barDataSet= new BarDataSet(barEntries, "Stock levels (Period) ");
                    barDataSet.setColor(Color.parseColor("#2196F3")); // Light Blue
                    BarData barData = new BarData(barDataSet);
                    barChart.setData(barData);

                    //set x axis formatter
                    XAxis xAxis = barChart.getXAxis();
                    xAxis.setValueFormatter(new ValueFormatter() {
                        @Override
                        public String getFormattedValue(float value) {
                            int i = (int) value;
                            if (i >=0 && i < xLabeles.size()) return xLabeles.get(i);
                            else return "";
                        }
                    });

                    xAxis.setGranularity(1f);
                    xAxis.setLabelRotationAngle(45f);
                    barChart.getDescription().setEnabled(false);
                    barChart.invalidate();

                    //pie chart for district distribution
                    List<PieEntry> provinceEntries = new ArrayList<>();
                    for (Map.Entry<String, Integer> entry : provinceTotals.entrySet()){
                        provinceEntries.add(new PieEntry(entry.getValue(), entry.getKey()));
                    }

                    PieDataSet provinceDataSet = new PieDataSet(provinceEntries, "Province Stock Distribution");
                    provinceDataSet.setColors(ColorTemplate.JOYFUL_COLORS);
                    pieChartProvince.setData(new PieData(provinceDataSet));
                    pieChartProvince.getDescription().setEnabled(false);
                    pieChartProvince.invalidate(); // refresh the chart

                }catch (Exception e){
                    Toast.makeText(this,"Error processing period stats: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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