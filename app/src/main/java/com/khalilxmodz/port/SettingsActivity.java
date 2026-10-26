package com.khalilxmodz.port;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import java.util.Locale;

/** Settings are saved the moment they change; the game applies them when this screen closes. */
public class SettingsActivity extends Activity {
    private LinearLayout root;
    private boolean inGame;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Prefs.init(this);
        inGame = getIntent().getBooleanExtra("ingame", false);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(0xFF101010);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(p, p, p, p);
        sv.addView(root);
        setContentView(sv);
        build();
    }

    private void build() {
        root.removeAllViews();
        header("Graphics");
        LinearLayout pr = new LinearLayout(this);
        for (int i = 0; i < 3; i++) {
            final int preset = i;
            Button bt = new Button(this);
            bt.setText(Prefs.PRESET_NAMES[i]);
            bt.setOnClickListener(v -> { Prefs.applyPreset(preset); recreate(); });
            pr.addView(bt);
        }
        root.addView(pr);
        text("Current preset: " + Prefs.PRESET_NAMES[Prefs.i("preset")]);
        slider("Render resolution scale (applies on next launch)", "render_scale", 0.4f, 1f, true, "%.2f");
        choice("Texture filtering", "filter", Prefs.FILTER_NAMES, true);
        toggle("Dynamic lighting", "dynamic", true);
        toggle("Particle effects", "particles", true);
        choice("FPS cap", "fps_cap", Prefs.FPS_NAMES, true);
        slider("Field of view", "fov", 60f, 120f, false, "%.0f");
        slider("Gamma (lower = brighter)", "gamma", 0.5f, 1f, false, "%.2f");
        slider("Contrast", "contrast", 1f, 2f, false, "%.2f");

        header("HUD");
        toggle("Show touch controls", "hud_visible", false);
        toggle("Performance overlay", "show_overlay", false);
        slider("Status bar / menu scale", "hud_scale", 1f, 6f, false, "%.1f");
        toggle("Crosshair", "crosshair", false);
        slider("Crosshair size", "crosshair_scale", 1f, 5f, false, "%.0f");
        slider("Joystick opacity", "joy_op", 0.1f, 1f, false, "%.2f");
        slider("Button opacity", "btn_op", 0.1f, 1f, false, "%.2f");
        if (inGame) button("Edit HUD layout (move / resize buttons)", () -> finishWith("edit"));

        header("Controls");
        slider("Look sensitivity - horizontal", "sens_x", 0.2f, 4f, false, "%.2f");
        slider("Look sensitivity - vertical", "sens_y", 0.2f, 4f, false, "%.2f");
        toggle("Invert vertical look", "invert_y", false);
        slider("Joystick dead zone", "dead_zone", 0f, 0.5f, false, "%.2f");
        toggle("Floating left joystick", "floating", false);
        choice("Button C action", "c_action", TouchOverlay.C_NAMES, false);

        header("Audio");
        slider("Master volume", "vol", 0f, 1f, false, "%.2f");
        slider("Music volume (needs CD/music tracks; none bundled)", "bgm", 0f, 1f, false, "%.2f");
        toggle("Mute", "mute", false);

        header("Game");
        text("Start map (e.g. e1m1):");
        EditText et = new EditText(this);
        et.setText(Prefs.s("start_map"));
        et.setTextColor(0xFFFFFFFF);
        et.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            public void onTextChanged(CharSequence s, int a, int b, int c) { }
            public void afterTextChanged(Editable e) {
                String m = e.toString().replaceAll("[^A-Za-z0-9_]", "");
                if (!m.isEmpty()) Prefs.putS("start_map", m);
            }
        });
        root.addView(et);
        if (inGame) {
            button("Restart current map", () -> finishWith("restart"));
            button("Return to main menu", () -> finishWith("menu"));
        }
        button("Reset all settings", () -> { Prefs.resetAll(); recreate(); });
        button("Done", () -> finishWith(null));
    }

    private void finishWith(String action) {
        Intent i = new Intent();
        if (action != null) i.putExtra("action", action);
        setResult(RESULT_OK, i);
        finish();
    }

    @Override public void onBackPressed() { finishWith(null); }

    private TextView text(String s) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextColor(0xFFCCCCCC); t.setTextSize(14);
        root.addView(t);
        return t;
    }
    private void header(String s) {
        TextView t = text(s);
        t.setTextColor(0xFFE0602A); t.setTextSize(20); t.setPadding(0, 32, 0, 8);
    }
    private void button(String label, final Runnable r) {
        Button bt = new Button(this);
        bt.setText(label);
        bt.setOnClickListener(v -> r.run());
        root.addView(bt);
    }
    private void slider(final String label, final String key, final float min, final float max,
                        final boolean gfx, final String fmt) {
        final TextView tv = text(label + ": " + String.format(Locale.US, fmt, Prefs.f(key)));
        SeekBar sb = new SeekBar(this);
        sb.setMax(1000);
        sb.setProgress(Math.round((Prefs.f(key) - min) / (max - min) * 1000f));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                if (!user) return;
                float v = min + (max - min) * p / 1000f;
                Prefs.putF(key, v);
                if (gfx) Prefs.putI("preset", 3);
                tv.setText(label + ": " + String.format(Locale.US, fmt, v));
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        });
        root.addView(sb);
    }
    private void toggle(String label, final String key, final boolean gfx) {
        Switch sw = new Switch(this);
        sw.setText(label); sw.setTextColor(0xFFCCCCCC);
        sw.setChecked(Prefs.b(key));
        sw.setOnCheckedChangeListener((CompoundButton b, boolean on) -> {
            Prefs.putB(key, on);
            if (gfx) Prefs.putI("preset", 3);
        });
        root.addView(sw);
    }
    private void choice(String label, final String key, String[] names, final boolean gfx) {
        text(label);
        Spinner sp = new Spinner(this);
        sp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
        sp.setSelection(Prefs.i(key), false);
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> a, View v, int pos, long id) {
                if (pos == Prefs.i(key)) return;
                Prefs.putI(key, pos);
                if (gfx) Prefs.putI("preset", 3);
            }
            @Override public void onNothingSelected(AdapterView<?> a) { }
        });
        root.addView(sp);
    }
}
