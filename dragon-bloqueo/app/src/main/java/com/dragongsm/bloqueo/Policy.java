package com.dragongsm.bloqueo;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.app.admin.FactoryResetProtectionPolicy;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.UserManager;
import android.telecom.TelecomManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Acciones sobre el equipo: proteger, bloquear, desbloquear y liberar. */
public final class Policy {
    private Policy() {}

    private static final int JOB_ID = 4410;
    private static final String CANAL = "bloqueo";

    /** Restricciones que quedan puestas mientras el equipo se esté pagando. */
    private static String[] restricciones() {
        List<String> r = new ArrayList<>();
        r.add(UserManager.DISALLOW_FACTORY_RESET);
        r.add(UserManager.DISALLOW_SAFE_BOOT);
        r.add(UserManager.DISALLOW_ADD_USER);
        r.add(UserManager.DISALLOW_DEBUGGING_FEATURES);
        r.add(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES);
        r.add(UserManager.DISALLOW_CONFIG_DATE_TIME);
        if (Build.VERSION.SDK_INT >= 29) r.add(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY);
        return r.toArray(new String[0]);
    }

    public static ComponentName admin(Context c) {
        return new ComponentName(c.getApplicationContext(), AdminReceiver.class);
    }

    private static DevicePolicyManager dpm(Context c) {
        return (DevicePolicyManager) c.getSystemService(Context.DEVICE_POLICY_SERVICE);
    }

    public static boolean isOwner(Context c) {
        try {
            DevicePolicyManager d = dpm(c);
            return d != null && d.isDeviceOwnerApp(c.getPackageName());
        } catch (Throwable t) {
            return false;
        }
    }

    /** Se ejecuta una vez al terminar la instalación con QR (y en cada reinicio, por las dudas). */
    public static void setup(Context ctx) {
        Context c = ctx.getApplicationContext();
        schedule(c);
        if (!isOwner(c) || Config.released(c)) return;
        DevicePolicyManager d = dpm(c);
        ComponentName a = admin(c);
        String pkg = c.getPackageName();
        try {
            d.setPermissionGrantState(a, pkg, Manifest.permission.READ_PHONE_STATE,
                    DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
            if (Build.VERSION.SDK_INT >= 33) {
                d.setPermissionGrantState(a, pkg, "android.permission.POST_NOTIFICATIONS",
                        DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
            }
        } catch (Throwable ignored) {
        }
        for (String r : restricciones()) {
            try { d.addUserRestriction(a, r); } catch (Throwable ignored) {}
        }
        try { d.setUninstallBlocked(a, pkg, true); } catch (Throwable ignored) {}
        try { d.setLockTaskPackages(a, lockTaskPackages(c)); } catch (Throwable ignored) {}
        applyFrp(c);
    }

    /**
     * Protección anti-recovery (FRP): graba en el equipo qué cuenta de Google lo
     * desbloquea después de un reseteo por recovery. Es el "hoja verde".
     */
    public static void applyFrp(Context ctx) {
        Context c = ctx.getApplicationContext();
        if (Build.VERSION.SDK_INT < 30 || !isOwner(c) || Config.released(c)) return;
        String cuenta = Config.frpAccount(c);
        try {
            DevicePolicyManager d = dpm(c);
            ComponentName a = admin(c);
            FactoryResetProtectionPolicy.Builder b = new FactoryResetProtectionPolicy.Builder()
                    .setFactoryResetProtectionEnabled(cuenta.length() > 0);
            if (cuenta.length() > 0) b.setFactoryResetProtectionAccounts(Collections.singletonList(cuenta));
            d.setFactoryResetProtectionPolicy(a, b.build());
        } catch (Throwable ignored) {
        }
    }

    private static void clearFrp(Context c) {
        if (Build.VERSION.SDK_INT < 30) return;
        try {
            FactoryResetProtectionPolicy p = new FactoryResetProtectionPolicy.Builder()
                    .setFactoryResetProtectionEnabled(false)
                    .build();
            dpm(c).setFactoryResetProtectionPolicy(admin(c), p);
        } catch (Throwable ignored) {
        }
    }

    /** Revisión automática cada 15 minutos, también después de reiniciar. */
    public static void schedule(Context c) {
        try {
            JobScheduler js = (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (js == null) return;
            if (Config.released(c)) { js.cancel(JOB_ID); return; }
            if (js.getPendingJob(JOB_ID) != null) return;
            JobInfo job = new JobInfo.Builder(JOB_ID, new ComponentName(c, CheckJobService.class))
                    .setPeriodic(15L * 60L * 1000L)
                    .setPersisted(true)
                    .build();
            js.schedule(job);
        } catch (Throwable ignored) {
        }
    }

    /** Apps que pueden abrirse con el equipo bloqueado: esta app y el teléfono (emergencias). */
    private static String[] lockTaskPackages(Context c) {
        Set<String> s = new HashSet<>(dialerPackages(c));
        s.add(c.getPackageName());
        return s.toArray(new String[0]);
    }

    private static Set<String> dialerPackages(Context c) {
        Set<String> s = new HashSet<>();
        s.add("com.android.phone");
        s.add("com.android.server.telecom");
        s.add("com.android.dialer");
        s.add("com.google.android.dialer");
        s.add("com.samsung.android.dialer");
        s.add("com.samsung.android.incallui");
        s.add("com.android.incallui");
        s.add("com.motorola.dialer");
        s.add("com.android.emergency");
        try {
            TelecomManager tm = (TelecomManager) c.getSystemService(Context.TELECOM_SERVICE);
            if (tm != null && tm.getDefaultDialerPackage() != null) s.add(tm.getDefaultDialerPackage());
        } catch (Throwable ignored) {
        }
        return s;
    }

    public static void lock(Context ctx) {
        Context c = ctx.getApplicationContext();
        boolean already = Config.locked(c);
        Config.setLocked(c, true);
        if (isOwner(c)) {
            DevicePolicyManager d = dpm(c);
            ComponentName a = admin(c);
            try { d.setLockTaskPackages(a, lockTaskPackages(c)); } catch (Throwable ignored) {}
            if (Build.VERSION.SDK_INT >= 28) {
                try { d.setLockTaskFeatures(a, DevicePolicyManager.LOCK_TASK_FEATURE_NONE); } catch (Throwable ignored) {}
            }
            setLockHome(c, true);
            suspendApps(c);
        }
        if (!already) {
            showLockScreen(c);
            if (isOwner(c)) {
                try { dpm(c).lockNow(); } catch (Throwable ignored) {}
            }
        }
    }

    public static void unlock(Context ctx) {
        Context c = ctx.getApplicationContext();
        boolean was = Config.locked(c);
        Config.setLocked(c, false);
        if (isOwner(c)) {
            setLockHome(c, false);
            unsuspendApps(c);
        }
        if (was) {
            try {
                NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) nm.cancel(1);
            } catch (Throwable ignored) {
            }
        }
    }

    /** Fin del plan de pagos: saca todos los controles y deja el celular libre. */
    public static void release(Context ctx) {
        Context c = ctx.getApplicationContext();
        unlock(c);
        Config.setReleased(c, true);
        try {
            JobScheduler js = (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (js != null) js.cancel(JOB_ID);
        } catch (Throwable ignored) {
        }
        if (!isOwner(c)) return;
        DevicePolicyManager d = dpm(c);
        ComponentName a = admin(c);
        for (String r : restricciones()) {
            try { d.clearUserRestriction(a, r); } catch (Throwable ignored) {}
        }
        clearFrp(c);
        try { d.setUninstallBlocked(a, c.getPackageName(), false); } catch (Throwable ignored) {}
        try { d.setLockTaskPackages(a, new String[0]); } catch (Throwable ignored) {}
        try { d.clearDeviceOwnerApp(c.getPackageName()); } catch (Throwable ignored) {}
    }

    private static void setLockHome(Context c, boolean on) {
        DevicePolicyManager d = dpm(c);
        ComponentName a = admin(c);
        ComponentName home = new ComponentName(c.getPackageName(), c.getPackageName() + ".LockHome");
        PackageManager pm = c.getPackageManager();
        try {
            pm.setComponentEnabledSetting(home,
                    on ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        } catch (Throwable ignored) {
        }
        try {
            if (on) {
                IntentFilter f = new IntentFilter(Intent.ACTION_MAIN);
                f.addCategory(Intent.CATEGORY_HOME);
                f.addCategory(Intent.CATEGORY_DEFAULT);
                d.addPersistentPreferredActivity(a, f, home);
            } else {
                d.clearPackagePersistentPreferredActivities(a, c.getPackageName());
            }
        } catch (Throwable ignored) {
        }
    }

    /** Mientras está bloqueado, las demás apps (WhatsApp, juegos, etc.) quedan suspendidas. */
    private static void suspendApps(Context c) {
        try {
            PackageManager pm = c.getPackageManager();
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> list = pm.queryIntentActivities(i, 0);
            Set<String> keep = dialerPackages(c);
            keep.add(c.getPackageName());
            Set<String> pkgs = new HashSet<>();
            for (ResolveInfo r : list) {
                if (r.activityInfo == null) continue;
                String p = r.activityInfo.packageName;
                if (!keep.contains(p)) pkgs.add(p);
            }
            String prev = Config.suspended(c);
            if (prev.length() > 0) {
                for (String p : prev.split(",")) if (p.length() > 0 && !keep.contains(p)) pkgs.add(p);
            }
            String[] arr = pkgs.toArray(new String[0]);
            String[] failed = dpm(c).setPackagesSuspended(admin(c), arr, true);
            Set<String> ok = new HashSet<>(pkgs);
            if (failed != null) for (String f : failed) ok.remove(f);
            Config.setSuspended(c, String.join(",", ok));
        } catch (Throwable ignored) {
        }
    }

    private static void unsuspendApps(Context c) {
        String prev = Config.suspended(c);
        if (prev.length() == 0) return;
        try {
            dpm(c).setPackagesSuspended(admin(c), prev.split(","), false);
        } catch (Throwable ignored) {
        }
        Config.setSuspended(c, "");
    }

    /** Muestra la pantalla de bloqueo (directo y con una notificación de pantalla completa). */
    public static void showLockScreen(Context c) {
        Intent i = new Intent(c, LockActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try { c.startActivity(i); } catch (Throwable ignored) {}
        try {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            NotificationChannel ch = new NotificationChannel(CANAL, "Bloqueo del equipo", NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(ch);
            PendingIntent pi = PendingIntent.getActivity(c, 0, i,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification n = new Notification.Builder(c, CANAL)
                    .setSmallIcon(android.R.drawable.ic_lock_lock)
                    .setContentTitle("Equipo bloqueado")
                    .setContentText("Comunicate con " + Config.nombreLocal(c))
                    .setContentIntent(pi)
                    .setFullScreenIntent(pi, true)
                    .setOngoing(true)
                    .setCategory(Notification.CATEGORY_ALARM)
                    .build();
            nm.notify(1, n);
        } catch (Throwable ignored) {
        }
    }
}
