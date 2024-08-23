package com.beemdevelopment.aegis.helpers;

import android.graphics.Rect;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

import androidx.annotation.NonNull;

public class CenterVerticalSpan extends MetricAffectingSpan {
    Rect _substringBounds;

    public CenterVerticalSpan(Rect substringBounds) {
        _substringBounds = substringBounds;
    }

    @Override
    public void updateMeasureState(@NonNull TextPaint textPaint) {
        applyBaselineShift(textPaint);
    }

    @Override
    public void updateDrawState(@NonNull TextPaint textPaint) {
        applyBaselineShift(textPaint);
    }

    private void applyBaselineShift(TextPaint textPaint) {
        float topDifference = textPaint.getFontMetrics().top - _substringBounds.top;
        textPaint.baselineShift -= (topDifference / 2f);
    }
}
