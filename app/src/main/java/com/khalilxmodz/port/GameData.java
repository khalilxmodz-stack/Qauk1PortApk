package com.khalilxmodz.port;

import android.content.Context;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/** Locates and validates user-supplied Quake data (never bundled). */
public final class GameData {
    private GameData() {}

    public static File base(Context c) { return new File(c.getFilesDir(), "quake"); }
    public static File id1(Context c) { return new File(base(c), "id1"); }

    public static boolean validPak(File f) {
        try (RandomAccessFile r = new RandomAccessFile(f, "r")) {
            byte[] h = new byte[12];
            r.readFully(h);
            return h[0] == 'P' && h[1] == 'A' && h[2] == 'C' && h[3] == 'K';
        } catch (IOException e) { return false; }
    }

    private static int le(byte[] b, int o) {
        return (b[o] & 255) | (b[o + 1] & 255) << 8 | (b[o + 2] & 255) << 16 | (b[o + 3] & 255) << 24;
    }

    static boolean pakHas(File pak, String name) {
        try (RandomAccessFile r = new RandomAccessFile(pak, "r")) {
            byte[] h = new byte[12];
            r.readFully(h);
            if (h[0] != 'P' || h[1] != 'A' || h[2] != 'C' || h[3] != 'K') return false;
            int ofs = le(h, 4), len = le(h, 8);
            if (len <= 0 || ofs < 12 || len % 64 != 0 || len > 4 * 1024 * 1024) return false;
            byte[] d = new byte[len];
            r.seek(ofs);
            r.readFully(d);
            for (int i = 0; i < len; i += 64) {
                int e = i;
                while (e < i + 56 && d[e] != 0) e++;
                if (new String(d, i, e - i, "US-ASCII").equalsIgnoreCase(name)) return true;
            }
        } catch (IOException e) { /* treated as missing */ }
        return false;
    }

    public static boolean mapExists(Context c, String map) {
        String rel = "maps/" + map + ".bsp";
        if (new File(id1(c), rel).isFile()) return true;
        File[] files = id1(c).listFiles();
        if (files == null) return false;
        for (File f : files) {
            if (f.getName().toLowerCase().endsWith(".pak") && pakHas(f, rel)) return true;
        }
        return false;
    }

    /** @return null when playable, otherwise a human-readable explanation. */
    public static String check(Context c) {
        File pak0 = new File(id1(c), "pak0.pak");
        String where = id1(c).getAbsolutePath();
        if (!pak0.isFile())
            return "Quake game data not found.\n\nRequired: pak0.pak (from the shareware or full game, folder \"id1\").\n"
                 + "Tap IMPORT GAME DATA and pick pak0.pak (and pak1.pak if you own the full game).\n\nExpected location:\n" + where;
        if (!validPak(pak0))
            return "pak0.pak exists but is not a valid Quake PAK file (bad header). Re-import a good copy.";
        String map = Prefs.s("start_map");
        if (!mapExists(c, map))
            return "Start map \"" + map + "\" was not found in your game data.\nShareware only contains e1m1-e1m7 and start.\nChange the map in Settings.";
        return null;
    }
}
