package com.macxs.facerecogz;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.macxs.facerecogz.Utils.PopupViews;
import com.macxs.facerecogz.databinding.ActivityChangePassBinding;

public class ChangePassActivity extends AppCompatActivity {
    ActivityChangePassBinding binding;
    private FirebaseApp firebaseApp;
    private FirebaseAuth firebaseAuth;
    PopupViews popper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChangePassBinding.inflate(getLayoutInflater());
        popper = new PopupViews(this);
        setContentView(binding.getRoot());
        initFirebases();
        iniComponents();

    }

    private void iniComponents() {
        binding.closeBtn.setOnClickListener(view -> {
            onBackPressed();
        });
        binding.sendChangeLink.setOnClickListener(view -> {
            if (binding.emailLink.getText().toString().trim().isEmpty()){
                popper.toastConfused("email is empty");
            }else{
                popper.showLoading("Please wait...");
                String email = binding.emailLink.getText().toString();
                firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                    popper.hideLoading();
                    if (task.isSuccessful()){
                        popper.toastSuccess("A request link has been sent to your email "+ email + ", please check your inbox");
                    }else {
                        popper.showError(task.getException().getMessage());
                    }
                });
            }
        });
    }
    public void initFirebases() {
        firebaseApp = FirebaseApp.initializeApp(this);
        firebaseAuth = FirebaseAuth.getInstance();
        
    }
}