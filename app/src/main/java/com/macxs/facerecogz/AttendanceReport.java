package com.macxs.facerecogz;

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

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.MemoryCacheSettings;
import com.google.firebase.firestore.PersistentCacheSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.macxs.facerecogz.Utils.GenerateDTR;
import com.macxs.facerecogz.Utils.PopupViews;
import com.macxs.facerecogz.databinding.ActivityAttendanceReportBinding;
import com.tejpratapsingh.pdfcreator.utils.FileManager;
import com.tejpratapsingh.pdfcreator.utils.PDFUtil;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class AttendanceReport extends AppCompatActivity {
    ActivityAttendanceReportBinding binding;
    GenerateDTR dtrgen;
    PopupViews popupViews;
    FirebaseApp firebaseApp;
    FirebaseAuth firebaseAuth;
    Map<String, Object> employeeDetails;
    Gson gson = new Gson();
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    String htmlCode = "";
    String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAttendanceReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initFirebases();
        initComponents();


    }

    void initComponents() {
        binding.closeBtn.setOnClickListener(view -> {
            onBackPressed();
        });
        LocalDateTime localDateTime = LocalDateTime.now();
        dtrgen = new GenerateDTR();
        popupViews = new PopupViews(this);
        binding.webview.getSettings().setBlockNetworkLoads(true);
        String[] years = getYears();
        ArrayAdapter monthsAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, months);
        ArrayAdapter yearsAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, years);
        monthsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinmonth.setAdapter(monthsAdapter);
        binding.spinyear.setAdapter(yearsAdapter);
        try {
            employeeDetails = gson.fromJson(getIntent().getStringExtra("empData"), Map.class);
            getAttendDetails((localDateTime.getMonthValue() - 1), localDateTime.getYear());
            binding.spinmonth.setSelection(localDateTime.getMonthValue() - 1);
            for (int i = 0; i < getYears().length; i++) {
                if (Integer.parseInt(getYears()[i]) == localDateTime.getYear()) {
                    binding.spinyear.setSelection(i);
                    break;
                }
            }

        } catch (Exception e) {

        }
        binding.spinmonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                getAttendDetails(position, Integer.parseInt(binding.spinyear.getSelectedItem().toString()));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        binding.spinyear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                getAttendDetails(binding.spinmonth.getSelectedItemPosition(), Integer.parseInt(binding.spinyear.getSelectedItem().toString()));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        binding.printBtn.setOnClickListener(view -> {
            print(htmlCode);
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

    private void getAttendDetails(int monthIndex, int year) {
        popupViews.showLoading("getting attendance reports");
        Log.e("request details", "emp-id:" + employeeDetails.get("employee-id") + " month:"+ String.valueOf(monthIndex+1) + "year:"+ String.valueOf(year));
        db.collection("attendance").whereEqualTo("employee-id", employeeDetails.get("employee-id"))
                .whereEqualTo("month", monthIndex + 1)
                .whereEqualTo("year", year)
                .get()
                .addOnCompleteListener(task -> {
                    ArrayList<Map<String, Object>> attendances = new ArrayList<>();
                    popupViews.hideFaceLoading();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        attendances.add(document.getData());

                    }
                    Log.e("attendance received", String.valueOf(attendances.size()));
                    String getDTrHTML = dtrgen.getHTMLreport(employeeDetails, attendances, monthIndex, year);
                    htmlCode = getDTrHTML;
                    String encodedHtml = Base64.encodeToString(getDTrHTML.getBytes(), Base64.NO_PADDING);
                    binding.webview.loadData(encodedHtml, "text/html", "base64");
                    popupViews.hideLoading();
                });
    }

    public void initFirebases() {
        firebaseApp = FirebaseApp.initializeApp(this);
        firebaseAuth = FirebaseAuth.getInstance();
    }

    void print(String htmlCode) {
        File pdfFile = FileManager.getInstance().createTempFile(this, "pdf", false);
        PDFUtil.generatePDFFromHTML(getApplicationContext(), pdfFile, htmlCode , new PDFPrint.OnPDFPrintListener(){

            @Override
            public void onSuccess(File file) {
                Uri pdfUri = Uri.fromFile(pdfFile);

                Intent intentPdfViewer = new Intent(AttendanceReport.this, PdfViewer.class);
                intentPdfViewer.putExtra("sendto", employeeDetails.get("email").toString());
                intentPdfViewer.putExtra(PdfViewer.PDF_FILE_URI, pdfUri);

                startActivity(intentPdfViewer);
            }

            @Override
            public void onError(Exception exception) {
                popupViews.showError("Unable to print document");
            }
        });
    }
}