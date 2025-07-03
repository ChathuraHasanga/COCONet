package com.s92067130.coconet;

import android.app.DatePickerDialog;
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

//Activity class for Admin Dashboard screen for viewing statistics and stock data
public class AdminDashActivity extends AppCompatActivity {

    DatabaseReference mDatabase;
    FirebaseAuth mAuth;
    ImageView calenderIcon;
    private BarChart barChart;
    private PieChart pieChartProvince;
    private TextView stockDate, stockQuantity, activeUsers, newSignups;


    //called when the activity is starting.
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        try {
            //hide title bar
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            super.onCreate(savedInstanceState);

            //hide the action bar
            if(getSupportActionBar() != null){
                getSupportActionBar().hide();
            }

            //enable edge-to-edge layout.
            EdgeToEdge.enable(this);

            //set layout from resource file.
            setContentView(R.layout.activity_admin_dash);

            //Initialize views
            stockDate = findViewById(R.id.stockDate);
            stockQuantity = findViewById(R.id.stockQuantity);
            activeUsers =findViewById(R.id.activeUsers);
            newSignups = findViewById(R.id.newSignUps);
            barChart = findViewById(R.id.barChart);
            pieChartProvince = findViewById(R.id.pieChart);
            calenderIcon = findViewById(R.id.calenderIcon);

            //Date selection Handler.
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

    // Show a date picker dialog to select the report date
    private void showDatePicker(){
        try {
            Calendar calender = Calendar.getInstance();
            int year = calender.get(Calendar.YEAR);
            int month = calender.get(Calendar.MONTH);
            int day = calender.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog picker = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
                String selectedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth+1, selectedDay);
                stockDate.setText("Date: " + selectedDate);
                updateStatsForDay(selectedYear, selectedMonth, selectedDay);
            }, year, month,day);

            picker.setTitle("Select Date");
            picker.show();
        }catch (Exception e){
            Toast.makeText(this, "Error showing date picker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    //Load and display dashboard statistics for the selected day.
    private void updateStatsForDay(int year, int month, int day) {
        try {
            mDatabase = FirebaseDatabase.getInstance("https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("users");

            mDatabase.get().addOnSuccessListener(dataSnapshot -> {
                try {
                    int totalQuantity = 0;
                    int activeUsersCount = 0;
                    int newSignupsCount = 0;
                    String selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day);
                    Map<String, Integer> provinceTotals = new HashMap<>();

                    for (DataSnapshot userSnap : dataSnapshot.getChildren()){

                        String province = userSnap.child("province").getValue(String.class);
                        String ownDate = userSnap.child("date").getValue(String.class);
                        if (province == null) province = "Unknown";

                        //New user signup count
                        if (ownDate != null && ownDate.equals(selectedDate)) {
                            newSignupsCount++;
                        }
                        DataSnapshot stockDataSnap = userSnap.child("stock_data");
                        if(!stockDataSnap.exists()) continue;

                        boolean isActiveToday = false;

                        for (DataSnapshot stockEntry : stockDataSnap.getChildren()){
                            String date = stockEntry.child("date").getValue(String.class);
                            Long quantity = stockEntry.child("quantity").getValue(Long.class);
                            String storeName = stockEntry.child("storeName").getValue(String.class);

                            if (date != null && quantity != null && storeName != null && date.equals(selectedDate)) {
                                totalQuantity += quantity;

                                int currentQty = provinceTotals.getOrDefault(province,0);
                                provinceTotals.put(province, currentQty+ quantity.intValue());
                            }

                            if (date != null && quantity != null && date.equals(selectedDate)) {
                                isActiveToday = true;
                            }
                        }
                        if (isActiveToday) activeUsersCount++;
                    }

                    //set text views.
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