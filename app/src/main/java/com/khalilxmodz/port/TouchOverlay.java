package com.khalilxmodz.port;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Debug;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.View;
import java.util.Locale;

/**
 * Multi-touch HUD. Every finger is tracked by its own pointer id, so the left stick,
 * the look area and any buttons work simultaneously. All input goes to the engine
 * through NativeBridge (console commands / usercmd / view angles).
 */
public class TouchOverlay extends View {
    public interface Host { void openSettings(); }

    static final int JOY = 0, BA = 1, BB = 2, BC = 3, BY = 4, BX = 5, BP = 6, BM = 7, BS = 8, N = 9;
    static final int NONE = -1, LOOK = -2, MENUKEY = 100;
    static final String[] ID = {"joy", "a", "b", "c", "y", "x", "pause", "menu", "set"};
    static final String[] LBL = {"", "A", "B", "C", "Y", "X", "II", "ESC", "SET"};
    static final float[][] DEF = {
            {.14f, .70f, 1f}, {.78f, .86f, 1f}, {.90f, .68f, 1.25f}, {.66f, .86f, 1f}, {.90f, .44f, 1f},
            {.78f, .50f, .9f}, {.44f, .09f, .7f}, {.52f, .09f, .7f}, {.60f, .09f, .7f}};

    public static final String[] C_NAMES = {"Swim down (+movedown)", "Swim up (+moveup)", "Select axe (impulse 1)",
            "Scoreboard (+showscores)", "Run (+speed)"};
    static final String[] C_PRESS = {"+movedown", "+moveup", "impulse 1", "+showscores", "+speed"};
    static final String[] C_REL = {"-movedown", "-moveup", null, "-showscores", "-speed"};
    static final String[] MLBL = {"^", "v", "<", ">", "OK", "ESC"};
    static final String[] TB = {"SIZE-", "SIZE+", "OPAC-", "OPAC+", "SAVE", "RESET", "DONE"};

    private Host host;
    private final float[][] lay = new float[N][3];
    private final boolean[] down = new boolean[N];
    private final SparseIntArray owner = new SparseIntArray();
    private final SparseArray<float[]> last = new SparseArray<>();

    private int joyPtr = -1;
    private float jcx, jcy, tdx, tdy;
    private boolean editMode;
    private int sel = NONE, editPtr = -1;
    private float editOx, editOy;
    private boolean inGame;
    private String statusLine = "", memText = "", editMsg = "";
    private long lastMem;
    private int resW, resH;
    private float resScale = 1f;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), stroke = new Paint(Paint.ANTI_ALIAS_FLAG),
            txt = new Paint(Paint.ANTI_ALIAS_FLAG);

    public TouchOverlay(Context c) {
        super(c);
        setWillNotDraw(false);
        stroke.setStyle(Paint.Style.STROKE);
        txt.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        txt.setTextAlign(Paint.Align.CENTER);
        loadLayout();
    }

    public void setHost(Host h) { host = h; }
    public void setRes(int w, int h, float scale) { resW = w; resH = h; resScale = scale; }

    // ---------------- layout persistence ----------------
    private void loadLayout() {
        for (int i = 0; i < N; i++) {
            lay[i][0] = Prefs.raw("x_" + ID[i], DEF[i][0]);
            lay[i][1] = Prefs.raw("y_" + ID[i], DEF[i][1]);
            lay[i][2] = Prefs.raw("s_" + ID[i], DEF[i][2]);
        }
    }
    private void saveLayout() {
        for (int i = 0; i < N; i++) {
            Prefs.putF("x_" + ID[i], lay[i][0]);
            Prefs.putF("y_" + ID[i], lay[i][1]);
            Prefs.putF("s_" + ID[i], lay[i][2]);
        }
    }
    private void resetLayout() {
        for (int i = 0; i < N; i++) {
            Prefs.remove("x_" + ID[i]); Prefs.remove("y_" + ID[i]); Prefs.remove("s_" + ID[i]);
        }
        Prefs.remove("joy_op"); Prefs.remove("btn_op");
        loadLayout();
    }

    public void setEdit(boolean on) {
        releaseAll();
        editMode = on; sel = NONE; editPtr = -1;
        editMsg = on ? "EDIT: tap an element, drag to move" : "";
        loadLayout();
        invalidate();
    }

    // ---------------- geometry ----------------
    private float unit() { return Math.min(getWidth(), getHeight()); }
    private float cx(int i) { return lay[i][0] * getWidth(); }
    private float cy(int i) { return lay[i][1] * getHeight(); }
    private float radius(int i) { return unit() * (i == JOY ? 0.17f : 0.075f) * lay[i][2]; }

    private int hit(float x, float y) {
        for (int i = N - 1; i >= 0; i--) {
            float r = radius(i) * (i == JOY ? 1.3f : 1.15f);
            float dx = x - cx(i), dy = y - cy(i);
            if (dx * dx + dy * dy <= r * r) return i;
        }
        return NONE;
    }

    private float[] menuPos(int k) {
        float w = getWidth(), h = getHeight(), u = unit(), r = u * 0.17f, bx = w * 0.17f, by = h * 0.70f;
        switch (k) {
            case 0: return new float[]{bx, by - r, u * 0.08f};
            case 1: return new float[]{bx, by + r, u * 0.08f};
            case 2: return new float[]{bx - r, by, u * 0.08f};
            case 3: return new float[]{bx + r, by, u * 0.08f};
            case 4: return new float[]{w * 0.86f, h * 0.72f, u * 0.10f};
            default: return new float[]{w * 0.74f, h * 0.84f, u * 0.08f};
        }
    }
    private int menuHit(float x, float y) {
        for (int k = 0; k < 6; k++) {
            float[] m = menuPos(k);
            float dx = x - m[0], dy = y - m[1], r = m[2] * 1.3f;
            if (dx * dx + dy * dy <= r * r) return k;
        }
        return NONE;
    }

    private float[] tbRect(int k) {
        float w = getWidth(), h = getHeight(), bw = w * 0.8f / TB.length, bh = unit() * 0.13f;
        float x0 = w * 0.1f + k * bw;
        return new float[]{x0 + 4, h - bh - 8, x0 + bw - 4, h - 8};
    }
    private int tbHit(float x, float y) {
        for (int k = 0; k < TB.length; k++) {
            float[] r = tbRect(k);
            if (x >= r[0] && x <= r[2] && y >= r[1] && y <= r[3]) return k;
        }
        return NONE;
    }

    // ---------------- actions ----------------
    private void setBtn(int e, boolean pressed) {
        if (down[e] == pressed) return;
        down[e] = pressed;
        switch (e) {
            case BA: NativeBridge.command(pressed ? "+jump" : "-jump"); break;
            case BB: NativeBridge.command(pressed ? "+attack" : "-attack"); break;
            case BC: {
                int a = Prefs.i("c_action");
                if (pressed) NativeBridge.command(C_PRESS[a]);
                else if (C_REL[a] != null) NativeBridge.command(C_REL[a]);
                break;
            }
            case BY: if (pressed) NativeBridge.command("impulse 10"); break;   // next weapon
            case BX: if (pressed) NativeBridge.command("impulse 12"); break;   // previous weapon
            case BP: if (pressed) NativeBridge.command("pause"); break;
            case BM:
                if (pressed) { NativeBridge.key(NativeBridge.KEY_ESC, true); NativeBridge.key(NativeBridge.KEY_ESC, false); }
                break;
            case BS: if (pressed && host != null) host.openSettings(); break;
            default: break;
        }
    }

    public void releaseAll() {
        for (int i = 1; i < N; i++) if (down[i]) setBtn(i, false);
        for (int i = 0; i < owner.size(); i++) {
            int o = owner.valueAt(i);
            if (o >= MENUKEY) NativeBridge.key(o - MENUKEY, false);
        }
        joyPtr = -1; tdx = tdy = 0;
        NativeBridge.move(0, 0);
        owner.clear(); last.clear(); editPtr = -1;
        postInvalidate();
    }

    private void updateJoy(float tx, float ty) {
        float r = radius(JOY), dx = tx - jcx, dy = ty - jcy;
        float m = (float) Math.hypot(dx, dy);
        if (m > r) { dx *= r / m; dy *= r / m; m = r; }
        tdx = dx; tdy = dy;
        float n = m / r, dz = Prefs.f("dead_zone");
        float k = n < dz ? 0f : (n - dz) / (1f - dz);
        float ux = m > 0 ? dx / m : 0, uy = m > 0 ? dy / m : 0;
        NativeBridge.move(-uy * k, ux * k);
    }

    // ---------------- touch ----------------
    @Override public boolean onTouchEvent(MotionEvent ev) {
        int a = ev.getActionMasked(), idx = ev.getActionIndex();
        switch (a) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                pdown(ev.getPointerId(idx), ev.getX(idx), ev.getY(idx)); break;
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < ev.getPointerCount(); i++) pmove(ev.getPointerId(i), ev.getX(i), ev.getY(i));
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                pup(ev.getPointerId(idx)); break;
            case MotionEvent.ACTION_CANCEL:
                releaseAll(); break;
            default: break;
        }
        invalidate();
        return true;
    }

    private void pdown(int id, float x, float y) {
        if (editMode) { editDown(id, x, y); return; }
        if (!inGame) {
            int k = menuHit(x, y);
            if (k != NONE) { owner.put(id, MENUKEY + k); NativeBridge.key(k, true); }
            return;
        }
        int e = hit(x, y);
        if (e == JOY) {
            if (joyPtr < 0) { joyPtr = id; owner.put(id, JOY); jcx = cx(JOY); jcy = cy(JOY); updateJoy(x, y); }
        } else if (e > JOY) {
            owner.put(id, e); setBtn(e, true);
        } else if (Prefs.b("floating") && x < getWidth() * 0.4f && joyPtr < 0) {
            joyPtr = id; owner.put(id, JOY); jcx = x; jcy = y; updateJoy(x, y);
        } else if (x >= getWidth() * 0.35f) {
            owner.put(id, LOOK); last.put(id, new float[]{x, y});
        }
    }

    private void pmove(int id, float x, float y) {
        if (editMode) {
            if (id == editPtr && sel >= 0) {
                lay[sel][0] = Math.max(0.03f, Math.min(0.97f, (x - editOx) / getWidth()));
                lay[sel][1] = Math.max(0.05f, Math.min(0.95f, (y - editOy) / getHeight()));
            }
            return;
        }
        int o = owner.get(id, NONE);
        if (o == JOY) updateJoy(x, y);
        else if (o == LOOK) {
            float[] l = last.get(id);
            if (l == null) return;
            float k = 0.15f;
            float dx = (x - l[0]) * Prefs.f("sens_x") * k;
            float dy = (y - l[1]) * Prefs.f("sens_y") * k * (Prefs.b("invert_y") ? -1f : 1f);
            l[0] = x; l[1] = y;
            NativeBridge.look(dx, dy);
        }
    }

    private void pup(int id) {
        if (editMode) { if (id == editPtr) editPtr = -1; return; }
        int o = owner.get(id, NONE);
        if (o == JOY) { joyPtr = -1; tdx = tdy = 0; NativeBridge.move(0, 0); }
        else if (o == LOOK) last.remove(id);
        else if (o >= MENUKEY) NativeBridge.key(o - MENUKEY, false);
        else if (o > JOY) setBtn(o, false);
        owner.delete(id);
    }

    private void editDown(int id, float x, float y) {
        int t = tbHit(x, y);
        if (t != NONE) {
            switch (t) {
                case 0: if (sel >= 0) lay[sel][2] = Math.max(0.4f, lay[sel][2] * 0.9f); break;
                case 1: if (sel >= 0) lay[sel][2] = Math.min(2.5f, lay[sel][2] * 1.1f); break;
                case 2: case 3: {
                    String key = sel == JOY ? "joy_op" : "btn_op";
                    float d = t == 2 ? -0.05f : 0.05f;
                    Prefs.putF(key, Math.max(0.1f, Math.min(1f, Prefs.f(key) + d)));
                    break;
                }
                case 4: saveLayout(); editMsg = "Layout saved"; break;
                case 5: resetLayout(); editMsg = "Defaults restored"; break;
                default: saveLayout(); setEdit(false); break;
            }
            return;
        }
        int e = hit(x, y);
        if (e != NONE) { sel = e; editPtr = id; editOx = x - cx(e); editOy = y - cy(e); }
    }

    // ---------------- status / stats ----------------
    private final Runnable tick = new Runnable() {
        @Override public void run() { refresh(); invalidate(); postDelayed(this, 250); }
    };
    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); post(tick); }
    @Override protected void onDetachedFromWindow() { removeCallbacks(tick); super.onDetachedFromWindow(); }

    private void refresh() {
        boolean g = NativeBridge.inGame();
        if (g != inGame) { inGame = g; releaseAll(); }
        long now = System.currentTimeMillis();
        if (now - lastMem > 1000) {
            lastMem = now;
            Debug.MemoryInfo mi = new Debug.MemoryInfo();
            Debug.getMemoryInfo(mi);
            memText = (mi.getTotalPss() / 1024) + " MB";
        }
        if (NativeBridge.broken) { statusLine = "Native engine not loaded"; return; }
        float[] s = NativeBridge.stats();
        int[] hud = NativeBridge.hud();
        if (s == null) return;
        statusLine = String.format(Locale.US,
                "FPS %.0f  avg %.1f  %.1f ms  max %.1f ms  spikes %d\nRender %dx%d (x%.2f)  Preset: %s  PSS %s",
                s[0], s[1], s[2], s[3], (int) s[4], resW, resH, resScale,
                Prefs.PRESET_NAMES[Prefs.i("preset")], memText);
        if (hud != null && inGame)
            statusLine += String.format(Locale.US, "\nHP %d  Armor %d  Ammo %d", hud[0], hud[1], hud[2]);
    }

    // ---------------- drawing ----------------
    @Override protected void onDraw(Canvas c) {
        if (editMode) { drawElements(c, true); drawToolbar(c); drawStatus(c, editMsg); return; }
        if (Prefs.b("show_overlay")) drawStatus(c, statusLine);
        if (!inGame) { drawMenuPad(c); return; }
        if (Prefs.b("hud_visible")) drawElements(c, false);
    }

    private void circle(Canvas c, float x, float y, float r, float op, boolean pressed, boolean selected) {
        fill.setColor(pressed ? 0xFF555555 : 0xFF000000);
        fill.setAlpha((int) (255 * op * 0.85f));
        c.drawCircle(x, y, r, fill);
        stroke.setStrokeWidth(Math.max(2f, r * 0.05f));
        stroke.setColor(selected ? 0xFFFFD000 : 0xFFDDDDDD);
        stroke.setAlpha((int) (255 * Math.min(1f, op + 0.3f)));
        c.drawCircle(x, y, r, stroke);
    }

    private void drawElements(Canvas c, boolean edit) {
        for (int i = 0; i < N; i++) {
            float op = i == JOY ? Prefs.f("joy_op") : Prefs.f("btn_op");
            float r = radius(i);
            if (i == JOY) {
                float bx = joyPtr >= 0 ? jcx : cx(JOY), by = joyPtr >= 0 ? jcy : cy(JOY);
                circle(c, bx, by, r, op, false, edit && sel == i);
                circle(c, bx + (joyPtr >= 0 ? tdx : 0), by + (joyPtr >= 0 ? tdy : 0), r * 0.4f, op, joyPtr >= 0, false);
            } else {
                circle(c, cx(i), cy(i), r, op, down[i], edit && sel == i);
                txt.setColor(0xFFFFFFFF);
                txt.setAlpha((int) (255 * Math.min(1f, op + 0.4f)));
                txt.setTextSize(r * 0.7f);
                c.drawText(LBL[i], cx(i), cy(i) + r * 0.25f, txt);
            }
        }
    }

    private void drawMenuPad(Canvas c) {
        for (int k = 0; k < 6; k++) {
            float[] m = menuPos(k);
            circle(c, m[0], m[1], m[2], 0.55f, false, false);
            txt.setColor(0xFFFFFFFF); txt.setAlpha(230); txt.setTextSize(m[2] * 0.8f);
            c.drawText(MLBL[k], m[0], m[1] + m[2] * 0.28f, txt);
        }
    }

    private void drawToolbar(Canvas c) {
        for (int k = 0; k < TB.length; k++) {
            float[] r = tbRect(k);
            fill.setColor(0xFF202020); fill.setAlpha(230);
            c.drawRect(r[0], r[1], r[2], r[3], fill);
            txt.setColor(0xFFFFFFFF); txt.setAlpha(255); txt.setTextSize((r[3] - r[1]) * 0.36f);
            c.drawText(TB[k], (r[0] + r[2]) / 2f, (r[1] + r[3]) / 2f + (r[3] - r[1]) * 0.12f, txt);
        }
    }

    private void drawStatus(Canvas c, String text) {
        if (text == null || text.isEmpty()) return;
        txt.setTextAlign(Paint.Align.LEFT);
        txt.setTextSize(unit() * 0.03f);
        txt.setColor(0xFFFFFFFF); txt.setAlpha(255);
        txt.setShadowLayer(3f, 1f, 1f, 0xFF000000);
        float y = unit() * 0.04f;
        for (String line : text.split("\n")) { c.drawText(line, unit() * 0.02f, y, txt); y += unit() * 0.036f; }
        txt.clearShadowLayer();
        txt.setTextAlign(Paint.Align.CENTER);
    }
}
