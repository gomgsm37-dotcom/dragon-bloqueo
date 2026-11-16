package com.dragongsm.bloqueo;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.os.Bundle;
import android.os.PersistableBundle;

/** Android 12+: último paso de la instalación con QR. Aplica la protección. */
public class PolicyComplianceActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            PersistableBundle extras = getIntent().getParcelableExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE);
            Config.saveExtras(this, extras);
        } catch (Throwable ignored) {
        }
        Policy.setup(this);
        Worker.syncAsync(this);
        setResult(RESULT_OK);
        finish();
    }
}
