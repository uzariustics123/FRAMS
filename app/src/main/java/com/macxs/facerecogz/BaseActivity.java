package com.macxs.facerecogz;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;



public class BaseActivity extends AppCompatActivity {
    SharedPreferences sharedprefs;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        sharedprefs = getSharedPreferences("sharedprefs", Activity.MODE_PRIVATE);
//        if (sharedprefs.getBoolean("darkmode", false)) {
//            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
//            changeBarColors(0x00000000, false);
//        } else {
//            changeBarColors(0xFFFFFFFF, false);
//            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
//        }
//        changeBarColors(0xFFFFFFFF, true);
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
    }
    public void changeBarColors(int color, boolean islight) {

        Window window = getWindow();
//        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
//            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
//            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
//            window.setStatusBarColor(color);
//        }
        // getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            View decorView = window.getDecorView();
            WindowInsetsControllerCompat wic = new WindowInsetsControllerCompat(window, decorView);

            wic.setAppearanceLightStatusBars(islight); // true or false as desired.

            // And then you can set any background color to the status bar.
            window.setStatusBarColor(color);
        } else {

        }

        // window.setNavigationBarColor(getColor(R.color.backgroundColor));

    }

}

