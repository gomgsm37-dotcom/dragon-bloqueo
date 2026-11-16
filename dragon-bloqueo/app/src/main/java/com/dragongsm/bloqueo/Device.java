package com.dragongsm.bloqueo;

import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;

/** Lee el IMEI del equipo (la app puede porque es administradora del equipo). */
public final class Device {
    private Device() {}

    public static String imei(Context c, int slot) {
        try {
            TelephonyManager tm = (TelephonyManager) c.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm == null) return "";
            String v = tm.getImei(slot);
            return v == null ? "" : v;
        } catch (Throwable t) {
            return "";
        }
    }

    /** Identificador principal: IMEI 1, o IMEI 2, o un ID interno si no hay IMEI. */
    public static String id(Context c) {
        String a = imei(c, 0);
        if (a.length() > 0) return a;
        String b = imei(c, 1);
        if (b.length() > 0) return b;
        String aid = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ANDROID_ID);
        return "ID-" + (aid == null ? "desconocido" : aid);
    }
}
