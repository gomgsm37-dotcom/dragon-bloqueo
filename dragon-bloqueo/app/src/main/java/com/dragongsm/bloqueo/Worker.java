package com.dragongsm.bloqueo;

import android.content.Context;

/** Corre la verificación en segundo plano. */
public final class Worker {
    private Worker() {}

    public static void syncAsync(Context ctx) {
        final Context c = ctx.getApplicationContext();
        new Thread(() -> {
            try { Sync.run(c); } catch (Throwable ignored) {}
        }, "dragon-sync").start();
    }
}
