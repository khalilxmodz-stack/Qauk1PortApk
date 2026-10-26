package com.khalilxmodz.port;

/** JNI bridge to android_input.c (lives inside libmain.so together with QuakeSpasm). */
public final class NativeBridge {
    private NativeBridge() {}
    public static volatile boolean broken = false;
    public static final int KEY_UP = 0, KEY_DOWN = 1, KEY_LEFT = 2, KEY_RIGHT = 3, KEY_ENTER = 4, KEY_ESC = 5;

    private static native void nativeSetMove(float fwd, float side);
    private static native void nativeAddLook(float dx, float dy);
    private static native void nativeCommand(String cmd);
    private static native void nativeKey(int key, boolean down);
    private static native void nativeReset();
    private static native int nativeInGame();
    private static native float[] nativeStats();
    private static native int[] nativeHud();

    public static void move(float f, float s) { if (!broken) try { nativeSetMove(f, s); } catch (UnsatisfiedLinkError e) { broken = true; } }
    public static void look(float dx, float dy) { if (!broken) try { nativeAddLook(dx, dy); } catch (UnsatisfiedLinkError e) { broken = true; } }
    public static void command(String c) { if (!broken) try { nativeCommand(c); } catch (UnsatisfiedLinkError e) { broken = true; } }
    public static void key(int k, boolean down) { if (!broken) try { nativeKey(k, down); } catch (UnsatisfiedLinkError e) { broken = true; } }
    public static void reset() { if (!broken) try { nativeReset(); } catch (UnsatisfiedLinkError e) { broken = true; } }
    public static boolean inGame() { if (broken) return false; try { return nativeInGame() != 0; } catch (UnsatisfiedLinkError e) { broken = true; return false; } }
    public static float[] stats() { if (broken) return null; try { return nativeStats(); } catch (UnsatisfiedLinkError e) { broken = true; return null; } }
    public static int[] hud() { if (broken) return null; try { return nativeHud(); } catch (UnsatisfiedLinkError e) { broken = true; return null; } }
}
