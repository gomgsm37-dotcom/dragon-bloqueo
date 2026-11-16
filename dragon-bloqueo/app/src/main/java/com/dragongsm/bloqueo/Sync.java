package com.dragongsm.bloqueo;

import android.content.Context;
import android.os.Build;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Consulta al panel el estado de este equipo (por IMEI) y aplica bloqueo o desbloqueo.
 * Siempre se llama desde un hilo en segundo plano.
 */
public final class Sync {
    private Sync() {}

    private static final long DIA = 24L * 60L * 60L * 1000L;

    public static synchronized void run(Context ctx) {
        Context c = ctx.getApplicationContext();
        if (Config.released(c)) return;
        String server = Config.server(c);
        if (server.length() > 0) {
            String id = Device.id(c);
            try {
                HttpURLConnection con = (HttpURLConnection) new URL(server + "/estado/" + id + ".json").openConnection();
                con.setConnectTimeout(15000);
                con.setReadTimeout(15000);
                con.setUseCaches(false);
                int code = con.getResponseCode();
                long serverDate = con.getDate();
                if (code == 200) {
                    String body = read(con.getInputStream()).trim();
                    if (serverDate > 0) Config.setOffset(c, serverDate - System.currentTimeMillis());
                    Config.setEstado(c, "null".equals(body) ? "" : body);
                    Config.setLastOk(c, Config.now(c));
                    Config.setLastError(c, "");
                } else {
                    Config.setLastError(c, "Servidor respondió " + code);
                }
                con.disconnect();
            } catch (Throwable t) {
                Config.setLastError(c, "Sin conexión: " + t.getClass().getSimpleName());
            }
        }
        apply(c);
        if (server.length() > 0 && !Config.released(c)) report(c, server);
    }

    /** Decide con los últimos datos guardados (funciona también sin internet). */
    public static void apply(Context c) {
        if (Config.released(c)) return;
        String json = Config.estadoJson(c);
        boolean lock = false;
        if (json.length() > 0) {
            try {
                JSONObject e = new JSONObject(json);
                if (e.optBoolean("liberar", false)) {
                    report(c, Config.server(c), true);
                    Policy.release(c);
                    return;
                }
                long now = Config.now(c);
                long vence = e.optLong("vence", 0L);
                int dias = e.optInt("diasSinConexion", 0);
                long lastOk = Config.lastOk(c);
                if (e.optBoolean("bloqueado", false)) lock = true;
                if (vence > 0 && now > vence) lock = true;
                if (dias > 0 && lastOk > 0 && now - lastOk > dias * DIA) lock = true;
                String tel = e.optString("telefono", "");
                String nombre = e.optString("negocio", "");
                Config.setLocal(c, nombre, tel);
                String frp = e.optString("frp", "");
                if (frp.length() > 0 && !frp.equals(Config.frpAccount(c))) {
                    Config.setFrpAccount(c, frp);
                    Policy.applyFrp(c);
                }
            } catch (Throwable ignored) {
            }
        }
        if (lock) Policy.lock(c); else Policy.unlock(c);
    }

    private static void report(Context c, String server) { report(c, server, false); }

    private static void report(Context c, String server, boolean liberado) {
        if (server == null || server.length() == 0) return;
        try {
            JSONObject r = new JSONObject();
            JSONObject sv = new JSONObject();
            sv.put(".sv", "timestamp");
            r.put("ultimo", sv);
            r.put("modelo", Build.MANUFACTURER + " " + Build.MODEL);
            r.put("android", Build.VERSION.RELEASE);
            r.put("imei2", Device.imei(c, 1));
            r.put("bloqueado", Config.locked(c));
            r.put("liberado", liberado);
            r.put("admin", Policy.isOwner(c));
            r.put("version", 1);
            byte[] data = r.toString().getBytes(StandardCharsets.UTF_8);
            HttpURLConnection con = (HttpURLConnection) new URL(server + "/reportes/" + Device.id(c) + ".json").openConnection();
            con.setConnectTimeout(15000);
            con.setReadTimeout(15000);
            con.setRequestMethod("PUT");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json");
            OutputStream os = con.getOutputStream();
            os.write(data);
            os.close();
            con.getResponseCode();
            con.disconnect();
        } catch (Throwable ignored) {
        }
    }

    private static String read(InputStream in) throws java.io.IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
