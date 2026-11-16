package com.dragongsm.bloqueo;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Pantalla informativa: estado de la protección, IMEI y conexión con el panel. */
public class MainActivity extends Activity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView info;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private View buildUi() {
        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(32), dp(20), dp(20));
        sv.addView(root);

        TextView t = new TextView(this);
        t.setText("Dragon Protección");
        t.setTextSize(24);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(Color.parseColor("#B71C1C"));
        root.addView(t);

        TextView sub = new TextView(this);
        sub.setText("Equipo vendido en cuotas por " + Config.nombreLocal(this)
                + ". Mientras el plan de pagos esté vigente, el equipo puede bloquearse si una cuota vence sin pagar.");
        sub.setTextSize(15);
        sub.setPadding(0, dp(8), 0, dp(16));
        root.addView(sub);

        info = new TextView(this);
        info.setTextSize(15);
        info.setTextIsSelectable(true);
        info.setLineSpacing(0, 1.25f);
        root.addView(info);

        Button check = new Button(this);
        check.setText("Verificar ahora");
        check.setAllCaps(false);
        check.setOnClickListener(v -> {
            info.setText("Verificando…");
            final Context c = getApplicationContext();
            new Thread(() -> {
                try { Sync.run(c); } catch (Throwable ignored) {}
                handler.post(this::refresh);
            }).start();
        });
        root.addView(check);

        // Solo se puede cargar el servidor a mano si todavía no tiene uno (instalación por cable).
        if (Config.server(this).length() == 0) {
            TextView l = new TextView(this);
            l.setText("\nDirección del panel (Firebase):");
            root.addView(l);
            final EditText et = new EditText(this);
            et.setHint("https://xxxx-default-rtdb.firebaseio.com");
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
            root.addView(et);
            Button save = new Button(this);
            save.setText("Guardar");
            save.setAllCaps(false);
            save.setOnClickListener(v -> {
                String s = et.getText().toString().trim();
                if (s.startsWith("https://")) {
                    Config.setServer(this, s);
                    Policy.setup(this);
                    recreate();
                } else {
                    et.setError("Tiene que empezar con https://");
                }
            });
            root.addView(save);
        }

        refresh();
        return sv;
    }

    private void refresh() {
        String last = Config.lastOk(this) > 0
                ? new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(Config.lastOk(this)))
                : "nunca";
        String err = Config.lastError(this);
        String imei2 = Device.imei(this, 1);
        boolean esTel = getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
        StringBuilder sb = new StringBuilder();
        sb.append(esTel ? "IMEI: " : "ID del equipo: ").append(Device.id(this)).append('\n');
        if (imei2.length() > 0) sb.append("IMEI 2: ").append(imei2).append('\n');
        sb.append("Protección activa: ").append(Policy.isOwner(this) ? "Sí" : "NO (instalar con QR)").append('\n');
        sb.append("Estado: ").append(Config.released(this) ? "Liberado" : (Config.locked(this) ? "Bloqueado" : "Habilitado")).append('\n');
        sb.append("Última verificación: ").append(last).append('\n');
        if (err.length() > 0) sb.append("Aviso: ").append(err).append('\n');
        sb.append("Panel: ").append(Config.server(this).length() > 0 ? "configurado" : "sin configurar").append('\n');
        info.setText(sb.toString());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Config.locked(this) && !Config.released(this)) Policy.showLockScreen(this);
        if (info != null) refresh();
    }
}
