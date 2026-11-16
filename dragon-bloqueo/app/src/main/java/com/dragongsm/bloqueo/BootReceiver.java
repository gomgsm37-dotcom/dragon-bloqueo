package com.dragongsm.bloqueo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Al prender el celular: vuelve a aplicar la protección y el bloqueo si correspondía. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final Context c = context.getApplicationContext();
        if (Config.released(c)) return;
        Policy.setup(c);
        if (Config.locked(c)) Policy.showLockScreen(c);
        final PendingResult pr = goAsync();
        new Thread(() -> {
            try { Sync.run(c); } catch (Throwable ignored) {}
            pr.finish();
        }, "dragon-boot").start();
    }
}
