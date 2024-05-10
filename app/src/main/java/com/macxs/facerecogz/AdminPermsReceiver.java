package com.macxs.facerecogz;
import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.widget.Toast;

public class AdminPermsReceiver extends DeviceAdminReceiver{
    void showToast(Context context, String msg) {
//        String status = context.getString(R.string.admin_receiver_status, msg);
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEnabled(Context context, Intent intent) {
        showToast(context,"enabled");
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return "disabledRequest";
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        showToast(context, "disabled");
    }

    @Override
    public void onPasswordChanged(Context context, Intent intent, UserHandle userHandle) {
        showToast(context, "pass");
    }

}
