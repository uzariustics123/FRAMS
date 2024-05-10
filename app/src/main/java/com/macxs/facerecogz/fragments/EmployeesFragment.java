package com.macxs.facerecogz.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.macxs.facerecogz.FaceRegistrationActivity;
import com.macxs.facerecogz.LoginActivity;
import com.macxs.facerecogz.R;
import com.macxs.facerecogz.Utils.PopupViews;
import com.macxs.facerecogz.adapters.EmployeeListadapter;
import com.macxs.facerecogz.databinding.EmployeeFragmentBinding;
import com.macxs.facerecogz.databinding.NewFaceRegisterBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmployeesFragment extends Fragment {
    EmployeeFragmentBinding employeeFragmentBinding;
    PopupViews popupViews;
    FirebaseApp firebaseApp;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    EmployeeListadapter employeeListAdpater;
    ArrayList<Map<String, Object>> employees = new ArrayList<>();
    ArrayList<Map<String, Object>> totalemployees = new ArrayList<>();
    NewFaceRegisterBinding newFaceRegisterBinding;
    Gson gson = new Gson();
    ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener;
    boolean isKeyboardShowing = false;
    View screenRootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        employeeFragmentBinding = EmployeeFragmentBinding.inflate(inflater, container, false);
        newFaceRegisterBinding = NewFaceRegisterBinding.inflate(inflater, container, false);
        return employeeFragmentBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        popupViews = new PopupViews(getActivity());
        initFirebases();
        initComponents(view);
        getEmployees();


    }

    @SuppressLint("ClickableViewAccessibility")
    private void initComponents(View refview) {

        screenRootView = refview.getRootView();
        onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                screenRootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = screenRootView.getRootView().getHeight();

                // Calculate the height difference between the screen and the visible display
                int heightDiff = screenHeight - r.bottom;

                if (heightDiff < 150) {
                    // Soft keyboard is hidden
                    if (isKeyboardShowing) {
                        employeeFragmentBinding.searchEt.clearFocus();
                        isKeyboardShowing = false;
                    }
                } else {
                    // Soft keyboard is shown
                    isKeyboardShowing = true;
                }
                Log.e("height", String.valueOf(heightDiff));
            }
        };
        screenRootView.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
        Drawable rightDrawable = ContextCompat.getDrawable(getActivity(), R.drawable.ic_clear);
        employeeFragmentBinding.searchEt.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, rightDrawable, null);
        employeeFragmentBinding.searchEt.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                // Get the right drawable of the EditText
                Drawable[] drawables = employeeFragmentBinding.searchEt.getCompoundDrawablesRelative();
                Drawable rightDrawable1 = drawables[2];

                // Check if the touch event occurred on the right drawable
                if (rightDrawable1 != null && event.getRawX() >= (employeeFragmentBinding.searchEt.getRight() - rightDrawable1.getBounds().width())) {
                    // Perform the desired action
                    employeeFragmentBinding.searchEt.setText("");
                    return true;
                }
            }
            return false;
        });
        employeeFragmentBinding.searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Log.e("keyword", charSequence.toString());
                if ((charSequence.toString().trim().isEmpty())) {
                    employees.clear();
                    employees.addAll(totalemployees);
                    employeeListAdpater.notifyDataSetChanged();
                    Log.e("empty keyword", charSequence.toString());
                    employeeFragmentBinding.employeeList.setVisibility(View.VISIBLE);
                    employeeFragmentBinding.employeeErrorLayout.setVisibility(View.GONE);
                } else {
                    filterList(charSequence.toString());
                }
            }


            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        employeeFragmentBinding.createfab.setOnClickListener(view -> {
            Intent i = new Intent(getActivity(), FaceRegistrationActivity.class);
            startActivity(i);
        });
        employeeFragmentBinding.refreshList.setOnRefreshListener(() -> {
            getEmployees();
            employeeFragmentBinding.refreshList.setRefreshing(false);
        });

    }

    private void filterList(String keyword) {
        ArrayList<Map<String, Object>> newListOfEmps = new ArrayList<>();
        for (Map emp : employees) {
            if (emp.get("firstname").toString().contains(keyword) || emp.get("lastname").toString().contains(keyword)) {
                newListOfEmps.add(emp);
            }
        }
        if (newListOfEmps.size() > 0){
            employees.clear();
            employees.addAll(newListOfEmps);
            employeeListAdpater.notifyDataSetChanged();
            Log.e("emps", String.valueOf(newListOfEmps.size()));
            Log.e("empstotal", String.valueOf(employees.size()));

            employeeFragmentBinding.employeeList.setVisibility(View.VISIBLE);
            employeeFragmentBinding.employeeErrorLayout.setVisibility(View.GONE);
        }else {
            employeeFragmentBinding.employeeList.setVisibility(View.GONE);
            employeeFragmentBinding.employeeErrorLayout.setVisibility(View.VISIBLE);
        }
    }

    public void initFirebases() {
        firebaseApp = FirebaseApp.initializeApp(getActivity());
        firebaseAuth = FirebaseAuth.getInstance();
    }

    public void showListEmpl() {
        if (!employees.isEmpty()) {
            employeeFragmentBinding.employeeList.setVisibility(View.VISIBLE);
            employeeFragmentBinding.employeeErrorLayout.setVisibility(View.GONE);

            employeeListAdpater = new EmployeeListadapter(getActivity(), getActivity().getSupportFragmentManager() ,employees);
            employeeFragmentBinding.employeeList.setAdapter(employeeListAdpater);
            employeeFragmentBinding.employeeList.setLayoutManager(new LinearLayoutManager(getActivity()));
            employeeFragmentBinding.employeeList.scheduleLayoutAnimation();
        } else {
            employeeFragmentBinding.employeeList.setVisibility(View.GONE);
            employeeFragmentBinding.employeeErrorLayout.setVisibility(View.VISIBLE);
        }
    }

    public void getEmployees() {
        popupViews.showLoading("Getting list of registered employees...");
        employeeFragmentBinding.employeeErrorLayout.setVisibility(View.GONE);
        db.collection("employees").get().addOnCompleteListener(task -> {
            popupViews.hideLoading();
            if (task.isSuccessful()) {
                employees.clear();
                totalemployees.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Log.d("asdw", document.getId() + " => " + document.getData());
                    Map employee = document.getData();
                    employee.put("employee-id", document.getId());
                    employees.add(employee);
//                    employees.add(formatEmployeeFaceData(employee));
                }
                totalemployees.addAll(employees);
                Log.e("totalemps", String.valueOf(totalemployees.size()));
            } else {
                Log.d("Error getting documents: ", task.getException().toString());
            }
            showListEmpl();
        });

    }

    public Map<String, Object> formatEmployeeFaceData(Map<String, Object> employeeData) {
        String facedateStr = (String) employeeData.get("face_data");
        String smilefacedateStr = (String) employeeData.get("smile_face_data");

        employeeData.put("face_data", getFormattedEmployeeFaceData(facedateStr));
        employeeData.put("smile_face_data", getFormattedEmployeeFaceData(smilefacedateStr));
        return employeeData;
    }
    public float[][] getFormattedEmployeeFaceData(Object data) {
        String facedataStr = (String) data;
        float[][] output = new float[1][192];

        List<?> arrayList = (List<?>) gson.fromJson(facedataStr, List.class);
        List<?> innerList = (List<?>) arrayList.get(0);

        innerList.forEach(item -> {
            if (item instanceof Double) {
                output[0][(int) innerList.indexOf(item)] = ((Double) item).floatValue();
            }
        });
        return output;
    }

    @Override
    public void onResume() {
        getEmployees();
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        ViewTreeObserver observer = screenRootView.getViewTreeObserver();
        observer.removeOnGlobalLayoutListener(onGlobalLayoutListener);
    }
}

