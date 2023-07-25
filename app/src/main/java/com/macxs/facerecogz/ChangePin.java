package com.macxs.facerecogz;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.macxs.facerecogz.Utils.PopupViews;
import com.macxs.facerecogz.databinding.ActivityChangePassBinding;
import com.macxs.facerecogz.databinding.ActivityChangePinBinding;

import java.util.HashMap;
import java.util.Map;

public class ChangePin extends AppCompatActivity {
    ActivityChangePinBinding binding;
    private FirebaseApp firebaseApp;
    private FirebaseAuth firebaseAuth;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    PopupViews popper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChangePinBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        popper = new PopupViews(this);
        initFirebases();
        initComponents();
    }

    private void initComponents() {
        binding.closeBtn.setOnClickListener(view -> {
            onBackPressed();
        });
        binding.savePin.setOnClickListener(view -> {
            binding.pinoldtil.setErrorEnabled(false);
            validateOldPin();
        });
    }

    private void validateOldPin() {
        popper.showLoading("Please wait...");
        db.collection("admin-configs").document("admin-configs").get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            String pincode = (String) document.get("pincode");
                            if (!binding.pinold.getText().toString().equals(pincode)) {
                                binding.pinoldtil.setError("Pin code doesn't match");
                                popper.hideLoading();
                            } else {
                                confirmPin();
                            }
                        } else {
                            popper.hideLoading();
                            popper.showError(task.getException().getMessage());
                        }
                    }
                });
    }

    private void confirmPin() {
        binding.pinconftil.setErrorEnabled(false);
        String pinew = binding.pinnew.getText().toString();
        String confpin = binding.pinconf.getText().toString();
        if (pinew.trim().isEmpty() || !pinew.equals(confpin)){
        popper.hideLoading();
        binding.pinconftil.setError("Doesn't match with new pin or new pin is empty");
        }else{
savePin(confpin);
        }
    }

    private void savePin(String confpin) {
        Map<String, Object> data = new HashMap<>();
        data.put("pincode", confpin);
        db.collection("admin-configs").document("admin-configs").update(data).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                popper.hideLoading();
                if (task.isSuccessful()){
                    popper.toastSuccess("Pin code has been changed");
                    binding.savePin.setEnabled(false);
                }else {
                    popper.showError(task.getException().getMessage());
                }
            }
        });
    }

    private void initFirebases() {
        firebaseApp = FirebaseApp.initializeApp(this);
        firebaseAuth = FirebaseAuth.getInstance();
    }
}