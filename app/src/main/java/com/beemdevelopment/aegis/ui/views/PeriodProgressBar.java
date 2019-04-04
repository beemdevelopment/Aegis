package com.beemdevelopment.aegis.ui.views;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;

import com.beemdevelopment.aegis.otp.TotpInfo;

import androidx.annotation.RequiresApi;

public class PeriodProgressBar extends ProgressBar {
    private int _period;

    public PeriodProgressBar(Context context) {
        super(context);
    }

    public PeriodProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PeriodProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public PeriodProgressBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setPeriod(int period) {
        _period = period;
    }

    public void refresh() {
        // reset the progress bar
        int maxProgress = getMax();
        setProgress(maxProgress);

        // calculate the progress the bar should start at
        long millisTillRotation = TotpInfo.getMillisTillNextRotation(_period);
        long period = _period * maxProgress;
        int currentProgress = maxProgress - (int) ((((double) period - millisTillRotation) / period) * maxProgress);

        // start progress animation
        ObjectAnimator animation = ObjectAnimator.ofInt(this, "progress", currentProgress, 0);
        animation.setDuration(millisTillRotation);
        animation.setInterpolator(new LinearInterpolator());
        animation.start();
    }
}
