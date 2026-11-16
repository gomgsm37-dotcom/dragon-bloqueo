package com.dragongsm.bloqueo;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;

public class AdminReceiver extends DeviceAdminReceiver {

    @Override
    public void onProfileProvisioningComplete(Context context, Intent intent) {
        try {
            PersistableBundle b = intent.getParcelableExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE);
            Config.saveExtras(context, b);
        } catch (Throwable ignored) {
        }
        Policy.setup(context);
        Worker.syncAsync(context);
    }

    @Override
    public void onEnabled(Context context, Intent intent) {
        Policy.setup(context);
    }
}
