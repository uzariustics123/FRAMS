package com.macxs.facerecogz.adapters;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.gson.Gson;
import com.macxs.facerecogz.AttendanceReport;
import com.macxs.facerecogz.FaceRegistrationActivity;
import com.macxs.facerecogz.Utils.PopupViews;
import com.macxs.facerecogz.databinding.EmpPopupBinding;
import com.macxs.facerecogz.databinding.EmployeeListItemBinding;
import com.macxs.facerecogz.databinding.NewFaceRegisterBinding;
import com.sdsmdg.tastytoast.TastyToast;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmployeeListadapter extends RecyclerView.Adapter<EmployeeListadapter.ViewHolder> {
    ArrayList<Map<String, Object>> employees;
    String imgIconUrl = "https://icon-library.com/images/user-icon-image/user-icon-image-2.jpg";
    Context context;
    BottomSheetDialog bottomSheetDialog;
    BottomSheetDialog editSheetDialog;
    NewFaceRegisterBinding newFaceRegisterBinding;
    EmpPopupBinding empPopupBinding;
    FirebaseApp firebaseApp;
    FirebaseAuth firebaseAuth;
    PopupViews popupViews;
    Gson gson = new Gson();
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    String amstarttime = "8:00 AM";
    String pmstarttime = "1:00 PM";
    String pmEndtime = "5:00 PM";
    String amEndtime = "12:00 PM";
    FragmentManager fragmentManager;
    DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .appendPattern("h:mm a")
            .optionalStart()
            .appendPattern("hh:mm a")
            .optionalEnd()
            .toFormatter();
    LocalTime defpmEndTime = LocalTime.parse("5:00 PM", formatter);
    LocalTime defpmStartTime = LocalTime.parse("1:00 PM", formatter);
    boolean changesMade = false;

    public EmployeeListadapter(Activity context, FragmentManager fragmentManager, ArrayList<Map<String, Object>> employees) {
        this.fragmentManager = fragmentManager;
        this.employees = employees;
        this.context = context;
        bottomSheetDialog = new BottomSheetDialog(context);
        editSheetDialog = new BottomSheetDialog(context);
        empPopupBinding = EmpPopupBinding.inflate(((Activity) context).getLayoutInflater());
        newFaceRegisterBinding = NewFaceRegisterBinding.inflate(((Activity) context).getLayoutInflater());
        bottomSheetDialog.setContentView(empPopupBinding.getRoot());
        initFirebases();
        popupViews = new PopupViews(context);
    }

    public void initFirebases() {
        firebaseApp = FirebaseApp.initializeApp(context);
        firebaseAuth = FirebaseAuth.getInstance();
    }

    void editEmployeeDetails(Map employeeData, int position) {
        Map<String, Object> newEmpdata = employeeData;
        String oldEmail = employeeData.get("email").toString();
        String oldfname = employeeData.get("firstname").toString();
        String odllname = employeeData.get("lastname").toString();
        String oldmname = employeeData.get("middlename").toString();
        String inAmtime = employeeData.get("am-in-time").toString();
        String outAmtime = employeeData.get("am-out-time").toString();
        String inPmtime = employeeData.get("pm-in-time").toString();
        String outPmtime = employeeData.get("pm-out-time").toString();
        editSheetDialog.setContentView(newFaceRegisterBinding.getRoot());
        editSheetDialog.show();
        newFaceRegisterBinding.actTitle.setText("Edit Details");
        newFaceRegisterBinding.email.setText(oldEmail);
        newFaceRegisterBinding.Fname.setText(oldfname);
        newFaceRegisterBinding.lname.setText(odllname);
        newFaceRegisterBinding.midname.setText(oldmname);
        newFaceRegisterBinding.imageCard.setVisibility(View.GONE);

        newFaceRegisterBinding.mornChipIn.setText(inAmtime);
        newFaceRegisterBinding.mornChipOut.setText(outAmtime);
        newFaceRegisterBinding.noonChipIn.setText(inPmtime);
        newFaceRegisterBinding.noonChipOut.setText(outPmtime);

        newFaceRegisterBinding.btClose.setOnClickListener(view -> {
            editSheetDialog.dismiss();
        });
        View.OnClickListener changeTimeListener = view -> {
            changeTime((Chip) view);
        };
        newFaceRegisterBinding.mornChipIn.setOnClickListener(changeTimeListener);
        newFaceRegisterBinding.mornChipOut.setOnClickListener(changeTimeListener);
        newFaceRegisterBinding.noonChipIn.setOnClickListener(changeTimeListener);
        newFaceRegisterBinding.noonChipOut.setOnClickListener(changeTimeListener);
        newFaceRegisterBinding.conBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String newFname = newFaceRegisterBinding.Fname.getText().toString();
                String newLname = newFaceRegisterBinding.lname.getText().toString();
                String newMname = newFaceRegisterBinding.midname.getText().toString();
                String newEmail = newFaceRegisterBinding.email.getText().toString();

                String newAmInTime = newFaceRegisterBinding.mornChipIn.getText().toString();
                String newAmOutTime = newFaceRegisterBinding.mornChipOut.getText().toString();
                String newPmInTime = newFaceRegisterBinding.noonChipIn.getText().toString();
                String newPmOutTime = newFaceRegisterBinding.noonChipOut.getText().toString();

                LocalTime amInTime = LocalTime.parse(newFaceRegisterBinding.mornChipIn.getText().toString(), formatter);
                LocalTime pmInTime = LocalTime.parse(newFaceRegisterBinding.noonChipIn.getText().toString(), formatter);
                LocalTime amOutime = LocalTime.parse(newFaceRegisterBinding.mornChipOut.getText().toString(), formatter);
                LocalTime pmOutime = LocalTime.parse(newFaceRegisterBinding.noonChipOut.getText().toString(), formatter);
                if (!odllname.equals(newLname) ||
                        !oldfname.equals(newFname) ||
                        !oldmname.equals(newMname) ||
                        !inAmtime.equals(newAmInTime) ||
                        !outAmtime.equals(newAmOutTime) ||
                        !inPmtime.equals(newPmInTime) ||
                        !outPmtime.equals(newPmOutTime) ||
                        !oldEmail.equals(newEmail)) {
                    changesMade = true;
                }
                if (changesMade) {
                    newEmpdata.put("firstname", newFname);
                    newEmpdata.put("lastname", newLname);
                    newEmpdata.put("middlename", newMname);
                    newEmpdata.put("email", newEmail);
                    if (amInTime.isAfter(amOutime)) {
                        popupViews.toastWarn("In time for morning cannot be after the out time");
                    } else if (amOutime.isAfter(pmInTime)) {
                        popupViews.toastWarn("Out time for morning cannot be after the in time for the afternoon");
                    } else if (pmInTime.isAfter(pmOutime)) {
                        popupViews.toastWarn("In time for afternoon cannot be after the out time for the afternoon");
                    } else if (amOutime.isAfter(defpmStartTime)) {
                        popupViews.toastWarn("Morning out attendance should not be assigned after the in attendance for afternoon");
                    } else if (pmInTime.isAfter(defpmEndTime)) {
                        popupViews.toastWarn("Afternoon attendance should not be assigned in the morning time");
                    } else {
                        newEmpdata.put("am-in-time", amstarttime);
                        newEmpdata.put("am-out-time", amEndtime);
                        newEmpdata.put("pm-in-time", pmstarttime);
                        newEmpdata.put("pm-out-time", pmEndtime);
                        updateEmpDetails(employeeData, newEmpdata);
                        editSheetDialog.dismiss();
                    }

                } else {
                    popupViews.toastWarn("No changes were made");
                }
            }
        });
        editSheetDialog.setOnDismissListener(dialogInterface -> changesMade = false);
    }

    private void updateEmpDetails(Map employeeData, Map<String, Object> newEmpdata) {
        String empID = employeeData.get("employee-id").toString();
        String face_data = gson.toJson(getFormattedEmployeeFaceData(employeeData.get("face_data")));
        String smile_face_data = gson.toJson(getFormattedEmployeeFaceData(employeeData.get("smile_face_data")));
        newEmpdata.put("face_data", face_data);
        newEmpdata.put("smile_face_data", smile_face_data);
        db.collection("employees").document(empID).set(newEmpdata).addOnSuccessListener(unused -> {
            popupViews.toastSuccess("Details updated");

        }).addOnFailureListener(e -> {
            popupViews.toastError("Unable to update employee details");

        }).addOnCompleteListener(task -> {

        });
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

    private void showPopup(Map employeeData, int position) {
        String fname = employeeData.get("firstname").toString();
        String employeeid = employeeData.get("employee-id").toString();
        String lname = employeeData.get("lastname").toString();
        String email = employeeData.get("email").toString();
        empPopupBinding.profileName.setText(fname + " " + lname);
        empPopupBinding.emailprofile.setText(email);

        Glide.with(context).load(imgIconUrl).into(empPopupBinding.profileAvatar);
        empPopupBinding.editBtn.setOnClickListener(view -> {
            editEmployeeDetails(employeeData, position);
            bottomSheetDialog.dismiss();
        });
        empPopupBinding.deleteEmp.setOnClickListener(view -> {
            warnDeleteEmp(employeeid, position);
            bottomSheetDialog.dismiss();
        });
        empPopupBinding.attBtn.setOnClickListener(view -> {
            Intent i = new Intent(context, AttendanceReport.class);
            i.putExtra("empData", gson.toJson(employeeData));
            context.startActivity(i);
            bottomSheetDialog.dismiss();
        });
        empPopupBinding.retakeBtn.setOnClickListener(view -> {
            Intent i = new Intent(context, FaceRegistrationActivity.class);
            i.putExtra("retake", true);
            i.putExtra("empData", gson.toJson(employeeData));
            context.startActivity(i);
            bottomSheetDialog.dismiss();
        });
        bottomSheetDialog.show();
    }

    private void warnDeleteEmp(String epmloyeeid, int position) {

        MaterialAlertDialogBuilder alerter = new MaterialAlertDialogBuilder(context);
        alerter.setTitle("Delete employee")
                .setMessage("Are you sure you want to delete this employee? This action is irreversible")
                .setPositiveButton("Delete", (dialogInterface, i) -> {
                    TastyToast.makeText(context, "Deleting employee", TastyToast.LENGTH_LONG, TastyToast.DEFAULT);
                    deleteEmp(epmloyeeid, position);
                })
                .setNegativeButton("Cancel", (dialogInterface, i) -> {

                }).create().show();
    }

    private void deleteEmp(String employeeid, int position) {

        db.collection("employees").document(employeeid).delete()
                .addOnSuccessListener(unused -> {
                    TastyToast.makeText(context, "Employee deleted", TastyToast.LENGTH_LONG, TastyToast.SUCCESS);
                    deleteAttendance(employeeid);
                    employees.remove(position);
                    notifyItemRemoved(position);
                }).addOnFailureListener(e -> {
                    TastyToast.makeText(context, "Error deleting employee", TastyToast.LENGTH_LONG, TastyToast.ERROR);
                    Log.w(TAG, "Error deleting employee", e);
                });

    }

    private void removeItemAdaper(int position) {
        this.notifyItemRemoved(position);
    }

    private void deleteAttendance(String employeeid) {
        CollectionReference collectionRef = db.collection("attendance");
        Query query = collectionRef.whereEqualTo("employee-id", employeeid);
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                WriteBatch batch = db.batch();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    batch.delete(document.getReference());
                }
                batch.commit().addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        popupViews.toastSuccess("attendance data deleted");
                    } else {
                        // Handle deletion failure
                        popupViews.toastConfused(task.getException().getMessage());
                    }
                });
            } else {
                // Handle query failure
                popupViews.toastConfused("Unable to get attendance data for this employee: " + task.getException().getMessage());
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        EmployeeListItemBinding viewBinding = EmployeeListItemBinding.inflate(LayoutInflater.from(parent.getContext()));
        return new ViewHolder(viewBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String fname = employees.get(position).get("firstname").toString();
        String lname = employees.get(position).get("lastname").toString();
        boolean unregistered = employees.get(position).get("face_data").toString().equals("unregistered");
        holder.viewBinding.peopleEmail.setText(employees.get(position).get("email").toString());
        Glide.with(context).load(imgIconUrl).into(holder.viewBinding.profAvatar);
        if (unregistered){
            holder.viewBinding.peopleName.setText(lname + ", " + fname+"(Face Not registered)");
        }else {
            holder.viewBinding.peopleName.setText(lname + ", " + fname);
        }
        holder.viewBinding.clicker.setOnClickListener(view -> {
            showPopup(employees.get(position), position);
        });

    }

    private void changeTime(Chip chipTime) {
        MaterialTimePicker.Builder materialTimeBuilder = new MaterialTimePicker.Builder();
//        materialTimeBuilder.setMinute(10).setMinute(10).setTimeFormat(TimeFormat.CLOCK_12H);
        materialTimeBuilder.setHour(12);
        MaterialTimePicker materialTimePicker = materialTimeBuilder.build();
        materialTimePicker.addOnPositiveButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int hour = materialTimePicker.getHour();
                int min = materialTimePicker.getMinute();
                String hourStr = String.valueOf(hour);
                String minuteStr = String.valueOf(min);
                String time = "";
                String suffix = (hour < 12) ? "AM" : "PM";
                if (hour < 12) {
                } else {
                    hourStr = String.valueOf((hour == 12) ? 12 : hour - 12);
                }
                if (min < 10) {
                    minuteStr = minuteStr + 0;
                }
                if (hour == 0) {
                    hourStr = String.valueOf(12);
                }
                time = hourStr + ":" + minuteStr + " " + suffix;
                chipTime.setText(time);
                Log.e("pmEndtime", pmEndtime);

                materialTimePicker.removeOnPositiveButtonClickListener(this);
            }
        });
//        materialTimePicker.show(( (Activity) context).getFragmentManager(), "starttime");
        materialTimePicker.show(fragmentManager, "starttime");
    }


    @Override
    public int getItemCount() {
        return employees.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        EmployeeListItemBinding viewBinding;

        public ViewHolder(@NonNull EmployeeListItemBinding appListItemBinding) {
            super(appListItemBinding.getRoot());
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            appListItemBinding.getRoot().setLayoutParams(lp);
            viewBinding = appListItemBinding;
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }
}
