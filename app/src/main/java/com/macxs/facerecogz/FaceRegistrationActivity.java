package com.macxs.facerecogz;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import android.media.Image;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.macxs.facerecogz.Utils.FaceGraphic;
import com.macxs.facerecogz.Utils.GraphicOverlay;
import com.macxs.facerecogz.Utils.MyAudioManager;
import com.macxs.facerecogz.Utils.PopupViews;
import com.macxs.facerecogz.databinding.ActivityFaceRegistrationBinding;
import com.macxs.facerecogz.databinding.NewFaceRegisterBinding;
import com.sdsmdg.tastytoast.TastyToast;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import pub.devrel.easypermissions.EasyPermissions;
import pub.devrel.easypermissions.PermissionRequest;

public class FaceRegistrationActivity extends BaseActivity {
    ActivityFaceRegistrationBinding binding;
    FaceDetector faceDetector;
    Interpreter tfLite;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    int[] intValues;
    int inputSize = 112;
    BitmapUtils bitmapUtils;
    int OUTPUT_SIZE = 192;
    boolean isModelQuantized = false;
    boolean isRetakeReg = false;
    boolean camStartCapturing = true;
    boolean flipX = true;
    float[][] embeddings;
    float[][] smile_embeddings;
    float IMAGE_MEAN = 128.0f;
    float IMAGE_STD = 128.0f;
    float distance = 1.0f;
    MyAudioManager myAudioManager;
    Gson gson = new Gson();
    FirebaseApp firebaseApp;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    String modelFile = "mobile_face_net.tflite";
    //    private HashMap<String, Recognition.Employee> registeredEmployees = new HashMap<>();
    ProcessCameraProvider cameraProvider;
    CameraSelector cameraSelector;
    String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA};
    int cam_face = CameraSelector.LENS_FACING_FRONT;
    BottomSheetDialog registerBottomSheetDialog;
    NewFaceRegisterBinding newFaceRegisterBinding;
    Bitmap facePreviewImg = null;
    Map<String, Object> newEmployeeData = new HashMap<>();
    PopupViews popupViews;
    boolean getCloser = false;
    GraphicOverlay graphicOverlay;
    Map<String, Object> upEmpData = new HashMap<>();
    boolean regReady = false;
    private int RecogPhase = 0;
    String amstarttime = "8:00 AM";
    String pmstarttime = "1:00 PM";
    String pmEndtime = "5:00 PM";
    String amEndtime = "12:00 PM";
    final String fixamstarttime = "8:00 AM";
    final String fixpmstarttime = "1:00 PM";
    final String fixpmEndtime = "5:00 PM";
    final String fixamEndtime = "12:00 PM";
    DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .appendPattern("h:mm a")
            .optionalStart()
            .appendPattern("hh:mm a")
            .optionalEnd()
            .toFormatter();
    LocalTime defamEndTime = LocalTime.parse("11:59 AM", formatter);
    LocalTime defpmEndTime = LocalTime.parse("5:00 PM", formatter);
    LocalTime defamStartTime = LocalTime.parse("8:00 AM", formatter);
    LocalTime defpmStartTime = LocalTime.parse("1:00 PM", formatter);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFaceRegistrationBinding.inflate(getLayoutInflater());
        newFaceRegisterBinding = NewFaceRegisterBinding.inflate(getLayoutInflater());
        initFirebases();
        setContentView(binding.getRoot());
        popupViews = new PopupViews(this);
        initComponents();
        myAudioManager = new MyAudioManager(this);
        registerBottomSheetDialog = new BottomSheetDialog(this);
        bitmapUtils = new BitmapUtils();
        statusbarConfig();
        if (EasyPermissions.hasPermissions(this, perms)) {
            Toast.makeText(getApplicationContext(), "has perms", Toast.LENGTH_SHORT).show();
        } else {
            // Do not have permissions, request them now
            requestPermsNow();
        }
        loadModel();
        //Initialize Face Detector
        initFaceDetector();
        //cam bind
        cameraBind();
        binding.regbtn.setOnClickListener(view -> {
            if (isRetakeReg) {
                retakeFaceReg();
            } else {
                registerNewFace();
            }

        });
//        getEmployeeData();

    }

    private void retakeFaceReg() {
        if (RecogPhase == 0) {
            RecogPhase = 1;
            popupViews.toastSuccess("Normal face saved. Do it again for a smiling face");
            return;
        }
        String facedata = gson.toJson(embeddings);
        String smilefacedata = gson.toJson(embeddings);
        upEmpData.put("face_data", facedata);
        upEmpData.put("smile_face_data", smilefacedata);
        updateEmp(upEmpData);
    }

    private void initComponents() {
        isRetakeReg = getIntent().getBooleanExtra("retake", false);
        TypeToken<Map<String, Object>> token = new TypeToken<Map<String, Object>>() {
        };
        upEmpData = gson.fromJson(getIntent().getStringExtra("empData"), Map.class);
        if (isRetakeReg) {
            binding.regbtn.setText("Update");
        }
        graphicOverlay = binding.graphicOverlay;
    }

    private void getEmployeeData() {
        String jsondata = getIntent().getStringExtra("employee-data");
        if (jsondata.trim().isEmpty()) {
            Toast.makeText(this, "Cannot parse employee data", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            newEmployeeData = gson.fromJson(jsondata, Map.class);
            Log.d("employee-datae", newEmployeeData.toString());
        }
    }

    private void registerNewFace() {
        if (RecogPhase == 0) {
            popupViews.toastSuccess("Now register again with a smiling face");
            RecogPhase = 1;
            return;
        }
        cameraProvider.unbindAll();
        registerBottomSheetDialog.setContentView(newFaceRegisterBinding.getRoot());
        setupFullHeightBottomSheet(registerBottomSheetDialog, newFaceRegisterBinding.getRoot());
        registerBottomSheetDialog.show();
        newFaceRegisterBinding.facePreviewImg.setImageBitmap(facePreviewImg);
        newFaceRegisterBinding.mornChipIn.setOnClickListener(view -> {
            timeStartAM();
        });
        newFaceRegisterBinding.noonChipIn.setOnClickListener(view -> {
            timeStartPM();
        });
        newFaceRegisterBinding.noonChipOut.setOnClickListener(view -> {
            timeEndPM();
        });
        newFaceRegisterBinding.mornChipOut.setOnClickListener(view -> {
            timeEndAM();
        });
        newFaceRegisterBinding.conBtn.setOnClickListener(view -> {
            LocalTime amInTime = LocalTime.parse(amstarttime, formatter);
            LocalTime pmInTime = LocalTime.parse(pmstarttime, formatter);
            LocalTime amOutime = LocalTime.parse(amEndtime, formatter);
            LocalTime pmOutime = LocalTime.parse(pmEndtime, formatter);
            if (amInTime.isAfter(amOutime)){
                popupViews.toastWarn("In time for morning cannot be after the out time");
                return;
            } else if (amOutime.isAfter(pmInTime)) {
                popupViews.toastWarn("Out time for morning cannot be after the in time for the afternoon");
                return;
            } else if (pmInTime.isAfter(pmOutime)) {
                popupViews.toastWarn("In time for afternoon cannot be after the out time for the afternoon");
                return;
            }else if (amOutime.isAfter(defpmStartTime)) {
                popupViews.toastWarn("Morning out attendance should not be assigned after the in attendance for afternoon");
                return;
            }else if (pmInTime.isAfter(defpmEndTime)) {
                popupViews.toastWarn("Afternoon attendance should not be assigned in the morning time");
                return;
            }

            HashMap<String, Object> employee = new HashMap<>();
            String fname = newFaceRegisterBinding.Fname.getText().toString();
            String lname = newFaceRegisterBinding.lname.getText().toString();
            String mname = newFaceRegisterBinding.midname.getText().toString();
            String email = newFaceRegisterBinding.email.getText().toString();
            employee.put("firstname", fname);
            employee.put("middlename", mname);
            employee.put("lastname", lname);
            employee.put("email", email);
            employee.put("am-in-time", amstarttime);
            employee.put("am-out-time",amEndtime);
            employee.put("pm-in-time", pmstarttime);
            employee.put("pm-out-time", pmEndtime);
            String facedata = gson.toJson(embeddings);
            String smilefacedata = gson.toJson(smile_embeddings);
            employee.put("face_data", facedata);
            employee.put("smile_face_data", smilefacedata);
//            newFaceRegisterBinding.employeeDatatxt.setText(empInfo);
            saveNewEmployee(employee);
            registerBottomSheetDialog.dismiss();
            popupViews.showFaceLoading("Registering new employee...");
//            camStartCapturing = true;
        });
        newFaceRegisterBinding.btClose.setOnClickListener(view -> {
            registerBottomSheetDialog.dismiss();
            RecogPhase = 0;
            amstarttime = fixamstarttime;
            amEndtime = fixamEndtime;
            pmstarttime = fixpmstarttime;
            pmEndtime = fixpmEndtime;
            cameraBind();
        });
        registerBottomSheetDialog.setCancelable(false);
    }


    private void initFaceDetector() {
        FaceDetectorOptions highAccuracyOpts =
                new FaceDetectorOptions.Builder()
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .build();
        faceDetector = FaceDetection.getClient(highAccuracyOpts);
        binding.backBtn.setOnClickListener(view -> {
            onBackPressed();
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
    }

    private void cameraBind() {
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
        preview.setSurfaceProvider(binding.previewCamView.getSurfaceProvider());
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setTargetResolution(new Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        Executor executor = Executors.newSingleThreadExecutor();
        imageAnalysis.setAnalyzer(executor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                try {
                    Thread.sleep(0);  //Camera preview refreshed every 10 millisec(adjust as required)
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
                        int rot = imageProxy.getImageInfo().getRotationDegrees();
                        //Adjust orientation of Face
                        Bitmap frame_bmp1 = BitmapUtils.rotateBitmap(frame_bmp, rot, false, false);
                        //Get bounding box of face
                        RectF boundingBox = new RectF(face.getBoundingBox());
                        //Crop out bounding box from whole Bitmap(image)
                        Bitmap cropped_face = BitmapUtils.getCropBitmapByCPU(frame_bmp1, boundingBox);
                        if (flipX)
                            cropped_face = BitmapUtils.rotateBitmap(cropped_face, 0, flipX, false);
                        //Scale the acquired Face to 112*112 which is required input for model
                        Bitmap scaled = BitmapUtils.getResizedBitmap(cropped_face, 112, 112);
                        if (cropped_face.getHeight() > 200) {
                            if (camStartCapturing) {
                                processFace(face, scaled, frame_bmp1);
                            }
                        } else {
                            inform("Please get closer");
                            if (!getCloser && !registerBottomSheetDialog.isShowing()) {
                                myAudioManager.play(MyAudioManager.getCloserAudio);
                                getCloser = true;
                                hideReg();
                            }
                        }

                    } else {
                        inform("No face detected");
                        hideReg();
                    }
                }).addOnFailureListener(e -> {

                }).addOnCompleteListener(task -> {
                    imageProxy.close();
                });

            }

        });
        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis, preview);
    }

    void hideReg() {
        binding.regbtn.setVisibility(View.INVISIBLE);
        binding.imageCard.setVisibility(View.INVISIBLE);

    }

    void showReg() {
        binding.regbtn.setVisibility(View.VISIBLE);
        binding.imageCard.setVisibility(View.VISIBLE);

    }

    private void processFace(Face face, Bitmap scaled, Bitmap frame_bmp1) {
        //check head orientation if looking up or down
        if (face.getHeadEulerAngleX() < -8) {
            inform("Reduce facing down");
            graphicOverlay.add(new FaceGraphic(graphicOverlay, face, Color.RED, Color.RED, true));
            graphicOverlay.setImageSourceInfo(frame_bmp1.getWidth(), frame_bmp1.getHeight(), flipX);
            hideReg();
        } else if (face.getHeadEulerAngleX() > 4) {
            inform("Reduce facing up too much");
            graphicOverlay.add(new FaceGraphic(graphicOverlay, face, Color.RED, Color.RED, true));
            graphicOverlay.setImageSourceInfo(frame_bmp1.getWidth(), frame_bmp1.getHeight(), flipX);
            hideReg();
        } else if (face.getHeadEulerAngleY() < -5) {
            inform("Try to minimize looking at the right");
            graphicOverlay.add(new FaceGraphic(graphicOverlay, face, Color.RED, Color.RED, true));
            graphicOverlay.setImageSourceInfo(frame_bmp1.getWidth(), frame_bmp1.getHeight(), flipX);
            hideReg();
        } else if (face.getHeadEulerAngleY() > 5) {
            inform("Try to minimize looking at the left");
            graphicOverlay.add(new FaceGraphic(graphicOverlay, face, Color.RED, Color.RED, true));
            graphicOverlay.setImageSourceInfo(frame_bmp1.getWidth(), frame_bmp1.getHeight(), flipX);
            hideReg();
        } else {
//            inform("Refistration ready");
            graphicOverlay.add(new FaceGraphic(graphicOverlay, face, Color.GREEN, Color.GREEN, true));
            graphicOverlay.setImageSourceInfo(frame_bmp1.getWidth(), frame_bmp1.getHeight(), flipX);
            recognizeImage(scaled, face); //Send scaled bitmap to create face data.
            previewPersonImage(scaled);
        }

    }

    private void recognizeImage(Bitmap scaled, Face face) {
        if (RecogPhase == 0) {
            embeddings = getFaceEmbeddingsFromBitmap(scaled);
            inform("Tap to register a normal face");
            showReg();
        } else if (RecogPhase == 1) {


            if (face.getSmilingProbability() != null && face.getSmilingProbability() > 0.70) {
                inform("Finally register a smiling face.");
                smile_embeddings = getFaceEmbeddingsFromBitmap(scaled);
                showReg();
            } else {
                inform("Ask the employee to smile.");
                hideReg();
            }
        }
        getCloser = false;
    }

    private void timeStartAM() {
        MaterialTimePicker.Builder materialTimeBuilder = new MaterialTimePicker.Builder();
//        materialTimeBuilder.setMinute(10).setMinute(10).setTimeFormat(TimeFormat.CLOCK_12H);
//        materialTimeBuilder.setTimeFormat(TimeFormat.CLOCK_24H);
        materialTimeBuilder.setHour(8);
        MaterialTimePicker materialTimePicker = materialTimeBuilder.build();
        materialTimePicker.addOnPositiveButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int hour =  materialTimePicker.getHour();
                int min =  materialTimePicker.getMinute();
                String hourStr = String.valueOf(hour);
                String minuteStr = String.valueOf(min);
                String time = "";
                String suffix = (hour < 12) ? "AM" : "PM";
                if ( hour < 12){
                }else{
                    Log.e("determine pm", String.valueOf(hour));
                    hourStr = String.valueOf((hour == 12) ? 12 : hour - 12);
                }
                if (min < 10){
                    minuteStr = minuteStr+0;
                }
                if (hour == 0){
                    hourStr = String.valueOf(12);
                }
                time = hourStr + ":" + minuteStr + " " + suffix;
                newFaceRegisterBinding.mornChipIn.setText("AM IN - "+time);
                amstarttime = time;
                Log.e("amstarttime", amstarttime);
                materialTimePicker.removeOnPositiveButtonClickListener(this);
            }
        });
        materialTimePicker.show(getSupportFragmentManager(), "starttime");
    }
    private void timeStartPM(){
        MaterialTimePicker.Builder materialTimeBuilder = new MaterialTimePicker.Builder();
//        materialTimeBuilder.setMinute(10).setMinute(10).setTimeFormat(TimeFormat.CLOCK_12H);
//        materialTimeBuilder.setTimeFormat(TimeFormat.CLOCK_24H);
        materialTimeBuilder.setHour(13);
        MaterialTimePicker materialTimePicker = materialTimeBuilder.build();
        materialTimePicker.addOnPositiveButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int hour =  materialTimePicker.getHour();
                int min =  materialTimePicker.getMinute();
                String hourStr = String.valueOf(hour);
                String minuteStr = String.valueOf(min);
                String time = "";
                String suffix = (hour < 12) ? "AM" : "PM";
                if ( hour < 12){
                }else{
                    Log.e("determine pm", String.valueOf(hour));
                    hourStr = String.valueOf((hour == 12) ? 12 : hour - 12);
                }
                if (min < 10){
                    minuteStr = minuteStr+0;
                }
                if (hour == 0){
                    hourStr = String.valueOf(12);
                }
                time = hourStr+":"+minuteStr+" "+suffix;
                newFaceRegisterBinding.noonChipIn.setText("PM IN - "+time);
                pmstarttime = time;
                Log.e("pmstarttime", pmstarttime);
                Log.e("pmtime", String.valueOf(hour));
                materialTimePicker.removeOnPositiveButtonClickListener(this);
            }
        });
        materialTimePicker.show(getSupportFragmentManager(), "starttime");
    }

    private void timeEndPM(){
        MaterialTimePicker.Builder materialTimeBuilder = new MaterialTimePicker.Builder();
//        materialTimeBuilder.setMinute(10).setMinute(10).setTimeFormat(TimeFormat.CLOCK_12H);
        materialTimeBuilder.setHour(17);
        MaterialTimePicker materialTimePicker = materialTimeBuilder.build();
        materialTimePicker.addOnPositiveButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int hour =  materialTimePicker.getHour();
                int min =  materialTimePicker.getMinute();
                String hourStr = String.valueOf(hour);
                String minuteStr = String.valueOf(min);
                String time = "";
                String suffix = (hour < 12) ? "AM" : "PM";
                if ( hour < 12){
                }else{
                    Log.e("determine pm", String.valueOf(hour));
                    hourStr = String.valueOf((hour == 12) ? 12 : hour - 12);
                }
                if (min < 10){
                    minuteStr = minuteStr+0;
                }
                if (hour == 0){
                    hourStr = String.valueOf(12);
                }
                time = hourStr+":"+minuteStr+" "+suffix;
                newFaceRegisterBinding.noonChipOut.setText("PM OUT - "+time);
//                SimpleDateFormat parser = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
//                String sample = "Aug 27, 2022";
//                SimpleDateFormat formattall = new SimpleDateFormat("MM-DD-yyyy hh:mm", Locale.US);
//                try {
//                    Date date =  parser.parse(sample);
//                    Log.e("date", String.valueOf(date.getTime()));
//                    Log.e("time", parser.format(sample));
//
//                } catch (ParseException e) {
//                    Log.e("error", e.toString());
//                }
                pmEndtime = time;
                Log.e("pmEndtime", pmEndtime);
                materialTimePicker.removeOnPositiveButtonClickListener(this);
            }
        });
        materialTimePicker.show(getSupportFragmentManager(), "starttime");
    }

    private void timeEndAM(){
        MaterialTimePicker.Builder materialTimeBuilder = new MaterialTimePicker.Builder();
//        materialTimeBuilder.setMinute(10).setMinute(10).setTimeFormat(TimeFormat.CLOCK_12H);
        materialTimeBuilder.setHour(12);
        MaterialTimePicker materialTimePicker = materialTimeBuilder.build();
        materialTimePicker.addOnPositiveButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int hour =  materialTimePicker.getHour();
                int min =  materialTimePicker.getMinute();
                String hourStr = String.valueOf(hour);
                String minuteStr = String.valueOf(min);
                String time = "";
                String suffix = (hour < 12) ? "AM" : "PM";
                if ( hour < 12){
                }else{
                    Log.e("determine pm", String.valueOf(hour));
                    hourStr = String.valueOf((hour == 12) ? 12 : hour - 12);
                }
                if (min < 10){
                    minuteStr = minuteStr+0;
                }
                if (hour == 0){
                    hourStr = String.valueOf(12);
                }
                time = hourStr+":"+minuteStr+" "+suffix;
                newFaceRegisterBinding.mornChipOut.setText("PM OUT - "+time);
//                SimpleDateFormat parser = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
//                String sample = "Aug 27, 2022";
//                SimpleDateFormat formattall = new SimpleDateFormat("MM-DD-yyyy hh:mm", Locale.US);
//                try {
//                    Date date =  parser.parse(sample);
//                    Log.e("date", String.valueOf(date.getTime()));
//                    Log.e("time", parser.format(sample));
//
//                } catch (ParseException e) {
//                    Log.e("error", e.toString());
//                }
                amEndtime = time;
                Log.e("pmEndtime", pmEndtime);
                materialTimePicker.removeOnPositiveButtonClickListener(this);
            }
        });
        materialTimePicker.show(getSupportFragmentManager(), "starttime");
    }

    public float[][] getFaceEmbeddingsFromBitmap(Bitmap bitmap) {
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


        float[][] faceDataOutput = new float[1][OUTPUT_SIZE]; //output of model will be stored in this variable

        outputMap.put(0, faceDataOutput);

        tfLite.runForMultipleInputsOutputs(inputArray, outputMap); //Run model
        return faceDataOutput;
    }

    public void initFirebases() {
        firebaseApp = FirebaseApp.initializeApp(this);
        firebaseAuth = FirebaseAuth.getInstance();
    }

    private void previewPersonImage(Bitmap scaled) {
        facePreviewImg = scaled;
        binding.facePreview.setImageBitmap(scaled);
        binding.imageCard.setVisibility(View.VISIBLE);
    }

    private void inform(String msg) {
        binding.recogInfo.setVisibility(View.VISIBLE);
        binding.recogInfo.setText(msg);
    }

    private void saveNewEmployee(Map<String, Object> employeeData) {
        cameraProvider.unbindAll();
        db.collection("employees")
                .add(employeeData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Employee saved", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving employee" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }).addOnCompleteListener(task -> {
                    popupViews.hideFaceLoading();
                    finish();
                });
    }

    private void updateEmp(Map<String, Object> employeeData) {
        TastyToast.makeText(this, "Updating face data", TastyToast.LENGTH_SHORT, TastyToast.DEFAULT);
        String empId = employeeData.get("employee-id").toString();
        db.collection("employees").document(empId).set(employeeData)
                .addOnSuccessListener(unused -> {
                    popupViews.toastSuccess("Face registration updated");
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.wtf("yawa", e.getMessage());
                    popupViews.toastConfused(e.getMessage());
                });
    }

    private void loadModel() {
        try {
            tfLite = new Interpreter(loadModelFile(this, modelFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void requestPermsNow() {
        EasyPermissions.requestPermissions(
                new PermissionRequest.Builder(this, 101, perms)
                        .setRationale("Grant storage access permision in order for the app to work as it's intended.")
                        .setPositiveButtonText("OK")
                        .setNegativeButtonText("Cancel")
                        .build());
    }

    private MappedByteBuffer loadModelFile(Activity activity, String MODEL_FILE) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void statusbarConfig() {
        Window w = getWindow();
        w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
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

}