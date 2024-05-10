package com.macxs.facerecogz;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.macxs.facerecogz.Utils.FaceGraphic;
import com.macxs.facerecogz.Utils.GraphicOverlay;
import com.macxs.facerecogz.Utils.MyAudioManager;
import com.macxs.facerecogz.Utils.PopupViews;
import com.macxs.facerecogz.databinding.ActivityScanPersonBinding;
import com.macxs.facerecogz.databinding.AdminPincodeInputBinding;
import com.macxs.facerecogz.databinding.AttendancePreviewPersonBinding;
import com.sdsmdg.tastytoast.TastyToast;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import pub.devrel.easypermissions.PermissionRequest;

public class ScanPerson extends BaseActivity {
    ActivityScanPersonBinding binding;
    AttendancePreviewPersonBinding attendancePreviewPersonBinding;
    FaceDetector faceDetector;
    Interpreter tfLite;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    int[] intValues;
    Gson gson = new Gson();
    int inputSize = 112;
    BitmapUtils bitmapUtils;
    String attendanceType;
    int OUTPUT_SIZE = 192;
    boolean isModelQuantized = false;
    boolean camStartCapturing = false;
    boolean flipX = true;
    float[][] embeddings;
    float IMAGE_MEAN = 128.0f;
    float IMAGE_STD = 128.0f;
    float distance = 0.80f;//Recognition Accuracy
    int recogConfirmAttemps = 0;
    int passingAttempts = 5;
    String lastRecogID = "";
    BottomSheetDialog recogPreviewDialog;
    Handler mainHandler;
    Runnable recogRunnable;
    boolean isLegalExit = true;
    Bitmap currentface = null;
    String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};

    String modelFile = "mobile_face_net.tflite";
    EmployeeNoSQLDatabase myLocalDB;
    private ArrayList<Map<String, Object>> registeredEmployees = new ArrayList<>();
    ProcessCameraProvider cameraProvider;
    CameraSelector cameraSelector;
    String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA};
    int cam_face;
    boolean getCloser = false;
    boolean stayStill = false;
    boolean youveBeen = false;
    int RecogPhase = 0;
    PopupViews popupViews;
    FirebaseApp firebaseApp;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    Map<String, Object> recentRecogs = new HashMap<>();
    MyAudioManager myAudioManager;
    GraphicOverlay graphicOverlay;
    Map<String, Object> currentReconizedEmployee;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScanPersonBinding.inflate(getLayoutInflater());
        myLocalDB = new EmployeeNoSQLDatabase(this);
//        registeredEmployees = myLocalDB.getRegisteredEmplyeesList();
        setContentView(binding.getRoot());
        mainHandler = new Handler(getMainLooper());
        recogPreviewDialog = new BottomSheetDialog(this);
        bitmapUtils = new BitmapUtils();
        popupViews = new PopupViews(this);
        myAudioManager = new MyAudioManager(this);
        cam_face = CameraSelector.LENS_FACING_FRONT;
        statusbarConfig();
        initFirebases();
        initComponents();
        //load model

//        DevicePolicyManager dpm =
//                (DevicePolicyManager) this.getSystemService(Context.DEVICE_POLICY_SERVICE);
//        dpm.setLockTaskPackages(adminName, );

        if (EasyPermissions.hasPermissions(this, perms)) {
            // Already have permission, do the thing
            // ...
        } else {
            // Do not have permissions, request them now
            requestPermsNow();
        }

        loadModel();
        //Initialize Face Detector
        initFaceDetector();
        //cam bind

        initConfigs();
    }

    private void initComponents() {
        graphicOverlay = binding.graphicOverlay;
//        binding.boxDrawer.setWillNotDraw(false);
        binding.graphicOverlay.setWillNotDraw(false);
//        binding.boxDrawer.setZOrderOnTop(true);
//        binding.boxDrawer.setZOrderOnTop(true);
        attendancePreviewPersonBinding = AttendancePreviewPersonBinding.inflate(getLayoutInflater());
        recogRunnable = new Runnable() {
            @Override
            public void run() {
                recogPreviewDialog.dismiss();
                camStartCapturing = true;
            }
        };
        binding.adminPanelBtn.setOnClickListener(view -> {
            inputPin();
        });
        binding.adminPanelBtn.setOnLongClickListener(view -> {
            this.stopLockTask();
            finish();
            return false;
        });
        binding.rotateCam.setOnClickListener(v -> {
            if (cam_face == CameraSelector.LENS_FACING_BACK) {
                cam_face = CameraSelector.LENS_FACING_FRONT;
                flipX = true;
            } else {
                cam_face = CameraSelector.LENS_FACING_BACK;
                flipX = false;
            }
            cameraProvider.unbindAll();
            cameraBind();
        });
        checkDeviceID();
        getPassingAttempts();
    }

    private void getPassingAttempts() {
        db.collection("admin-configs").document("admin-configs").get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot data = task.getResult();

                } else {
                    task.getException().printStackTrace();
                }
            }
        });
    }

    private void checkDeviceID() {
        db.collection("devices").whereEqualTo("device-id", DeviceId.getDeviceId(getBaseContext())).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                        String status = document.get("status").toString();
                        if (status.equals("verified")) {
                            cameraBind();
                        } else if (status.equals("unverified")) {
                            statusTextWarn("Device: " + DeviceId.getDeviceId(getBaseContext()) + "\n" +
                                    "status: Unverified\nPlease ask your Administrator for the said issue");
                        } else if (status.equals("maintenance")) {
                            statusTextWarn("Device: " + DeviceId.getDeviceId(getBaseContext()) + "\n" +
                                    "status: Maintenance\nDevice is under maintenance ask your Administrator to resolve the issue");
                        } else {
                            statusTextWarn("Device: " + DeviceId.getDeviceId(getBaseContext()) + "\n" +
                                    "status: " + status + "\nPlease contact your Administrator");
                        }
                    } else {
                        statusTextWarn("Failed to get device registration");
                    }


                });
    }

    private void initFaceDetector() {
        FaceDetectorOptions highAccuracyOpts =
                new FaceDetectorOptions.Builder()
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
//                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .build();
        faceDetector = FaceDetection.getClient(highAccuracyOpts);
    }

    public void initFirebases() {
        firebaseApp = FirebaseApp.initializeApp(this);
        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseAuth.AuthStateListener authStateListener =
                firebaseAuth -> {
                    if (firebaseAuth.getCurrentUser() == null) {
                        Intent i = new Intent(this, LoginActivity.class);
                        startActivity(i);
                        finish();
                    }
                };
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    private void initConfigs() {
        camStartCapturing = false;
        db.collection("admin-configs").document("admin-configs").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                String pincode = (String) document.get("pincode");
                try {
                    passingAttempts = (int) document.get("recog-passing-attempts");
                    distance = (float) document.get("similarity-accuracy");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (pincode != null) {
                    Log.e("pin", pincode);
                    getEmployees();
                } else {
                    camStartCapturing = false;
                    gotoAdminPanel();
                }

            } else {
                camStartCapturing = false;
                gotoAdminPanel();
            }

        });
    }

    private void inputPin() {
        camStartCapturing = false;
        AdminPincodeInputBinding adminPincodeInputBinding = AdminPincodeInputBinding.inflate(getLayoutInflater());
        BottomSheetDialog btmsht = new BottomSheetDialog(this);
        btmsht.setContentView(adminPincodeInputBinding.getRoot());
        setupFullHeightBottomSheet(btmsht, adminPincodeInputBinding.getRoot());
        btmsht.setOnDismissListener(dialogInterface -> {
            camStartCapturing = true;
        });
//        adminPincodeInputBinding.enterbtn.setOnClickListener(view -> {
//            String pin = adminPincodeInputBinding.etOtp.getText().toString();
//            alert.hide();
//            comparePin(pin);
//        });
        btmsht.show();
        adminPincodeInputBinding.etOtp.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.length() == 4) {
                    comparePin(charSequence.toString(), btmsht);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });


    }

    private void comparePin(String pin, BottomSheetDialog btmsht) {
        db.collection("admin-configs").document("admin-configs").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                String pincode = (String) document.get("pincode");
                if (pincode != null) {
                    if (pincode.equals(pin)) {
                        myAudioManager.play(MyAudioManager.dingAudio);
                        btmsht.dismiss();
                        camStartCapturing = false;
                        gotoAdminPanel();
                    } else {
                        TastyToast.makeText(this, "Invalid pin code", TastyToast.LENGTH_LONG, TastyToast.ERROR);
                        myAudioManager.play(MyAudioManager.buzzer);
                    }

                } else {
                    TastyToast.makeText(this, "Something went wrong! please try again", TastyToast.LENGTH_LONG, TastyToast.ERROR);
                    myAudioManager.play(MyAudioManager.buzzer);
                }

            } else {
                TastyToast.makeText(this, "Something went wrong! please try again", TastyToast.LENGTH_LONG, TastyToast.ERROR);
                task.getException().printStackTrace();
                myAudioManager.play(MyAudioManager.buzzer);
            }

        });
    }
//load Ternsorflow ai model to interpret face embeddings or face features data
    private void loadModel() {
        try {
            tfLite = new Interpreter(loadModelFile(this, modelFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Bind camera and preview view
    private void cameraBind() {
//        boundingBoxOverlay.setCameraFacing(cam_face);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this in Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(@NonNull ProcessCameraProvider processCameraProvider) {

        Preview preview = new Preview.Builder().build();
        cameraSelector = new CameraSelector.Builder().requireLensFacing(cam_face).build();
        preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setTargetResolution(new Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        Executor executor = Executors.newSingleThreadExecutor();
        imageAnalysis.setAnalyzer(executor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                try {
                    Thread.sleep(100);  //Camera preview refreshed every 100 millisec(adjust as required)
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                InputImage image = null;
                @SuppressLint("UnsafeOptInUsageError")
                Image mediaImage = imageProxy.getImage();

                if (mediaImage != null) {
                    image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
//                    System.out.println("Rotation "+imageProxy.getImageInfo().getRotationDegrees());
                }
                //Process acquired image to detect faces
                Task<List<Face>> result = faceDetector.process(image).addOnSuccessListener(faces -> {
                    graphicOverlay.clear();
                    if (faces.size() != 0) {

                        Face face = faces.get(0);//Get first face from detected faces
                        //mediaImage to Bitmap
                        Bitmap frame_bmp = bitmapUtils.toBitmap(mediaImage);
//                        Bitmap frame = Bitmap.createBitmap(mediaImage.getWidth(), mediaImage.getHeight(), Bitmap.Config.ARGB_8888);
                        int rot = imageProxy.getImageInfo().getRotationDegrees();
                        //Adjust orientation of Face
                        Bitmap frame_bmp1 = BitmapUtils.rotateBitmap(frame_bmp, rot, false, false);
                        //Get bounding box of face
                        RectF boundingBox = new RectF(face.getBoundingBox());

                        //Crop out bounding box from whole Bitmap(image)
                        Bitmap cropped_face = BitmapUtils.getCropBitmapByCPU(frame_bmp1, boundingBox);
//                        binding.faceSize.setText(String.valueOf());
                        if (flipX)
                            cropped_face = BitmapUtils.rotateBitmap(cropped_face, 0, flipX, false);
                        //Scale the acquired Face to 112*112 which is required input for model
                        Bitmap scaled = BitmapUtils.getResizedBitmap(cropped_face, 112, 112);
                        if (cropped_face.getHeight() > 200) {
                            //accept closest face position from camera
                            if (camStartCapturing) {
                                processFace(face, scaled, frame_bmp1);
                            }

                        } else {
                            statusText("Please get closer");
                            removeFacePreview();
                            if (!getCloser) {
                                myAudioManager.play(MyAudioManager.getCloserAudio);
                                getCloser = true;
                            }
                        }


                    } else {
                        statusText("No face detected");
                        getCloser = false;
                        removeFacePreview();
                    }
                }).addOnFailureListener(e -> {

                }).addOnCompleteListener(task -> {
                    imageProxy.close();
                });

            }

        });
        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis, preview);
    }
//process face and draw graphics to surface view of camera
    private void processFace(Face face, Bitmap scaled, Bitmap frame_bmp1) {
        //check head orientation if looking up or down
        if (face.getHeadEulerAngleX() < -8) {
            statusText("Reduce facing down");
            graphicOverlay.add(new FaceGraphic(graphicOverlay, face, Color.RED, Color.RED, false));
            graphicOverlay.setImageSourceInfo(frame_bmp1.getWidth(), frame_bmp1.getHeight(), flipX);
        } else if (face.getHeadEulerAngleX() > 4) {
            statusText("Reduce facing up too much");
            graphicOverlay.add(new FaceGraphic(graphicOverlay, face, Color.RED, Color.RED, false));
            graphicOverlay.setImageSourceInfo(frame_bmp1.getWidth(), frame_bmp1.getHeight(), flipX);
        }
        // or if facing sideways
        else if (face.getHeadEulerAngleY() < -5) {
            statusText("Try to minimize looking at the right");
            graphicOverlay.add(new FaceGraphic(graphicOverlay, face, Color.RED, Color.RED, false));
            graphicOverlay.setImageSourceInfo(frame_bmp1.getWidth(), frame_bmp1.getHeight(), flipX);
        } else if (face.getHeadEulerAngleY() > 5) {
            statusText("Try to minimize looking at the left");
            graphicOverlay.add(new FaceGraphic(graphicOverlay, face, Color.RED, Color.RED, false));
            graphicOverlay.setImageSourceInfo(frame_bmp1.getWidth(), frame_bmp1.getHeight(), flipX);
        } else {// finally accept face with the best orientation and position
            graphicOverlay.add(new FaceGraphic(graphicOverlay, face, Color.GREEN, Color.GREEN, false));
            graphicOverlay.setImageSourceInfo(frame_bmp1.getWidth(), frame_bmp1.getHeight(), flipX);
            recognizeImage(scaled, face); //Send scaled bitmap to create face data.
        }

    }

    private void previewPersonImage(Bitmap scaled) {
        binding.prevLayout.setVisibility(View.VISIBLE);
        binding.facePreview.setImageBitmap(scaled);
    }

    private void removeFacePreview() {
        binding.prevLayout.setVisibility(View.INVISIBLE);
        binding.facePreview.setImageDrawable(null);
    }

    void statusText(String msg) {
        binding.captureInfo.setText(msg);
        binding.captureInfo.setTextColor(Color.WHITE);
    }

    void statusTextWarn(String msg) {
        binding.captureInfo.setText(msg);
        binding.captureInfo.setTextColor(Color.RED);
    }

    private void recognizeImage(Bitmap imageBitmap, Face face) {
        previewPersonImage(imageBitmap);
        float[] facedata = getFaceDataFromBitmap(imageBitmap)[0];
        String currentRecogID = "";
        List<Map<String, Object>> closesMatches = getClosestMatches(facedata);

        if (closesMatches.size() > 0 && RecogPhase == 0) {
            currentRecogID = closesMatches.get(0).get("employee-id").toString();
                if (recogConfirmAttemps >= 3) {
                    recogConfirmAttemps = 0;
                    currentface = imageBitmap;
//                    checkAttendance(closesMatches.get(0));
                    currentReconizedEmployee = closesMatches.get(0);
                    RecogPhase = 1;
                } else if (currentRecogID.equals(lastRecogID)) {
                    recogConfirmAttemps++;
                    statusText("Stay still");
                    if (!stayStill) {
                        myAudioManager.play(MyAudioManager.staystillAudio);
                        stayStill = true;
                    }
                } else {
                    recogConfirmAttemps = 0;
                    getCloser = false;
                    stayStill = false;
                    youveBeen = false;
                }
                lastRecogID = currentRecogID;



        } else if (RecogPhase == 1) {
            if (face.getSmilingProbability() != null && face.getSmilingProbability() > 0.70) {
                if (closesMatches.size() > 0){
                    if (closesMatches.get(0).get("employee-id").toString().equals(lastRecogID)){
                        camStartCapturing = false;
                        currentRecogID = closesMatches.get(0).get("employee-id").toString();
                        checkAttendance(closesMatches.get(0));
                    }else {
                        currentRecogID = closesMatches.get(0).get("employee-id").toString();
                        popupViews.toastError("Mismatched! try again with a your sweet smile");
                        camStartCapturing = true;
                        RecogPhase = 0;
                    }
                }else{
                    statusText("make a good a smile");
                }

//                cameraProvider.unbindAll();
            } else {
                statusText("Take a moment to smile");
            }
//            RecogPhase = 2;


        } else if (RecogPhase == 2) {
            RecogPhase = 3;
        } else if (RecogPhase == 3) {

        } else {
            statusText("Unknown");
            recogConfirmAttemps = 0;
            RecogPhase = 0;
            currentReconizedEmployee = new HashMap<>();
        }


    }

    private void checkAttendance(Map<String, Object> employeeData) {
        camStartCapturing = false;
        if (isFromRecents(employeeData.get("employee-id").toString())) {
            TastyToast.makeText(this, "You've been recently recognized", TastyToast.LENGTH_LONG, TastyToast.WARNING);
            if (!youveBeen) {
                myAudioManager.play(MyAudioManager.youvebeenAudio);
                youveBeen = true;
            }

            camStartCapturing = true;
        } else {
            initAttendance(employeeData);
//            TastyToast.makeText(this, "Please wait", TastyToast.LENGTH_LONG, TastyToast.WARNING);
        }
        mainHandler.removeCallbacks(recogRunnable);
    }

    private void initAttendance(Map<String, Object> employeeData) {
        popupViews.showFaceLoading("Analyzing...");
        Map<String, Object> newAttendance = new HashMap<>();
        CollectionReference attendanceRef = db.collection("attendance");
        Query query = attendanceRef.orderBy("time", Query.Direction.DESCENDING);
        newAttendance.put("employee-id", employeeData.get("employee-id"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a");
        {
            LocalDateTime localDateTime = LocalDateTime.now();
            newAttendance.put("month", localDateTime.getMonthValue());
            newAttendance.put("day", localDateTime.getDayOfMonth());
            newAttendance.put("year", localDateTime.getYear());
            String formattedTime = localDateTime.format(formatter);
            newAttendance.put("time", formattedTime);
        }
        String defInTimeAMstr  = String.valueOf(employeeData.get("am-in-time"));
        String defOutTimeAMstr  = String.valueOf(employeeData.get("am-out-time"));
        String defInTimePMstr  = String.valueOf(employeeData.get("pm-in-time"));
        String defOutTimePMstr  = String.valueOf(employeeData.get("pm-out-time"));

        LocalTime defInTimeAM = LocalTime.parse(defInTimeAMstr, formatter);
        LocalTime defOutTimeAM = LocalTime.parse(defOutTimeAMstr, formatter);
        LocalTime defInTimePM = LocalTime.parse(defInTimePMstr, formatter);
        LocalTime defOutTimePM = LocalTime.parse(defOutTimePMstr, formatter);
        db.collection("attendance")
                .whereEqualTo("employee-id", employeeData.get("employee-id"))
                .whereEqualTo("month", newAttendance.get("month"))
                .whereEqualTo("year", newAttendance.get("year"))
                .whereEqualTo("day", newAttendance.get("day")).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean arriveAM = false;
                        boolean departAM = false;
                        boolean arrivePM = false;
                        boolean departPM = false;
                        LocalDateTime localDateTime = LocalDateTime.now();
                        LocalTime localTimeNow = LocalTime.now();
//                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a");
                        LocalTime amEndTime = LocalTime.parse("11:59 AM", formatter);
                        LocalTime pmStartTime = LocalTime.parse("1:00 PM", formatter);
                        String formattedTime = localDateTime.format(formatter);
                        if (task.getResult().size() % 2 == 0) {
                            attendanceType = "arrival";
                        } else {
                            attendanceType = "departure";
                        }
//                        Log.e("task size", String.valueOf(task.getResult().size()));
//                        Log.e("task modulo", String.valueOf(task.getResult().size() % 2));
                        for (QueryDocumentSnapshot document : task.getResult()) {
//                            Log.d("asdw", document.getId() + " => " + document.getData());
                            Map<String, Object> attendance = document.getData();
                            LocalTime lastTimeAtt = LocalTime.parse(String.valueOf(attendance.get("time")), formatter);
                            String lastAttType = document.get("attendance-type").toString();
                            attendance.put("employee-id", document.getId());
                            if (lastAttType.equals("am-in")) {//handle am arrival
                                arriveAM = true;
                            }  else if (lastAttType.equals("am-out")) {//handle am late and early out
                                departAM = true;
                            } else if (lastAttType.equals("pm-in")) {//handle pm in
                                arrivePM = true;
                            } else if (lastAttType.equals("pm-out")) {//handle pm early out
                                departPM = true;
                            }
                            Log.e( formattedTime+" is before", defInTimePMstr );
//                                Log.e("time", attendance.get("time").toString());
//                                Log.e("type", lastAttType);
//                                Log.e("results", String.valueOf(task.getResult().size()));
                        }


                        if (departPM) {
                            TastyToast.makeText(getBaseContext(), "you already took your attendance for today.", TastyToast.LENGTH_LONG, TastyToast.CONFUSING);
                            popupViews.hideFaceLoading();
                            myAudioManager.play(MyAudioManager.youalreadyAudio);
                            mainHandler.postDelayed(recogRunnable, 4000);
                        } else if (localTimeNow.isBefore(defOutTimeAM) && departAM) {
                            TastyToast.makeText(getBaseContext(), "you already took your attendance for this Morning.", TastyToast.LENGTH_LONG, TastyToast.ERROR);
                            popupViews.hideFaceLoading();
                            mainHandler.postDelayed(recogRunnable, 4000);
                        } else {
                            //am in
                            if (localTimeNow.isBefore(defOutTimeAM.minusHours(1))){
                                if (!arriveAM) {
                                    attendanceType = "arrival";
                                    newAttendance.put("attendance-type", "am-in");
                                }
                                 else if(!departAM) {//am early out
                                    newAttendance.put("attendance-type", "am-out");
                                    attendanceType = "departure";
                                }
                            }else if (localTimeNow.isAfter(defOutTimeAM.minusHours(1)) && localTimeNow.isBefore(defInTimePM) && !departAM) {//am late out
                                    newAttendance.put("attendance-type", "am-out");
                                    attendanceType = "departure";
                            }
//                            handle afternoon time
                            else if (localTimeNow.isBefore(defOutTimePM.minusHours(1))){
                                if (!arrivePM) {
                                    newAttendance.put("attendance-type", "pm-in");
                                    attendanceType = "arrival";
                                }else if(!departPM) {
                                    newAttendance.put("attendance-type", "pm-out");
                                    attendanceType = "departure";
                                }
                            }
//                            late outs pm
                            else if(localTimeNow.isAfter(defOutTimePM.minusHours(1)) && !departPM){
                                    newAttendance.put("attendance-type", "pm-out");
                                    attendanceType = "departure";
                            }
                            saveAttendance(newAttendance, attendanceType, employeeData);
                        }
                    } else {
                        popupViews.hideFaceLoading();
                        popupViews.showError(task.getException().getMessage());

                    }
                });


//        TastyToast.makeText(this, "Faces " + dateTime, TastyToast.LENGTH_LONG, TastyToast.DEFAULT);
    }

    private void saveAttendance(Map<String, Object> attendanceData, String type, Map<String, Object> employeedata) {
        attendanceData.put("type", type);
        db.collection("attendance").add(attendanceData).addOnSuccessListener(documentReference -> {
            popupViews.hideFaceLoading();
            addRecentlyRecognized(employeedata.get("employee-id").toString());
            attendanceSuccess(employeedata);
            myAudioManager.play(MyAudioManager.dingAudio);
//documentReference.g
        }).addOnFailureListener(e -> {
            popupViews.showError("Failed to get your attendance\nreason:" + e.getMessage());
        });
    }

    private boolean isFromRecents(String employee_id) {
        if (recentRecogs.containsKey(employee_id)) {
            return true;
        }
        return false;
    }

    private void addRecentlyRecognized(String employee_id) {
        recentRecogs.put(employee_id, employee_id);
        new Handler(getMainLooper()).postDelayed((Runnable) () -> {
            recentRecogs.remove(employee_id);
        }, 120000);
    }

    private void attendanceSuccess(Map employeeData) {
        recogPreviewDialog.setContentView(attendancePreviewPersonBinding.getRoot());
        attendancePreviewPersonBinding.attType.setText(attendanceType);
        attendancePreviewPersonBinding.imgPreview.setImageBitmap(currentface);
        attendancePreviewPersonBinding.empName.setText(employeeData.get("firstname").toString() + " " + employeeData.get("lastname").toString());
        recogPreviewDialog.show();
        mainHandler.postDelayed(recogRunnable, 4000);

    }

    private void gotoAdminPanel() {
        try {
            popupViews.hideLoading();

        } catch (Exception e) {

        }
//        isLegalExit = true;
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }


    public void getEmployees() {
        popupViews.showFaceLoading("Syncing registered faces...");
        db.collection("employees").get().addOnCompleteListener(task -> {
            popupViews.hideFaceLoading();
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Map employee = document.getData();
                    employee.put("employee-id", document.getId());
                    if (employee.get("face_data").toString().equals("unregistered")){

                    }else{
                        formatEmployeeFaceData(employee, "face_data");
                        formatEmployeeFaceData(employee, "smile_face_data");
                        registeredEmployees.add(employee);
                    }


                }
                camStartCapturing = true;
                TastyToast.makeText(this, "Faces synced", TastyToast.LENGTH_LONG, TastyToast.DEFAULT);
            } else {
                camStartCapturing = false;
                TastyToast.makeText(this, "error getting employees", TastyToast.LENGTH_LONG, TastyToast.CONFUSING);
                Log.d("Error getting documents: ", task.getException().toString());
            }
        });
    }

    public List<Map<String, Object>> getClosestMatches(float[] facedata) {
        List<Map<String, Object>> closestMatches = new ArrayList<>();
        Map<String, Object> closestMatch = new HashMap<>();
        HashMap<String, Object> secondClosestMatch = new HashMap<>();

        for (Map<String, Object> eachEmployee : registeredEmployees) {
            final float[] knownFace = ((float[][]) (RecogPhase == 1? eachEmployee.get("smile_face_data"): eachEmployee.get("face_data")))[0];
            float face_distance = 0;
            for (int i = 0; i < facedata.length; i++) {
                float diff = facedata[i] - knownFace[i];
                face_distance += diff * diff;
            }
            face_distance = (float) Math.sqrt(face_distance);
            if (face_distance < distance) {
                closestMatch = eachEmployee;
                closestMatch.put("distance", face_distance);
                closestMatches.add(closestMatch);
            }
        }
        sortClosestToFarthest(closestMatches);
        return closestMatches;
    }

    public void sortClosestToFarthest(List<Map<String, Object>> closest) {
        Collections.sort(closest, new Comparator<Map<String, Object>>() {

            @Override
            public int compare(Map<String, Object> stringObjectMap, Map<String, Object> t1) {
                return Float.compare((Float) stringObjectMap.get("distance"), (Float) t1.get("distance"));
            }
        });
        Log.e("sorted", closest.toString());
    }

    public float[][] getFaceDataFromBitmap(Bitmap bitmap) {
        //Create ByteBuffer to store normalized image

        ByteBuffer imgData = ByteBuffer.allocateDirect(inputSize * inputSize * 3 * 4);

        imgData.order(ByteOrder.nativeOrder());

        intValues = new int[inputSize * inputSize];

        //get pixel values from Bitmap to normalize
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        imgData.rewind();

        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                int pixelValue = intValues[i * inputSize + j];
                if (isModelQuantized) {
                    // Quantized model
                    imgData.put((byte) ((pixelValue >> 16) & 0xFF));
                    imgData.put((byte) ((pixelValue >> 8) & 0xFF));
                    imgData.put((byte) (pixelValue & 0xFF));
                } else { // Float model
                    imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);

                }
            }
        }
        //imgData is input to our model
        Object[] inputArray = {imgData};

        Map<Integer, Object> outputMap = new HashMap<>();


        embeddings = new float[1][OUTPUT_SIZE]; //output of model will be stored in this variable

        outputMap.put(0, embeddings);

        tfLite.runForMultipleInputsOutputs(inputArray, outputMap); //Run model
        return embeddings;
    }

    private void requestPermsNow() {
        EasyPermissions.requestPermissions(
                new PermissionRequest.Builder(this, 101, perms)
                        .setRationale("Grant storage access permision in order for the app to work as it is intended.")
                        .setPositiveButtonText("OK")
                        .setNegativeButtonText("Cancel")
                        .build());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @AfterPermissionGranted(101)
    private void methodRequiresTwoPermission() {

        if (EasyPermissions.hasPermissions(this, perms)) {
            // Already have permission, do the thing
            // ...
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, "Needs storage access permision in order for the app to work as it is intended.",
                    101, perms);
        }
    }

    private void statusbarConfig() {
        Window w = getWindow();
        w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

    }

    private MappedByteBuffer loadModelFile(Activity activity, String MODEL_FILE) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public void formatEmployeeFaceData(Map<String, Object> employeeData, String key) {
//        String facedataStr = (String) employeeData.get(key);
//        TypeToken<ArrayList<Object>> token = new TypeToken<ArrayList<Object>>() {};
        String facedataStr = (String) employeeData.get(key);
        float[][] output = new float[1][OUTPUT_SIZE];

        List<?> arrayList = (List<?>) gson.fromJson(facedataStr, List.class);
        List<?> innerList = (List<?>) arrayList.get(0);

        innerList.forEach(item -> {
            if (item instanceof Double) {
                output[0][(int) innerList.indexOf(item)] = ((Double) item).floatValue();
            }
        });

        System.out.println("Entry output "+ employeeData.get("firstname") + " " + Arrays.deepToString(output));
        employeeData.put(key, output);
    }

    private void setupFullHeightBottomSheet(BottomSheetDialog bottomSheetDialog, View contentView) {
        FrameLayout bottomSheet = (FrameLayout) contentView.getParent();

        BottomSheetBehavior behavior = BottomSheetBehavior.from(bottomSheet);
        ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        if (layoutParams != null) {
            layoutParams.height = height;
        }
        bottomSheet.setLayoutParams(layoutParams);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSheetDialog.setOnShowListener(dialog -> {
            bottomSheet.setBackgroundColor(Color.TRANSPARENT);
        });
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
    }

    @Override
    protected void onStop() {
        super.onStop();
//        if (!isLegalExit){
//            Intent intent = new Intent(this, this.getClass());
//            startActivity(intent);
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        myAudioManager.release();
    }
}
