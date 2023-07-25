package com.macxs.facerecogz;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.internal.LinkedTreeMap;
import com.macxs.facerecogz.Utils.MyAudioManager;
import com.macxs.facerecogz.Utils.PopupViews;
import com.macxs.facerecogz.databinding.ActivityMainBinding;
import com.macxs.facerecogz.fragments.DevicesFragment;
import com.macxs.facerecogz.fragments.EmployeesFragment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    EmployeeNoSQLDatabase myLocalDB;
    DrawerLayout drawerLayout;
    PopupViews popupViews;
    FirebaseApp firebaseApp;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FragmentTransaction fragmentTransaction;
    FragmentManager fragmentManager;
    public int CURRENT_FRAGMENT = 0;
    int EMPLOYEES_FRAGMENT = 1;
    int DEVICES_FRAGMENT = 2;
    int SCANNER_FRAGMENT = 3;
    EmployeesFragment employeesFragment;
    DevicesFragment devicesFragment;
    MyAudioManager myAudioManager;
    View headerView;
    PieChart pieChart;
    boolean totalEmpsLoaded = false;
    boolean attendeesLoaded = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        myLocalDB = new EmployeeNoSQLDatabase(this);
        fragmentManager = getSupportFragmentManager();
        drawerLayout = binding.drawer;
        myAudioManager = new MyAudioManager(this);

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        employeesFragment = new EmployeesFragment();
        devicesFragment = new DevicesFragment();
        setupDrawer();
        initFirebases();
//        binding.faby.setOnClickListener(view -> {
//            startActivity(a);
//        });
//        binding.regPerson.setOnClickListener(view -> {
//            startActivity(reg);
//        });
//        myAudioManager.play(MyAudioManager.dingAudio);


    }

    private void setupDrawer() {
//        DisplayMetrics displayMetrics = new DisplayMetrics();
//        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
//        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
//        int drawerWidth;
//        if (dpWidth >= 600) { // This is a tablet
//            drawerWidth = (int) (300 * displayMetrics.density);
//        } else { // This is a phone
//            drawerWidth = (int) (240 * displayMetrics.density);
//        }
//        DrawerLayout.LayoutParams drawerParams = (DrawerLayout.LayoutParams) drawerLayout.getChildAt(1).getLayoutParams();
//        drawerParams.width = drawerWidth;
//        drawerLayout.getChildAt(1).setLayoutParams(drawerParams);
        ActionBarDrawerToggle toggle =
                new ActionBarDrawerToggle(
                        this, binding.drawer, binding.toolbar, R.string.app_name, R.string.app_name) {
                    public void onDrawerOpened(View drawerView) {
                        getPieData();
                        pieChart.setVisibility(View.VISIBLE);
                        pieChart.animateY(1400, Easing.EaseInOutQuad);
                        pieChart.spin(2000, 0f, 360f, Easing.EaseInOutQuad);
                        super.onDrawerOpened(binding.drawer);

                    }

                    @Override
                    public void onDrawerClosed(View drawerView) {
                        super.onDrawerClosed(drawerView);
                        pieChart.setVisibility(View.INVISIBLE);
                    }

                };
        toggle.syncState();
        binding.drawer.addDrawerListener(toggle);
        setUpPieChart();
        navigationMenus();
    }

    private void navigationMenus() {
        binding.navigation.setNavigationItemSelectedListener(item -> {

            switch (item.getItemId()){
                case R.id.admin_menu_employee:
                    loadFragment(EMPLOYEES_FRAGMENT);
                    break;
                case R.id.admin_menu_devices:
                    loadFragment(DEVICES_FRAGMENT);
                    break;
                case R.id.pincode:
                    Intent intent = new Intent(this, ChangePin.class);
                    startActivity(intent);
                    break;
                case R.id.admin_menu_logout:
                    logout();
                    break;
                case R.id.admin_menu_exit:
                    onBackPressed();
                    break;
            }

            return false;
        });
    }
    public void loadFragment(int toFragmentid) {
        binding.drawer.closeDrawer(GravityCompat.START);
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        if(toFragmentid != CURRENT_FRAGMENT){
            if (toFragmentid == EMPLOYEES_FRAGMENT){
                CURRENT_FRAGMENT = EMPLOYEES_FRAGMENT;
                fragmentTransaction.replace(binding.mainFrame.getId(), employeesFragment);
                fragmentTransaction.commit();
            }
            else if(toFragmentid == DEVICES_FRAGMENT){
                CURRENT_FRAGMENT = DEVICES_FRAGMENT;
                fragmentTransaction.replace(binding.mainFrame.getId(), devicesFragment);
                fragmentTransaction.commit();
            }
            else if(toFragmentid == SCANNER_FRAGMENT){
                CURRENT_FRAGMENT = SCANNER_FRAGMENT;
//                fragmentTransaction.replace(binding.mainFrame.getId(), adminEventFragment);
//                fragmentTransaction.commit();
            }

        }

    }

    public void initFirebases() {
        firebaseApp = FirebaseApp.initializeApp(this);
        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseAuth.AuthStateListener authStateListener =
                firebaseAuth -> {
                    if (firebaseAuth.getCurrentUser() == null) {
                        Intent i = new Intent(this, LoginActivity.class);
                        startActivity(i);
                        finish();
                    }
                };
        firebaseAuth.addAuthStateListener(authStateListener);
    }
    public void logout(){
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle("Confirm to sign out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign out", (materialdialog, which) -> {
                    materialdialog.dismiss();
                    firebaseAuth.signOut();
                })
                .setNegativeButton("Cancel", (materialdialog, which) -> {
                    materialdialog.dismiss();
                });
        dialog.create().show();

    }
    private void hasPinCode() {
        db.collection("admin-configs").document("admin-configs").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                String pincode = (String) document.get("pincode");
                if (pincode != null){
                    Log.e("pin", pincode);
                }else{

                }

            } else {

            }

        });
    }
    private void setUpPieChart(){
        headerView = binding.navigation.getHeaderView(0);
        pieChart = headerView.findViewById(R.id.pie_chart);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setUsePercentValues(true);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.animateY(1400, Easing.EaseInOutQuad);
        pieChart.setEntryLabelColor(Color.WHITE);
        pieChart.setEntryLabelTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
        pieChart.setEntryLabelTextSize(12f);
        pieChart.setDrawEntryLabels(false);
        pieChart.getDescription().setEnabled(false);
        getPieData();
        Legend legend = pieChart.getLegend();
        legend.setTextSize(12f);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setXEntrySpace(7f);
        legend.setYEntrySpace(0f);
        legend.setYOffset(0f);

    }
    private void getPieData() {

        LocalDateTime localDateTime = LocalDateTime.now();
        int monthInd = localDateTime.getMonthValue() - 1;
        int yearNow = localDateTime.getYear();
        Set<String> attendedIDs = new HashSet<>();
        ArrayList<Map<String, Object>> totalEmps = new ArrayList<>();
        int totalEmpsID = 0;
        db.collection("attendance")
                .whereEqualTo("month", monthInd + 1)
                .whereEqualTo("year", yearNow)
                .get()
                .addOnCompleteListener(task -> {
                    ArrayList<Map<String, Object>> attendances = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        attendances.add(document.getData());
                        if (!attendedIDs.contains((String) document.get("employee-id"))){
                            attendedIDs.add((String) document.get("employee-id"));
                        }
                    }
                    attendeesLoaded = true;
                    if (attendeesLoaded && totalEmpsLoaded){
                        updatePieData(attendedIDs.size(), totalEmps.size());
                    }
                    Log.e("attendees received", String.valueOf(attendedIDs.size()));
                });
        db.collection("employees").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()){

                for (QueryDocumentSnapshot document : task.getResult()) {
                    Map employee = document.getData();
                    employee.put("employee-id", document.getId());
                    totalEmps.add(employee);
                }
                totalEmpsLoaded = true;

                if (attendeesLoaded && totalEmpsLoaded){
                    updatePieData(attendedIDs.size(), totalEmps.size());
                    Log.e("total empes", String.valueOf(totalEmps.size()));
                }
            }

                });

    }
    void updatePieData(int numOfAttendees, int totalEmpNum){
        Log.e("final count total", String.valueOf(totalEmpNum));
        Log.e("final count attendees", String.valueOf(numOfAttendees));
        int absentees = (totalEmpNum - numOfAttendees);
        pieChart.setCenterText(generateCenterSpannableText(totalEmpNum));
        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(numOfAttendees, String.valueOf(numOfAttendees)+" Attendees Today"));
        entries.add(new PieEntry(absentees, String.valueOf(absentees)+" Absentees Today"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setSliceSpace(1f);
//        dataSet.setColors(new int[] { Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW });

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value) + " %";
            }
        });
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.WHITE);
        data.setValueTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
        pieChart.highlightValues(null);
        pieChart.setData(data);
        pieChart.invalidate(); // refreshes the chart
    }
    private SpannableString generateCenterSpannableText(int numberOfEmps) {

        SpannableString s = new SpannableString(String.valueOf(numberOfEmps)+ " Employees");
//        s.setSpan(new RelativeSizeSpan(1.7f), 0, s.length() -1, 0);
//        s.setSpan(new StyleSpan(Typeface.NORMAL), 14, s.length() - 15, 0);
//        s.setSpan(new ForegroundColorSpan(Color.GRAY), 14, s.length() - 15, 0);
//        s.setSpan(new RelativeSizeSpan(.8f), 14, s.length() - 15, 0);
//        s.setSpan(new StyleSpan(Typeface.ITALIC), s.length() - 14, s.length(), 0);
        s.setSpan(new ForegroundColorSpan(ColorTemplate.getHoloBlue()), 0, s.length(), 0);
        return s;
    }
    private void setPinCode(){

    }

    @Override
    protected void onStart() {
        super.onStart();
       hasPinCode();
       loadFragment(EMPLOYEES_FRAGMENT);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(this, ScanPerson.class));
    }

    @Override
    protected void onDestroy() {
        myAudioManager.release();
        super.onDestroy();
    }
}