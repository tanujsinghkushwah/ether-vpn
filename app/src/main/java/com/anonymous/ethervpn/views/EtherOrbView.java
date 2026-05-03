package com.anonymous.ethervpn.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * Ether connection orb visualizer.
 *
 * States:
 *   IDLE        — static ring, lock icon glyph, no animation
 *   CONNECTING  — pulsing rings + spinning dash arc
 *   CONNECTED   — full ring (emerald), check-mark glyph
 *
 * Colors match the design's violet accent (idle/connecting) and emerald (connected).
 */
public class EtherOrbView extends View {

    public enum State { IDLE, CONNECTING, CONNECTED }

    // Violet accent — oklch(0.70 0.20 280) ≈ #B98AFF
    private static final int COLOR_ACCENT = 0xFFB98AFF;
    // Emerald connected — oklch(0.70 0.18 155) ≈ #4ADE80
    private static final int COLOR_CONNECTED = 0xFF4ADE80;

    private State state = State.IDLE;

    // Animators
    private ValueAnimator spinAnimator;
    private ValueAnimator pulseAnimator;
    private float spinAngle = 0f;
    private float pulseScale = 0f; // 0→1 drives the three pulsing ring opacities

    // Paints
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickMajorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint spherePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect = new RectF();

    public EtherOrbView(Context context) {
        super(context);
        init();
    }

    public EtherOrbView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EtherOrbView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(2f);

        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeWidth(1f);
        tickPaint.setColor(0x66FFFFFF);

        tickMajorPaint.setStyle(Paint.Style.STROKE);
        tickMajorPaint.setStrokeWidth(1.5f);

        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(2f);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        spherePaint.setStyle(Paint.Style.FILL);

        glowPaint.setStyle(Paint.Style.FILL);
        glowPaint.setMaskFilter(new android.graphics.BlurMaskFilter(40f, android.graphics.BlurMaskFilter.Blur.NORMAL));

        iconPaint.setStyle(Paint.Style.STROKE);
        iconPaint.setStrokeWidth(2.5f);
        iconPaint.setStrokeCap(Paint.Cap.ROUND);
        iconPaint.setStrokeJoin(Paint.Join.ROUND);
        iconPaint.setAntiAlias(true);
    }

    public void setState(State newState) {
        if (this.state == newState) return;
        this.state = newState;
        stopAnimators();
        if (newState == State.CONNECTING) {
            startConnectingAnimators();
        } else if (newState == State.CONNECTED) {
            startConnectedAnimators();
        }
        invalidate();
    }

    public State getState() {
        return state;
    }

    private void startConnectingAnimators() {
        spinAnimator = ValueAnimator.ofFloat(0f, 360f);
        spinAnimator.setDuration(1600);
        spinAnimator.setRepeatCount(ValueAnimator.INFINITE);
        spinAnimator.setInterpolator(new LinearInterpolator());
        spinAnimator.addUpdateListener(a -> {
            spinAngle = (float) a.getAnimatedValue();
            invalidate();
        });
        spinAnimator.start();

        pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulseAnimator.setDuration(2400);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setInterpolator(new LinearInterpolator());
        pulseAnimator.addUpdateListener(a -> {
            pulseScale = (float) a.getAnimatedValue();
            invalidate();
        });
        pulseAnimator.start();
    }

    private void startConnectedAnimators() {
        // Gentle idle pulse for connected state
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulseAnimator.setDuration(3000);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setInterpolator(new LinearInterpolator());
        pulseAnimator.addUpdateListener(a -> {
            pulseScale = (float) a.getAnimatedValue();
            invalidate();
        });
        pulseAnimator.start();
    }

    private void stopAnimators() {
        if (spinAnimator != null) { spinAnimator.cancel(); spinAnimator = null; }
        if (pulseAnimator != null) { pulseAnimator.cancel(); pulseAnimator = null; }
        spinAngle = 0f;
        pulseScale = 0f;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (state == State.CONNECTING) startConnectingAnimators();
        else if (state == State.CONNECTED) startConnectedAnimators();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimators();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = Math.min(w, h) / 2f * 0.9f;

        int ringColor = (state == State.CONNECTED) ? COLOR_CONNECTED : COLOR_ACCENT;

        // ── Outer halo glow ──────────────────────────────────────────────
        glowPaint.setShader(new RadialGradient(cx, cy, radius * 1.3f,
                new int[]{ addAlpha(ringColor, 0.2f), Color.TRANSPARENT },
                new float[]{ 0f, 1f }, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, radius * 1.3f, glowPaint);

        // ── Pulsing rings (3 rings offset by 0.8s each) ──────────────────
        if (state != State.IDLE) {
            for (int i = 0; i < 3; i++) {
                float delay = i * (1f / 3f);
                float p = (pulseScale + delay) % 1f;
                // scale: 0.6→1.4, opacity: 0.7→0
                float scale = 0.6f + p * 0.8f;
                float alpha = 0.7f * (1f - p);
                ringPaint.setColor(addAlpha(ringColor, alpha));
                float r = radius * scale;
                canvas.drawCircle(cx, cy, r, ringPaint);
            }
        }

        // ── 60 tick marks ────────────────────────────────────────────────
        float innerR = radius * 0.88f;
        float outerR = radius * 0.96f;
        for (int i = 0; i < 60; i++) {
            double angle = (i / 60.0) * Math.PI * 2 - Math.PI / 2;
            float x1 = cx + (float) Math.cos(angle) * innerR;
            float y1 = cy + (float) Math.sin(angle) * innerR;
            float x2 = cx + (float) Math.cos(angle) * outerR;
            float y2 = cy + (float) Math.sin(angle) * outerR;
            boolean major = (i % 5 == 0);
            if (major) {
                tickMajorPaint.setColor(addAlpha(ringColor, 0.7f));
                canvas.drawLine(x1, y1, x2, y2, tickMajorPaint);
            } else {
                canvas.drawLine(x1, y1, x2, y2, tickPaint);
            }
        }

        // ── Progress arc (connecting) or full ring (connected) ───────────
        float arcR = radius * 0.82f;
        arcRect.set(cx - arcR, cy - arcR, cx + arcR, cy + arcR);
        arcPaint.setColor(ringColor);

        if (state == State.CONNECTING) {
            arcPaint.setPathEffect(new DashPathEffect(
                    new float[]{ arcR * 0.5f, arcR * 2.8f }, 0));
            canvas.save();
            canvas.rotate(spinAngle - 90, cx, cy);
            canvas.drawArc(arcRect, 0, 360, false, arcPaint);
            canvas.restore();
        } else if (state == State.CONNECTED) {
            arcPaint.setPathEffect(null);
            arcPaint.setAlpha(Math.round(0.6f * 255));
            canvas.drawArc(arcRect, -90, 360, false, arcPaint);
            arcPaint.setAlpha(255);
        }

        // ── Core sphere ──────────────────────────────────────────────────
        float sphereR = radius * 0.58f;
        if (state == State.IDLE) {
            spherePaint.setShader(new RadialGradient(
                    cx - sphereR * 0.35f, cy - sphereR * 0.3f, sphereR,
                    new int[]{ 0x10FFFFFF, 0x05FFFFFF, Color.TRANSPARENT },
                    new float[]{ 0f, 0.5f, 1f }, Shader.TileMode.CLAMP));
        } else {
            spherePaint.setShader(new RadialGradient(
                    cx - sphereR * 0.35f, cy - sphereR * 0.3f, sphereR,
                    new int[]{ addAlpha(ringColor, 0.33f), addAlpha(ringColor, 0.13f), Color.TRANSPARENT },
                    new float[]{ 0f, 0.5f, 1f }, Shader.TileMode.CLAMP));
        }
        canvas.drawCircle(cx, cy, sphereR, spherePaint);

        // Sphere border
        ringPaint.setColor(state == State.IDLE ? 0x14FFFFFF : addAlpha(ringColor, 0.4f));
        ringPaint.setStrokeWidth(1f);
        canvas.drawCircle(cx, cy, sphereR, ringPaint);
        ringPaint.setStrokeWidth(2f);

        // ── Center icon ──────────────────────────────────────────────────
        float iconSize = sphereR * 0.35f;
        if (state == State.CONNECTED) {
            // Check mark
            iconPaint.setColor(COLOR_CONNECTED);
            canvas.drawLine(cx - iconSize, cy, cx - iconSize * 0.2f, cy + iconSize * 0.7f, iconPaint);
            canvas.drawLine(cx - iconSize * 0.2f, cy + iconSize * 0.7f, cx + iconSize, cy - iconSize * 0.5f, iconPaint);
        } else {
            // Lock body
            iconPaint.setColor(0x99E8ECF4);
            float lw = iconSize * 0.55f;
            float lh = iconSize * 0.45f;
            float lt = cy;
            // shackle
            canvas.drawLine(cx - lw * 0.5f, lt, cx - lw * 0.5f, cy - lh * 0.8f, iconPaint);
            canvas.drawLine(cx + lw * 0.5f, lt, cx + lw * 0.5f, cy - lh * 0.8f, iconPaint);
            canvas.drawArc(new RectF(cx - lw * 0.5f, cy - lh * 1.6f, cx + lw * 0.5f, cy - lh * 0.8f),
                    180, -180, false, iconPaint);
            // body rectangle
            RectF bodyRect = new RectF(cx - lw * 0.7f, lt, cx + lw * 0.7f, cy + lh);
            iconPaint.setStyle(Paint.Style.STROKE);
            canvas.drawRoundRect(bodyRect, 4f, 4f, iconPaint);
        }
    }

    /** Blend alpha (0..1) into an ARGB color. */
    private static int addAlpha(int color, float alpha) {
        return (color & 0x00FFFFFF) | (Math.round(alpha * 255) << 24);
    }
}
