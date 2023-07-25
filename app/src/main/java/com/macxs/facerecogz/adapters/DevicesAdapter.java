package com.macxs.facerecogz.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.chip.Chip;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.macxs.facerecogz.R;
import com.macxs.facerecogz.Utils.PopupViews;
import com.macxs.facerecogz.databinding.DeviceListItemBinding;
import com.macxs.facerecogz.databinding.EmployeeListItemBinding;

import java.util.ArrayList;
import java.util.Map;

public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.ViewHolder> {

    Context context;
    ArrayList<Map<String, Object>> devices;
    FirebaseApp firebaseApp;
    FirebaseAuth firebaseAuth;
    PopupViews popupViews;
    Gson gson = new Gson();
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    public DevicesAdapter(Activity context, ArrayList<Map<String, Object>> devices) {
        this.devices = devices;
        this.context = context;
        popupViews = new PopupViews(context);
        initFirebases();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        DeviceListItemBinding viewBinding = DeviceListItemBinding.inflate(LayoutInflater.from(parent.getContext()));
        return new ViewHolder(viewBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull DevicesAdapter.ViewHolder holder, int position) {
        String devID = devices.get(position).get("device-id").toString();
        String devModel = devices.get(position).get("device-model").toString();
        String devName = devices.get(position).get("device-name").toString();
        String devStatus = devices.get(position).get("status").toString();
        String devDocID = devices.get(position).get("document-id").toString();
        String devLoc = devices.get(position).get("location").toString();
        holder.viewBinding.devLoc.setText(devLoc);
        holder.viewBinding.devId.setText("device id: "+devID);
        holder.viewBinding.devName.setText("device name: "+devName);
        holder.viewBinding.devModel.setText("device model: "+devModel);
        if (devStatus.equals("verified")){
            holder.viewBinding.devImg.setImageResource(R.drawable.ic_devices);
            holder.viewBinding.verifyChip.setVisibility(View.GONE);
        }else{
            holder.viewBinding.devImg.setImageResource(R.drawable.ic_device_unknown);
            holder.viewBinding.verifyChip.setVisibility(View.VISIBLE);
            holder.viewBinding.verifyChip.setOnClickListener(view -> {
                Map updatedDev = devices.get(position);
                updatedDev.put("status", "verified");
            verifyDevice(holder.viewBinding.verifyChip,holder.viewBinding.devImg, devDocID, updatedDev);
            });
        }
    }

    private void verifyDevice(Chip verifyChip, ImageView imgicon, String docID, Map updatedDevData) {
        db.collection("devices").document(docID).update(updatedDevData).addOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {
                if (task.isSuccessful()){
                    popupViews.toastSuccess("device verified");
                    verifyChip.setVisibility(View.GONE);
                    imgicon.setImageResource(R.drawable.ic_devices);
                }else{
                    popupViews.showError(task.getException().getMessage());
                }
            }
        });
    }


    @Override
    public int getItemCount() {
        return devices.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        DeviceListItemBinding viewBinding;

        public ViewHolder(@NonNull DeviceListItemBinding deviceListItemBinding) {
            super(deviceListItemBinding.getRoot());
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            deviceListItemBinding.getRoot().setLayoutParams(lp);
            viewBinding = deviceListItemBinding;
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }

    public void initFirebases() {
        firebaseApp = FirebaseApp.initializeApp(context);
        firebaseAuth = FirebaseAuth.getInstance();
    }
}
