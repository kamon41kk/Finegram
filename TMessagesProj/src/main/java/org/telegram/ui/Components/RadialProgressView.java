/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 * MD3 Expressive loading indicator added for Finegram MD3.
 */

package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.Keep;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

public class RadialProgressView extends View {

    private long lastUpdateTime;
    private float radOffset;
    private float currentCircleLength;
    private boolean risingCircleLength;
    private float currentProgressTime;
    private RectF cicleRect = new RectF();
    private boolean useSelfAlpha;
    private float drawingCircleLenght;

    private int progressColor;

    private DecelerateInterpolator decelerateInterpolator;
    private AccelerateInterpolator accelerateInterpolator;
    private Paint progressPaint;
    private static final float rotationTime = 2000;
    private static final float risingTime = 500;
    private int size;

    private float currentProgress;
    private float progressAnimationStart;
    private int progressTime;
    private float animatedProgress;
    private boolean toCircle;
    private float toCircleProgress;

    private boolean noProgress = true;
    private final Theme.ResourcesProvider resourcesProvider;

    // ===== MD3 Expressive loading indicator =====
    public static boolean MD3_LOADER = true;

    // {lobes, amplitude, squish}; lobes == 0 -> ellipse with given squish
    private static final float[][] MD3_SHAPES = {
            {10, 0.10f, 1f},   // soft burst
            {9, 0.07f, 1f},    // cookie 9
            {5, 0.06f, 1f},    // pentagon
            {0, 0f, 0.62f},    // pill
            {8, 0.09f, 1f},    // sunny
            {4, 0.10f, 1f},    // cookie 4
            {0, 0f, 0.82f},    // oval
    };
    private static final long MD3_STEP = 650;
    private static final long MD3_MORPH = 450;
    private static final int MD3_POINTS = 96;

    private final Path md3Path = new Path();
    private final Paint md3Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final OvershootInterpolator md3Interpolator = new OvershootInterpolator(1.6f);
    private long md3Start;
    // ============================================

    public RadialProgressView(Context context) {
        this(context, null);
    }

    public RadialProgressView(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;

        size = AndroidUtilities.dp(40);

        progressColor = getThemedColor(Theme.key_progressCircle);
        decelerateInterpolator = new DecelerateInterpolator();
        accelerateInterpolator = new AccelerateInterpolator();
        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setStrokeWidth(AndroidUtilities.dp(3));
        progressPaint.setColor(progressColor);

        md3Paint.setStyle(Paint.Style.FILL);
    }

    public void setUseSelfAlpha(boolean value) {
        useSelfAlpha = value;
    }

    @Keep
    @Override
    public void setAlpha(float alpha) {
        super.setAlpha(alpha);
        if (useSelfAlpha) {
            Drawable background = getBackground();
            int a = (int) (alpha * 255);
            if (background != null) {
                background.setAlpha(a);
            }
            progressPaint.setAlpha(a);
        }
    }

    public void setNoProgress(boolean value) {
        noProgress = value;
    }

    public void setProgress(float value) {
        currentProgress = value;
        if (animatedProgress > value) {
            animatedProgress = value;
        }
        progressAnimationStart = animatedProgress;
        progressTime = 0;
    }

    public void sync(RadialProgressView from) {
        lastUpdateTime = from.lastUpdateTime;
        radOffset = from.radOffset;
        toCircle = from.toCircle;
        toCircleProgress = from.toCircleProgress;
        noProgress = from.noProgress;
        currentCircleLength = from.currentCircleLength;
        drawingCircleLenght = from.drawingCircleLenght;
        currentProgressTime = from.currentProgressTime;
        currentProgress = from.currentProgress;
        progressTime = from.progressTime;
        animatedProgress = from.animatedProgress;
        risingCircleLength = from.risingCircleLength;
        progressAnimationStart = from.progressAnimationStart;
        md3Start = from.md3Start;
        updateAnimation(17 * 5);
    }

    private void updateAnimation() {
        long newTime = System.currentTimeMillis();
        long dt = newTime - lastUpdateTime;
        if (dt > 17) {
            dt = 17;
        }
        lastUpdateTime = newTime;
        updateAnimation(dt);
    }

    private void updateAnimation(long dt) {
        radOffset += 360 * dt / rotationTime;
        int count = (int) (radOffset / 360);
        radOffset -= count * 360;

        if (toCircle && toCircleProgress != 1f) {
            toCircleProgress += 16 / 220f;
            if (toCircleProgress > 1f) {
                toCircleProgress = 1f;
            }
        } else if (!toCircle && toCircleProgress != 0f) {
            toCircleProgress -= 16 / 400f;
            if (toCircleProgress < 0) {
                toCircleProgress = 0f;
            }
        }

        if (noProgress) {
            if (toCircleProgress == 0) {
                currentProgressTime += dt;
                if (currentProgressTime >= risingTime) {
                    currentProgressTime = risingTime;
                }
                if (risingCircleLength) {
                    currentCircleLength = 4 + 266 * accelerateInterpolator.getInterpolation(currentProgressTime / risingTime);
                } else {
                    currentCircleLength = 4 - 270 * (1.0f - decelerateInterpolator.getInterpolation(currentProgressTime / risingTime));
                }

                if (currentProgressTime == risingTime) {
                    if (risingCircleLength) {
                        radOffset += 270;
                        currentCircleLength = -266;
                    }
                    risingCircleLength = !risingCircleLength;
                    currentProgressTime = 0;
                }
            } else {
                if (risingCircleLength) {
                    float old = currentCircleLength;
                    currentCircleLength = 4 + 266 * accelerateInterpolator.getInterpolation(currentProgressTime / risingTime);
                    currentCircleLength += 360 * toCircleProgress;
                    float dx = old - currentCircleLength;
                    if (dx > 0) {
                        radOffset += old - currentCircleLength;
                    }
                } else {
                    float old = currentCircleLength;
                    currentCircleLength = 4 - 270 * (1.0f - decelerateInterpolator.getInterpolation(currentProgressTime / risingTime));
                    currentCircleLength -= 364 * toCircleProgress;
                    float dx = old - currentCircleLength;
                    if (dx > 0) {
                        radOffset += old - currentCircleLength;
                    }
                }
            }
        } else {
            float progressDiff = currentProgress - progressAnimationStart;
            if (progressDiff > 0) {
                progressTime += dt;
                if (progressTime >= 200.0f) {
                    animatedProgress = progressAnimationStart = currentProgress;
                    progressTime = 0;
                } else {
                    animatedProgress = progressAnimationStart + progressDiff * AndroidUtilities.decelerateInterpolator.getInterpolation(progressTime / 200.0f);
                }
            }
            currentCircleLength = Math.max(4, 360 * animatedProgress);
        }
        invalidate();
    }

    public void setSize(int value) {
        size = value;
        invalidate();
    }

    public void setStrokeWidth(float value) {
        progressPaint.setStrokeWidth(AndroidUtilities.dp(value));
    }

    public void setProgressColor(int color) {
        progressColor = color;
        progressPaint.setColor(progressColor);
    }

    public void toCircle(boolean toCircle, boolean animated) {
        this.toCircle = toCircle;
        if (!animated) {
            toCircleProgress = toCircle ? 1f : 0f;
        }
    }

    private boolean useMd3() {
        return MD3_LOADER && noProgress && toCircleProgress == 0f;
    }

    private static double md3Radius(float[] s, double a) {
        if (s[0] > 0) {
            return (1.0 + s[1] * Math.cos(s[0] * a)) / (1.0 + s[1]);
        }
        double c = Math.cos(a);
        double sn = Math.sin(a) / s[2];
        return 1.0 / Math.sqrt(c * c + sn * sn);
    }

    private void drawMd3(Canvas canvas, float cx, float cy) {
        long now = SystemClock.uptimeMillis();
        if (md3Start == 0) {
            md3Start = now;
        }
        long elapsed = now - md3Start;
        int step = (int) (elapsed / MD3_STEP);
        long inStep = elapsed % MD3_STEP;
        float t = Math.min(1f, inStep / (float) MD3_MORPH);
        float e = md3Interpolator.getInterpolation(t);

        float[] from = MD3_SHAPES[step % MD3_SHAPES.length];
        float[] to = MD3_SHAPES[(step + 1) % MD3_SHAPES.length];

        float rotation = (elapsed % 4500L) * 360f / 4500f + ((step % 4) + e) * 90f;
        float radius = size * 0.42f;

        md3Path.rewind();
        for (int i = 0; i < MD3_POINTS; i++) {
            double a = 2 * Math.PI * i / MD3_POINTS;
            double r1 = md3Radius(from, a);
            double r2 = md3Radius(to, a);
            float r = (float) (r1 + (r2 - r1) * e) * radius;
            float x = (float) (Math.cos(a) * r);
            float y = (float) (Math.sin(a) * r);
            if (i == 0) {
                md3Path.moveTo(x, y);
            } else {
                md3Path.lineTo(x, y);
            }
        }
        md3Path.close();

        md3Paint.setColor(progressPaint.getColor());
        canvas.save();
        canvas.translate(cx, cy);
        canvas.rotate(rotation);
        canvas.drawPath(md3Path, md3Paint);
        canvas.restore();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (useMd3()) {
            drawMd3(canvas, getMeasuredWidth() / 2f, getMeasuredHeight() / 2f);
            updateAnimation();
            return;
        }
        int x = (getMeasuredWidth() - size) / 2;
        int y = (getMeasuredHeight() - size) / 2;
        cicleRect.set(x, y, x + size, y + size);
        canvas.drawArc(cicleRect, radOffset, drawingCircleLenght = currentCircleLength, false, progressPaint);
        updateAnimation();
    }

    public void draw(Canvas canvas, float cx, float cy) {
        if (useMd3()) {
            drawMd3(canvas, cx, cy);
            updateAnimation();
            return;
        }
        cicleRect.set(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f);
        canvas.drawArc(cicleRect, radOffset, drawingCircleLenght = currentCircleLength, false, progressPaint);
        updateAnimation();
    }

    public boolean isCircle() {
        return Math.abs(drawingCircleLenght) >= 360;
    }

    private int getThemedColor(int key) {
        return Theme.getColor(key, resourcesProvider);
    }
}
