package com.khalilxmodz.port;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;

public class SplashActivity extends Activity {
    static final String TEXT = "By KhalilXmodz.Port";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ValueAnimator anim;

    static class SplashView extends View {
        float t;
        final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG), line = new Paint(), vig = new Paint();
        SplashView(Context c) {
            super(c);
            text.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            text.setTextAlign(Paint.Align.CENTER);
            text.setColor(0xFFE0602A);
            line.setColor(0xFF000000);
        }
        @Override protected void onDraw(Canvas c) {
            int w = getWidth(), h = getHeight();
            c.drawColor(0xFF0A0A0A);
            float u = Math.min(w, h);
            text.setTextSize(u * 0.085f);
            float flicker = 0.82f + 0.18f * (float) Math.sin(t * 40f) * (float) Math.sin(t * 7f);
            float fade = Math.min(1f, t * 3f);
            text.setAlpha((int) (255 * flicker * fade));
            text.setShadowLayer(u * 0.02f, 0, 0, 0xAAE0602A);
            c.drawText(TEXT, w / 2f, h / 2f + u * 0.03f, text);
            line.setAlpha(60);
            for (int y = 0; y < h; y += 4) c.drawRect(0, y, w, y + 1.5f, line);
            vig.setShader(new RadialGradient(w / 2f, h / 2f, Math.max(w, h) * 0.7f,
                    0x00000000, 0xCC000000, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, vig);
        }
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        final SplashView v = new SplashView(this);
        setContentView(v);
        anim = ValueAnimator.ofFloat(0f, 1.8f);
        anim.setDuration(1800);
        anim.addUpdateListener(a -> { v.t = (Float) a.getAnimatedValue(); v.invalidate(); });
        anim.start();
        handler.postDelayed(() -> {
            startActivity(new Intent(this, LauncherActivity.class));
            finish();
        }, 1800);
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (anim != null) anim.cancel();
        super.onDestroy();
    }
}
