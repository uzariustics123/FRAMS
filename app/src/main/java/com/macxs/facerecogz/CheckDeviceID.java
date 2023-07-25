package com.macxs.facerecogz;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.macxs.facerecogz.databinding.DeviceCheckBinding;

import java.util.HashMap;
import java.util.Map;

public class CheckDeviceID extends AppCompatActivity {
    FirebaseApp firebaseApp;
    DeviceCheckBinding binding;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    String deviceID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DeviceCheckBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        deviceID = DeviceId.getDeviceId(getBaseContext());
        db.collection("devices").whereEqualTo("device-id", deviceID).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult().size() > 0) {
                    Intent intent = new Intent(this, ScanPerson.class);
                    startActivity(intent);
                }else {
                    regDeviceID();
                }
            } else {
binding.statxt.setText("Failed to identify this device please restart the app "+task.getException().getMessage());
            }
        });
    }

    void regDeviceID() {
        Map<String, Object> newDevice = new HashMap<>();
        EditText loced = new EditText(getBaseContext());
        loced.setInputType(InputType.TYPE_CLASS_TEXT);
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
        dialogBuilder.setCancelable(false);
        dialogBuilder.setTitle("New Device")
                .setMessage("this device is not yet registered, please specify the location for this device to be registered")
                .setView(loced)
                .setPositiveButton("Register", (dialogInterface, i) -> {
                    String loc = loced.getText().toString();

                    if (!loc.trim().isEmpty()){
                        newDevice.put("location", loc);
                        newDevice.put("device-id", deviceID);
                        newDevice.put("status", "unverified");
                        newDevice.put("device-name", DeviceId.getDeviceName());
                        newDevice.put("device-model", DeviceId.getDeviceModel());
                        db.collection("devices").add(newDevice).addOnSuccessListener(documentReference -> {
                            Intent intent = new Intent(this, ScanPerson.class);
                            startActivity(intent);
                        }).addOnFailureListener(e -> {
                            binding.statxt.setText("Failed to register device please restart the app: "+e.getMessage());
                            Log.e("error", e.toString());
                            e.printStackTrace();
                        });
                    }

                });
        dialogBuilder.create().show();


    }
}