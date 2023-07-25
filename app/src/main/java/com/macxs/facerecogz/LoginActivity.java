package com.macxs.facerecogz;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.macxs.facerecogz.Utils.PopupViews;
import com.macxs.facerecogz.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {
    ActivityLoginBinding binding;
    PopupViews popupViews;
    FirebaseApp firebaseApp;
    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        popupViews = new PopupViews(this);
        initFirebases();
        setContentView(binding.getRoot());
        initComponents();
    }

    private void initComponents() {
        binding.signInBtn.setOnClickListener(view -> {
            String email = binding.useremail.getText().toString();
            String pass = binding.pass.getText().toString();
            signup(email, pass);
        });
        binding.forgotBtn.setOnClickListener(view -> {
            Intent intent = new Intent(this, ChangePassActivity.class);
            startActivity(intent);
        });
    }

    public void signup(String email, String pass) {
        if (email.trim().isEmpty() || pass.trim().isEmpty()) {
            popupViews.showError("email and password should'nt be empty");
        } else {
            popupViews.showLoading("Signing up. Please wait...");
            firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
                popupViews.hideLoading();
                if (task.isSuccessful()) {

                } else {
                    popupViews.showError(task.getException().getMessage());
                }
            });
        }
    }

    public void initFirebases() {
        firebaseApp = FirebaseApp.initializeApp(this);
        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseAuth.AuthStateListener authStateListener =
                firebaseAuth -> {
                    if (firebaseAuth.getCurrentUser() != null) {
                        Intent i = new Intent(this, CheckDeviceID.class);
                        startActivity(i);
                        finish();
                    }
                };
        firebaseAuth.addAuthStateListener(authStateListener);
    }

}