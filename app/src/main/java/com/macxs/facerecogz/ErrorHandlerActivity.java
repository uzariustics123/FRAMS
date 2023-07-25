package com.macxs.facerecogz;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.macxs.facerecogz.databinding.ActivityErrorHandlerBinding;

public class ErrorHandlerActivity extends AppCompatActivity {
    ActivityErrorHandlerBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityErrorHandlerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Intent intent = getIntent();
        String error = intent.getStringExtra("error");
        binding.errorInfo.setText(error);
    }
}