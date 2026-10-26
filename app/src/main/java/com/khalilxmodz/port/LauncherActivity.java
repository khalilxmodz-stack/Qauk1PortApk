package com.khalilxmodz.port;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LauncherActivity extends Activity {
    static final int REQ_IMPORT = 21;
    private TextView status;
    private Button play;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Prefs.init(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(0xFF0A0A0A);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("QUAKE  -  KhalilXmodz.Port");
        title.setTextColor(0xFFE0602A);
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        status = new TextView(this);
        status.setTextColor(0xFFCCCCCC);
        status.setTextSize(14);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, pad / 2, 0, pad / 2);
        root.addView(status);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        play = btn("PLAY", v -> startActivity(new Intent(this, GameActivity.class)));
        row.addView(play);
        row.addView(btn("IMPORT GAME DATA", v -> pickFiles()));
        row.addView(btn("SETTINGS", v -> startActivity(new Intent(this, SettingsActivity.class))));
        root.addView(row);
        setContentView(root);
    }

    private Button btn(String t, View.OnClickListener l) {
        Button x = new Button(this);
        x.setText(t);
        x.setOnClickListener(l);
        return x;
    }

    @Override protected void onResume() { super.onResume(); refresh(); }

    private void refresh() {
        String problem = GameData.check(this);
        play.setEnabled(problem == null);
        status.setText(problem == null
                ? "Game data OK. Start map: " + Prefs.s("start_map") : problem);
    }

    private void pickFiles() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(i, REQ_IMPORT);
    }

    @Override public void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_IMPORT || res != RESULT_OK || data == null) return;
        final List<Uri> uris = new ArrayList<>();
        ClipData cd = data.getClipData();
        if (cd != null) for (int i = 0; i < cd.getItemCount(); i++) uris.add(cd.getItemAt(i).getUri());
        else if (data.getData() != null) uris.add(data.getData());
        status.setText("Importing " + uris.size() + " file(s)...");
        new Thread(() -> {
            final StringBuilder log = new StringBuilder();
            for (Uri u : uris) log.append(importOne(u)).append('\n');
            runOnUiThread(() -> { refresh(); status.setText(log + "\n" + status.getText()); });
        }).start();
    }

    private String name(Uri u) {
        try (Cursor c = getContentResolver().query(u, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (i >= 0) return c.getString(i);
            }
        } catch (Exception ignored) { }
        return null;
    }

    private String importOne(Uri u) {
        String n = name(u);
        if (n == null) return "Skipped: unreadable file";
        String low = n.toLowerCase(Locale.US);
        File dir;
        if (low.matches("pak\\d+\\.pak")) dir = GameData.id1(this);
        else if (low.endsWith(".bsp")) dir = new File(GameData.id1(this), "maps");
        else return "Skipped " + n + " (only pakN.pak and .bsp files are accepted)";
        dir.mkdirs();
        File out = new File(dir, low);
        try (InputStream in = getContentResolver().openInputStream(u);
             OutputStream os = new FileOutputStream(out)) {
            byte[] buf = new byte[1 << 16];
            int r;
            while ((r = in.read(buf)) > 0) os.write(buf, 0, r);
        } catch (Exception e) {
            out.delete();
            return "FAILED " + n + ": " + e.getMessage();
        }
        if (low.endsWith(".pak") && !GameData.validPak(out)) {
            out.delete();
            return "REJECTED " + n + ": not a valid PAK file";
        }
        return "Imported " + low + " (" + out.length() / 1024 + " KB)";
    }
}
