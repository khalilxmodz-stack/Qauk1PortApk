package com.khalilxmodz.port;

import android.content.Intent;
import android.os.Bundle;
import android.system.ErrnoException;
import android.system.Os;
import android.util.DisplayMetrics;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import java.util.Locale;
import org.libsdl.app.SDLActivity;

/** Hosts SDL + QuakeSpasm (libmain.so) and the multi-touch overlay. */
public class GameActivity extends SDLActivity implements TouchOverlay.Host {
    static final int REQ_SETTINGS = 77;
    private TouchOverlay overlay;

    @Override protected String[] getLibraries() { return new String[]{"SDL2", "GL", "main"}; }

    @Override protected String[] getArguments() {
        String map = Prefs.s("start_map").replaceAll("[^A-Za-z0-9_]", "");
        return new String[]{"-basedir", GameData.base(this).getAbsolutePath(), "-condebug", "+map", map};
    }

    @Override protected void onCreate(Bundle b) {
        Prefs.init(this);
        try {   // gl4es configuration, read when libGL.so is loaded by SDLActivity.onCreate
            Os.setenv("LIBGL_ES", "2", true);
            Os.setenv("LIBGL_GL", "21", true);
            Os.setenv("LIBGL_NOBANNER", "1", true);
        } catch (ErrnoException ignored) { }
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        int[] res = applyRenderScale();
        overlay = new TouchOverlay(this);
        overlay.setHost(this);
        overlay.setRes(res[0], res[1], Prefs.f("render_scale"));
        addContentView(overlay, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        applySettings();
    }

    /** Lower internal render resolution by fixing the surface size; the compositor upscales. */
    private int[] applyRenderScale() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float s = Math.max(0.4f, Math.min(1f, Prefs.f("render_scale")));
        int w = Math.round(Math.max(dm.widthPixels, dm.heightPixels) * s) & ~1;
        int h = Math.round(Math.min(dm.widthPixels, dm.heightPixels) * s) & ~1;
        if (mLayout != null) {
            for (int i = 0; i < mLayout.getChildCount(); i++) {
                View v = mLayout.getChildAt(i);
                if (v instanceof SurfaceView) ((SurfaceView) v).getHolder().setFixedSize(w, h);
            }
        }
        return new int[]{w, h};
    }

    private static String fmt(float v) { return String.format(Locale.US, "%.3f", v); }

    /** Every value below maps to a real QuakeSpasm cvar; no placeholder settings. */
    void applySettings() {
        NativeBridge.command("fov " + Math.round(Prefs.f("fov")));
        NativeBridge.command("gamma " + fmt(Prefs.f("gamma")));
        NativeBridge.command("contrast " + fmt(Prefs.f("contrast")));
        NativeBridge.command("volume " + (Prefs.b("mute") ? "0" : fmt(Prefs.f("vol"))));
        NativeBridge.command("bgmvolume " + fmt(Prefs.f("bgm")));
        NativeBridge.command("gl_texturemode " + Prefs.FILTER_CVAR[Prefs.i("filter")]);
        NativeBridge.command("r_dynamic " + (Prefs.b("dynamic") ? 1 : 0));
        NativeBridge.command("r_particles " + (Prefs.b("particles") ? 1 : 0));
        int cap = Prefs.FPS_VALUES[Prefs.i("fps_cap")];
        NativeBridge.command("host_maxfps " + (cap == 0 ? 1000 : cap));
        String hs = fmt(Prefs.f("hud_scale"));
        NativeBridge.command("scr_sbarscale " + hs);
        NativeBridge.command("scr_menuscale " + hs);
        NativeBridge.command("scr_conscale " + hs);
        NativeBridge.command("crosshair " + (Prefs.b("crosshair") ? 1 : 0));
        NativeBridge.command("scr_crosshairscale " + Math.round(Prefs.f("crosshair_scale")));
    }

    @Override public void openSettings() {
        startActivityForResult(new Intent(this, SettingsActivity.class).putExtra("ingame", true), REQ_SETTINGS);
    }

    @Override public void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_SETTINGS) return;
        applySettings();
        String a = data == null ? null : data.getStringExtra("action");
        if ("edit".equals(a)) overlay.setEdit(true);
        else if ("restart".equals(a)) NativeBridge.command("restart");
        else if ("menu".equals(a)) { NativeBridge.command("disconnect"); NativeBridge.command("togglemenu"); }
    }

    @Override public void onBackPressed() {
        NativeBridge.key(NativeBridge.KEY_ESC, true);
        NativeBridge.key(NativeBridge.KEY_ESC, false);
    }

    @Override protected void onPause() {
        if (overlay != null) overlay.releaseAll();   // no stuck movement/fire after backgrounding
        NativeBridge.reset();
        super.onPause();
    }
}
