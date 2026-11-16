package com.dragongsm.bloqueo;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.BaseBundle;

/** Datos guardados en el celular. */
public final class Config {
    private Config() {}

    private static SharedPreferences p(Context c) {
        return c.getApplicationContext().getSharedPreferences("cfg", Context.MODE_PRIVATE);
    }

    public static String server(Context c) { return p(c).getString("server", ""); }
    public static String telLocal(Context c) { return p(c).getString("tel", ""); }
    public static String nombreLocal(Context c) { return p(c).getString("nombre", "Dragon G.S.M"); }
    public static boolean locked(Context c) { return p(c).getBoolean("locked", false); }
    public static boolean released(Context c) { return p(c).getBoolean("released", false); }
    public static String estadoJson(Context c) { return p(c).getString("estado", ""); }
    public static long lastOk(Context c) { return p(c).getLong("lastOk", 0L); }
    public static long timeOffset(Context c) { return p(c).getLong("offset", 0L); }
    public static String suspended(Context c) { return p(c).getString("suspended", ""); }
    public static String lastError(Context c) { return p(c).getString("lastError", ""); }
    public static String frpAccount(Context c) { return p(c).getString("frp", ""); }

    public static void setServer(Context c, String s) {
        if (s == null) return;
        s = s.trim();
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        p(c).edit().putString("server", s).apply();
    }
    public static void setLocked(Context c, boolean v) { p(c).edit().putBoolean("locked", v).commit(); }
    public static void setReleased(Context c, boolean v) { p(c).edit().putBoolean("released", v).commit(); }
    public static void setEstado(Context c, String json) { p(c).edit().putString("estado", json).apply(); }
    public static void setLastOk(Context c, long t) { p(c).edit().putLong("lastOk", t).apply(); }
    public static void setOffset(Context c, long o) { p(c).edit().putLong("offset", o).apply(); }
    public static void setSuspended(Context c, String s) { p(c).edit().putString("suspended", s).commit(); }
    public static void setLastError(Context c, String s) { p(c).edit().putString("lastError", s == null ? "" : s).apply(); }
    public static void setFrpAccount(Context c, String s) {
        if (s == null) return;
        s = s.trim();
        if (s.equals(frpAccount(c))) return;
        p(c).edit().putString("frp", s).commit();
    }
    public static void setLocal(Context c, String nombre, String tel) {
        SharedPreferences.Editor e = p(c).edit();
        if (nombre != null && nombre.length() > 0) e.putString("nombre", nombre);
        if (tel != null && tel.length() > 0) e.putString("tel", tel);
        e.apply();
    }

    /** Hora corregida con la hora del servidor (evita que cambien la fecha del celular). */
    public static long now(Context c) { return System.currentTimeMillis() + timeOffset(c); }

    /** Guarda los datos que vienen dentro del código QR de instalación. */
    public static void saveExtras(Context c, BaseBundle b) {
        if (b == null) return;
        String server = b.getString("server");
        if (server != null && server.length() > 0 && server(c).length() == 0) setServer(c, server);
        setLocal(c, b.getString("nombre"), b.getString("tel"));
        String frp = b.getString("frp");
        if (frp != null && frp.length() > 0) setFrpAccount(c, frp);
    }
}
