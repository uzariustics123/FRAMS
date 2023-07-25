package com.macxs.facerecogz;

import static android.provider.Settings.*;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

public class DeviceId {

    public static String getDeviceId(Context context) {
        String deviceId = Secure.getString(context.getContentResolver(),
                Secure.ANDROID_ID);
        return deviceId;
    }
    public static String getDeviceName() {
        String devName = Build.DEVICE;
        return devName;
    }
    public static String getDeviceModel() {
        String devModel = Build.MODEL;
        return devModel;
    }

}
