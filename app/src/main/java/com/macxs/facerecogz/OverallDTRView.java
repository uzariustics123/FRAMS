package com.macxs.facerecogz;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.print.PDFPrint;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.macxs.facerecogz.Utils.GenerateDTR;
import com.macxs.facerecogz.Utils.PopupViews;
import com.macxs.facerecogz.databinding.ActivityOverallDtrviewBinding;
import com.tejpratapsingh.pdfcreator.utils.FileManager;
import com.tejpratapsingh.pdfcreator.utils.PDFUtil;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OverallDTRView extends AppCompatActivity {
    GenerateDTR dtrgen;
    PopupViews popupViews;
    FirebaseApp firebaseApp;
    FirebaseAuth firebaseAuth;
    Gson gson = new Gson();
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    ActivityOverallDtrviewBinding binding;
    String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
    String htmlCode = "";
    int currentDay = LocalDate.now().getDayOfMonth();
    int currentYear = LocalDate.now().getYear();
    int currentMonthPos = LocalDate.now().getMonthValue()-1;
    ArrayList<Map<String, Object>> employees = new ArrayList<>();
//    Map<String, List>  employeesAttendances = new HashMap<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOverallDtrviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        popupViews = new PopupViews(this);
        dtrgen = new GenerateDTR();
        initFirebases();
        initCompnnts();
        getEmployees();
    }

    void getEmployees(){
        popupViews.showLoading("Getting employee list");
        db.collection("employees").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Log.d("asdw", document.getId() + " => " + document.getData());
                        Map<String, Object> employee = document.getData();
                        employee.put("employee-id", document.getId());
                        if (employee.get("face_data").toString().equals("unregistered")){
                            employees.add(employee);
                        }else {
                            employees.add(formatEmployeeFaceData(employee));
                        }
                        //init attendances
//                        employeesAttendances.put(document.getId(),new ArrayList<>());
                    }
                    LocalDateTime localDateTime = LocalDateTime.now();
                    getEmployeesAttendancesIn(currentMonthPos, currentYear);
                    binding.chipGo.setEnabled(false);
                } else {
                    Log.d("Error getting documents: ", task.getException().toString());
                }
            }
        });
    }
    void setYearsCurrentSelection(){
        LocalDateTime localDateTime = LocalDateTime.now();
        binding.spinmonth.setSelection(localDateTime.getMonthValue() - 1);
        for (int i = 0; i < getYears().length; i++) {
            if (Integer.parseInt(getYears()[i]) == localDateTime.getYear()) {
                binding.spinyear.setSelection(i);
                break;
            }
        }
    }


    void getEmployeesAttendancesIn(int monthIndex, int year){
        popupViews.showLoading("Please wait...");
        LocalDate preferedDate = LocalDate.of(year, monthIndex + 1,1);
        String dateStr = preferedDate.getMonth().toString()+" "+String.valueOf(currentDay)+", "+ String.valueOf(year);
        db.collection("attendance")
                .whereEqualTo("month", monthIndex + 1)
                .whereEqualTo("year", year)
                .get()
                .addOnCompleteListener(task -> {
                    popupViews.hideFaceLoading();
//                    Map<String, List>  freshEmployeesAttendances = new HashMap<>();
                    List<Map<String, Object>> attendances = new ArrayList<>();

                    for (QueryDocumentSnapshot document : task.getResult()) {
                        attendances.add(document.getData());

//                        employeesAttendances.put((String) document.get("employee-id"), (List) document.getData());
//                        if (freshEmployeesAttendances.get(document.get("employee-id")) != null){
//                            freshEmployeesAttendances.get(document.get("employee-id")).add(document.getData());
//                        }else {
//                            List<Map> newList = new ArrayList<>();
//                            newList.add(document.getData());
//                            freshEmployeesAttendances.put(document.get("employee-id").toString(), newList);
//                        }
                    }
                    String getDTrHTML = GenerateDTR.getOverAllEmployeeAttendances(dateStr, employees, attendances);
                    htmlCode = getDTrHTML;
                    String encodedHtml = Base64.encodeToString(getDTrHTML.getBytes(), Base64.NO_PADDING);
                    binding.webview.loadData(encodedHtml, "text/html", "base64");
                    popupViews.hideLoading();
                });
    }

    private void initCompnnts() {
        String[] years = getYears();
        ArrayAdapter monthsAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, months);
        ArrayAdapter yearsAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, years);
        monthsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinmonth.setAdapter(monthsAdapter);
        binding.spinyear.setAdapter(yearsAdapter);
        setYearsCurrentSelection();
        binding.spinmonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                currentMonthPos = position;
                binding.chipGo.setEnabled(true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        binding.spinyear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
//                getEmployeesAttendancesIn(binding.spinmonth.getSelectedItemPosition(), Integer.parseInt(binding.spinyear.getSelectedItem().toString()));
                currentYear = Integer.parseInt(binding.spinyear.getSelectedItem().toString());
                binding.chipGo.setEnabled(true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        binding.printBtn.setOnClickListener(view -> {
            print(htmlCode);
        });
        binding.closeBtn.setOnClickListener(view -> onBackPressed());
        binding.chipGo.setOnClickListener(view -> {
            getEmployeesAttendancesIn(currentMonthPos, currentYear);
            binding.chipGo.setEnabled(false);
        });
    }

    String[] getCurrentDays(){
        LocalDate prefferedDate = LocalDate.of(currentYear, currentMonthPos + 1, currentDay);
        int daysInMonth = prefferedDate.lengthOfMonth();

        // Create an int array to store the days of the current month
        String[] days = new String[daysInMonth];

        // Fill the int array with the days of the current month
        for (int i = 0; i < daysInMonth; i++) {
            days[i] = "Day "+ String.valueOf(i + 1);
        }
        return days;
    }
    void print(String htmlCode) {
        File pdfFile = FileManager.getInstance().createTempFile(this, "pdf", false);
        PDFUtil.generatePDFFromHTML(getApplicationContext(), pdfFile, htmlCode , new PDFPrint.OnPDFPrintListener(){

            @Override
            public void onSuccess(File file) {
                Uri pdfUri = Uri.fromFile(pdfFile);

                Intent intentPdfViewer = new Intent(OverallDTRView.this, PdfViewer.class);
//                intentPdfViewer.putExtra("sendto", employeeDetails.get("email").toString());
                intentPdfViewer.putExtra(PdfViewer.PDF_FILE_URI, pdfUri);

                startActivity(intentPdfViewer);
            }

            @Override
            public void onError(Exception exception) {
                popupViews.showError("Unable to print document");
            }
        });
    }
    String[] getYears() {
        String[] years = new String[10];
        int start_year = 2023;
        for (int i = 0; i < 10; i++) {
            years[i] = String.valueOf(start_year);
            start_year++;
        }
        Log.wtf("years", Arrays.deepToString(years));
        return years;
    }

    public void initFirebases() {
        firebaseApp = FirebaseApp.initializeApp(this);
        firebaseAuth = FirebaseAuth.getInstance();
    }
    public Map<String, Object> formatEmployeeFaceData(Map<String, Object> employeeData) {
        String facedateStr = (String) employeeData.get("face_data");
//        TypeToken<ArrayList<Object>> token = new TypeToken<ArrayList<Object>>() {};
        int OUTPUT_SIZE = 192;
        float[][] output = new float[1][OUTPUT_SIZE];
        ArrayList arrayList = (ArrayList) gson.fromJson(facedateStr, ArrayList.class);
        arrayList = (ArrayList) arrayList.get(0);
        for (int counter = 0; counter < arrayList.size(); counter++) {
            output[0][counter] = ((Double) arrayList.get(counter)).floatValue();
        }
        System.out.println("Entry output " + Arrays.deepToString(output));
        employeeData.put("face_data", output);
        return employeeData;
    }
}