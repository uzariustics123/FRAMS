package com.macxs.facerecogz.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.macxs.facerecogz.Utils.PopupViews;
import com.macxs.facerecogz.adapters.DevicesAdapter;
import com.macxs.facerecogz.adapters.EmployeeListadapter;
import com.macxs.facerecogz.databinding.DevicesFragmentBinding;

import java.util.ArrayList;
import java.util.Map;

public class DevicesFragment extends Fragment {
    DevicesFragmentBinding binding;
    PopupViews popupViews;
    FirebaseApp firebaseApp;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    ArrayList<Map<String, Object>> devices = new ArrayList<>();
    DevicesAdapter devicesAdapter;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DevicesFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void initFirebases() {
        firebaseApp = FirebaseApp.initializeApp(getActivity());
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        popupViews = new PopupViews(getActivity());
        initFirebases();
        getDevices();
        binding.refreshList.setOnRefreshListener(() -> {
            getDevices();
            binding.refreshList.setRefreshing(false);
        });
    }

    private void getDevices() {
        popupViews.showLoading("Getting list of devices...");
        binding.devicesErrorLayout.setVisibility(View.GONE);
        db.collection("devices").get().addOnCompleteListener(task -> {
            popupViews.hideLoading();
            if (task.isSuccessful()) {
                devices.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Log.d("asdw", document.getId() + " => " + document.getData());
                    Map device = document.getData();
                    device.put("document-id", document.getId());
                    devices.add(device);
                }
                Log.e("totaldevice", String.valueOf(devices.size()));
            } else {
                Log.d("Error getting documents: ", task.getException().toString());
            }
            showDevicesInList();
        });
    }

    public void showDevicesInList() {
        if (!devices.isEmpty()) {
            binding.deviceList.setVisibility(View.VISIBLE);
            binding.devicesErrorLayout.setVisibility(View.GONE);

            devicesAdapter = new DevicesAdapter(getActivity(), devices);
            binding.deviceList.setAdapter(devicesAdapter);
            binding.deviceList.setLayoutManager(new LinearLayoutManager(getActivity()));
            binding.deviceList.scheduleLayoutAnimation();
        } else {
            binding.deviceList.setVisibility(View.GONE);
            binding.devicesErrorLayout.setVisibility(View.VISIBLE);
        }
    }
}
