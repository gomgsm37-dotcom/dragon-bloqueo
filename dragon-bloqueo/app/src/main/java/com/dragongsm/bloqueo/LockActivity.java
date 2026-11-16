package com.dragongsm.bloqueo;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

/** Pantalla que ve el cliente cuando el equipo está bloqueado. */
public class LockActivity extends Activity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView mensaje;
    private TextView estadoTxt;
    private boolean checking = false;

    private final Runnable poll = new Runnable() {
        @Override
        public void run() {
            verificar(false);
            handler.postDelayed(this, 30000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(buildUi());
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private View buildUi() {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Color.parseColor("#111418"));
        sv.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(48), dp(24), dp(32));
        sv.addView(root);

        TextView icon = new TextView(this);
        icon.setText("🔒");
        icon.setTextSize(64);
        icon.setGravity(Gravity.CENTER);
        root.addView(icon);

        TextView title = new TextView(this);
        title.setText("Equipo bloqueado");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(12), 0, dp(12));
        root.addView(title);

        mensaje = new TextView(this);
        mensaje.setTextColor(Color.parseColor("#E6E6E6"));
        mensaje.setTextSize(18);
        mensaje.setGravity(Gravity.CENTER);
        mensaje.setLineSpacing(0, 1.2f);
        root.addView(mensaje);

        TextView local = new TextView(this);
        local.setText(Config.nombreLocal(this) + (Config.telLocal(this).length() > 0 ? "\nTel: " + Config.telLocal(this) : ""));
        local.setTextColor(Color.parseColor("#F5B841"));
        local.setTextSize(18);
        local.setTypeface(Typeface.DEFAULT_BOLD);
        local.setGravity(Gravity.CENTER);
        local.setPadding(0, dp(20), 0, dp(24));
        root.addView(local);

        boolean tel = getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);

        if (tel && Config.telLocal(this).length() > 0) {
            Button llamar = boton("Llamar al local", "#2E7D32");
            llamar.setOnClickListener(v -> abrirDialer(Config.telLocal(this)));
            root.addView(llamar);
        }

        Button pague = boton("Ya pagué – verificar", "#1565C0");
        pague.setOnClickListener(v -> verificar(true));
        root.addView(pague);
        pague.requestFocus(); // foco inicial para el control remoto

        if (tel) {
            Button sos = boton("Emergencias 911", "#C62828");
            sos.setOnClickListener(v -> emergencia());
            root.addView(sos);
        }

        estadoTxt = new TextView(this);
        estadoTxt.setTextColor(Color.parseColor("#9AA0A6"));
        estadoTxt.setTextSize(13);
        estadoTxt.setGravity(Gravity.CENTER);
        estadoTxt.setPadding(0, dp(16), 0, 0);
        root.addView(estadoTxt);

        TextView imei = new TextView(this);
        boolean esTel = getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
        imei.setText((esTel ? "IMEI: " : "ID: ") + Device.id(this));
        imei.setTextColor(Color.parseColor("#6B7178"));
        imei.setTextSize(12);
        imei.setGravity(Gravity.CENTER);
        imei.setPadding(0, dp(8), 0, 0);
        root.addView(imei);

        actualizarMensaje();
        return sv;
    }

    private Button boton(String txt, String color) {
        Button b = new Button(this);
        b.setText(txt);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(17);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(color));
        bg.setCornerRadius(dp(12));
        b.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56));
        lp.setMargins(0, dp(6), 0, dp(6));
        b.setLayoutParams(lp);
        return b;
    }

    private void actualizarMensaje() {
        String m = "Tu cuota está vencida. Para volver a usar el equipo, regularizá el pago.";
        try {
            String j = Config.estadoJson(this);
            if (j.length() > 0) {
                String custom = new JSONObject(j).optString("mensaje", "");
                if (custom.length() > 0) m = custom;
            }
        } catch (Throwable ignored) {
        }
        mensaje.setText(m);
    }

    private void abrirDialer(String tel) {
        try {
            Intent i = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + tel));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Throwable ignored) {
        }
    }

    private void emergencia() {
        try {
            Intent i = new Intent("com.android.phone.EmergencyDialer.DIAL");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Throwable t) {
            abrirDialer("911");
        }
    }

    private void verificar(final boolean manual) {
        if (checking) return;
        checking = true;
        if (manual) estadoTxt.setText("Verificando…");
        final Context c = getApplicationContext();
        new Thread(() -> {
            try { Sync.run(c); } catch (Throwable ignored) {}
            handler.post(() -> {
                checking = false;
                if (!Config.locked(c)) {
                    salir();
                } else {
                    actualizarMensaje();
                    String err = Config.lastError(c);
                    estadoTxt.setText(err.length() > 0 ? err : (manual ? "Todavía figura como bloqueado." : ""));
                }
            });
        }, "dragon-check").start();
    }

    private void salir() {
        try { stopLockTask(); } catch (Throwable ignored) {}
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Config.locked(this)) {
            salir();
            return;
        }
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            boolean inLock = am != null && am.getLockTaskModeState() != ActivityManager.LOCK_TASK_MODE_NONE;
            if (!inLock) startLockTask();
        } catch (Throwable ignored) {
        }
        handler.removeCallbacks(poll);
        handler.postDelayed(poll, 30000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(poll);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        // No se puede salir con "atrás" mientras está bloqueado.
    }
}
