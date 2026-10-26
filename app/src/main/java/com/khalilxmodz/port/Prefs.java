package com.khalilxmodz.port;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashMap;
import java.util.Map;

/** All persistent settings. Defaults live in one place (DEF). */
public final class Prefs {
    private static SharedPreferences sp;
    private static final Map<String, Object> DEF = new HashMap<>();
    static {
        DEF.put("sens_x", 1.0f);      DEF.put("sens_y", 1.0f);
        DEF.put("invert_y", false);   DEF.put("dead_zone", 0.12f);
        DEF.put("floating", false);   DEF.put("c_action", 0);
        DEF.put("joy_op", 0.5f);      DEF.put("btn_op", 0.5f);
        DEF.put("hud_visible", true); DEF.put("show_overlay", true);
        DEF.put("hud_scale", 3.0f);   DEF.put("crosshair", true);
        DEF.put("crosshair_scale", 2.0f);
        DEF.put("preset", 1);         DEF.put("render_scale", 0.8f);
        DEF.put("filter", 1);         DEF.put("dynamic", true);
        DEF.put("particles", true);   DEF.put("fps_cap", 2);
        DEF.put("fov", 90.0f);        DEF.put("gamma", 1.0f);
        DEF.put("contrast", 1.0f);
        DEF.put("vol", 0.7f);         DEF.put("bgm", 0.5f);
        DEF.put("mute", false);
        DEF.put("start_map", "e1m1");
    }

    public static final String[] PRESET_NAMES = {"Performance", "Balanced", "Quality", "Custom"};
    public static final String[] FILTER_NAMES = {"Nearest (fastest)", "Bilinear", "Trilinear"};
    public static final String[] FILTER_CVAR = {"GL_NEAREST_MIPMAP_NEAREST", "GL_LINEAR_MIPMAP_NEAREST", "GL_LINEAR_MIPMAP_LINEAR"};
    public static final String[] FPS_NAMES = {"30", "45", "60", "Unlimited"};
    public static final int[] FPS_VALUES = {30, 45, 60, 0};

    public static void init(Context c) {
        if (sp == null) sp = c.getApplicationContext().getSharedPreferences("khalilxmodz_port", Context.MODE_PRIVATE);
    }
    public static float f(String k) { return sp.getFloat(k, (Float) DEF.get(k)); }
    public static int i(String k) { return sp.getInt(k, (Integer) DEF.get(k)); }
    public static boolean b(String k) { return sp.getBoolean(k, (Boolean) DEF.get(k)); }
    public static String s(String k) { return sp.getString(k, (String) DEF.get(k)); }
    public static void putF(String k, float v) { sp.edit().putFloat(k, v).apply(); }
    public static void putI(String k, int v) { sp.edit().putInt(k, v).apply(); }
    public static void putB(String k, boolean v) { sp.edit().putBoolean(k, v).apply(); }
    public static void putS(String k, String v) { sp.edit().putString(k, v).apply(); }
    public static float raw(String k, float d) { return sp.getFloat(k, d); }
    public static void remove(String k) { sp.edit().remove(k).apply(); }
    public static void resetAll() { sp.edit().clear().apply(); }

    public static void applyPreset(int p) {
        float[] scale = {0.6f, 0.8f, 1.0f};
        boolean[] dyn = {false, true, true};
        boolean[] par = {false, true, true};
        putF("render_scale", scale[p]); putI("filter", p);
        putB("dynamic", dyn[p]); putB("particles", par[p]);
        putI("fps_cap", 2); putI("preset", p);
    }
}
