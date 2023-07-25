package com.macxs.facerecogz.Utils;

import android.app.Activity;

import com.google.firebase.firestore.FirebaseFirestore;


public class AppFirebaseStorage {
    Activity activity;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    public AppFirebaseStorage(Activity activity){
        this.activity = activity;
    }
}
