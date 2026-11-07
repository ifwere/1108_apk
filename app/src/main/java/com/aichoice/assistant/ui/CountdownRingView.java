package com.aichoice.assistant.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.aichoice.assistant.R;

/**
 * A circular progress ring used by the countdown animation. The progress value
 * is the fraction of time remaining (1 = full ring, 0 = empty).
 */
public class CountdownRingView extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();
    private float progress = 0f;

    public CountdownRingView(Context context) {
        this(context, null);
    }

    public CountdownRingView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CountdownRingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setUpPaints();
    }

    private void setUpPaints() {
        float strokeWidth = dp(6f);

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        trackPaint.setStrokeWidth(strokeWidth);
        trackPaint.setColor(ContextCompat.getColor(getContext(), R.color.countdown_track));

        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setStrokeWidth(strokeWidth);
        progressPaint.setColor(ContextCompat.getColor(getContext(), R.color.ring_light_deepseek));
    }

    public void setAccentColor(int color) {
        progressPaint.setColor(color);
        invalidate();
    }

    public void setProgress(float value) {
        float clamped = Math.max(0f, Math.min(1f, value));
        if (Math.abs(clamped - progress) < 0.001f) {
            return;
        }
        progress = clamped;
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        float inset = progressPaint.getStrokeWidth() / 2f + dp(2f);
        arcBounds.set(inset, inset, getWidth() - inset, getHeight() - inset);
        canvas.drawArc(arcBounds, -90f, 360f, false, trackPaint);
        if (progress > 0f) {
            canvas.drawArc(arcBounds, -90f, 360f * progress, false, progressPaint);
        }
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
