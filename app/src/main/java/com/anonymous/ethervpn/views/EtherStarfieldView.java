package com.anonymous.ethervpn.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws a deterministic starfield matching the design's Starfield component.
 * Stars are computed once from a fixed seed and density value.
 * ~15% of stars have a slow twinkle animation.
 */
public class EtherStarfieldView extends View {

    private static final int SEED = 7;
    private static final int STAR_COUNT = 80;
    private static final long TWINKLE_DURATION_BASE_MS = 2000;

    private final List<Star> stars = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator twinkleAnimator;
    private float twinklePhase = 0f;

    public EtherStarfieldView(Context context) {
        super(context);
        init();
    }

    public EtherStarfieldView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EtherStarfieldView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        generateStars();
        paint.setColor(0xFFFFFFFF);
    }

    private void generateStars() {
        int s = SEED;
        for (int i = 0; i < STAR_COUNT; i++) {
            s = (s * 9301 + 49297) % 233280;
            float x = (s / 233280f);
            s = (s * 9301 + 49297) % 233280;
            float y = (s / 233280f);
            s = (s * 9301 + 49297) % 233280;
            float r = 0.4f + (s / 233280f) * 1.4f;
            s = (s * 9301 + 49297) % 233280;
            float o = 0.25f + (s / 233280f) * 0.7f;
            s = (s * 9301 + 49297) % 233280;
            boolean twinkle = (s / 233280f) > 0.85f;
            stars.add(new Star(x, y, r, o, twinkle, i));
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        twinkleAnimator = ValueAnimator.ofFloat(0f, (float) (Math.PI * 2));
        twinkleAnimator.setDuration(5000);
        twinkleAnimator.setRepeatCount(ValueAnimator.INFINITE);
        twinkleAnimator.setInterpolator(new LinearInterpolator());
        twinkleAnimator.addUpdateListener(a -> {
            twinklePhase = (float) a.getAnimatedValue();
            invalidate();
        });
        twinkleAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (twinkleAnimator != null) {
            twinkleAnimator.cancel();
            twinkleAnimator = null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        for (Star st : stars) {
            float alpha;
            if (st.twinkle) {
                // phase offset per star to desync them
                float phase = twinklePhase + st.index * 0.4f;
                float factor = (float) (0.5f + 0.5f * Math.sin(phase));
                alpha = st.opacity * (0.2f + 0.8f * factor);
            } else {
                alpha = st.opacity;
            }
            paint.setAlpha(Math.round(alpha * 255));
            float radius = st.radius * 0.006f * Math.min(w, h);
            canvas.drawCircle(st.x * w, st.y * h, radius, paint);
        }
    }

    private static class Star {
        final float x, y, radius, opacity;
        final boolean twinkle;
        final int index;

        Star(float x, float y, float radius, float opacity, boolean twinkle, int index) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.opacity = opacity;
            this.twinkle = twinkle;
            this.index = index;
        }
    }
}
