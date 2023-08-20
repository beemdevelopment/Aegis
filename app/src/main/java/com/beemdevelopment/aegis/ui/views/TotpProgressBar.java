package com.beemdevelopment.aegis.ui.views;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;

import androidx.annotation.RequiresApi;

import com.beemdevelopment.aegis.helpers.AnimationsHelper;
import com.beemdevelopment.aegis.otp.TotpInfo;

public class TotpProgressBar extends ProgressBar {
    private int _period = TotpInfo.DEFAULT_PERIOD;
    private Handler _handler;
    private float _animDurationScale;

    public TotpProgressBar(Context context) {
        super(context);
    }

    public TotpProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TotpProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public TotpProgressBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setPeriod(int period) {
        _period = period;
    }

    public void start() {
        stop();
        _handler = new Handler();
        _animDurationScale = AnimationsHelper.Scale.ANIMATOR.getValue(getContext());
        refresh();
    }

    public void stop() {
        if (_handler != null) {
            _handler.removeCallbacksAndMessages(null);
            _handler = null;
        }
    }

    public void restart() {
        stop();
        start();
    }

    private void refresh() {
        // calculate the current progress the bar should start at
        int maxProgress = getMax();
        long millisTillRotation = TotpInfo.getMillisTillNextRotation(_period);
        int currentProgress = (int) (maxProgress * ((float) millisTillRotation / (_period * 1000)));

        // start progress animation, compensating for any changes to the animator duration scale settings
        float animPart = (float) maxProgress / _period;
        int animEnd = (int) (Math.floor(currentProgress / animPart) * animPart);
        int animPartDuration = _animDurationScale > 0 ? (int) (1000 / _animDurationScale) : 0;
        float animDurationFraction = (float) (currentProgress - animEnd) / animPart;
        int realAnimDuration = (int) (1000 * animDurationFraction);
        int animDuration =  (int) (animPartDuration * animDurationFraction);

        ObjectAnimator animation = ObjectAnimator.ofInt(this, "progress", currentProgress, animEnd);
        animation.setDuration(animDuration);
        animation.setInterpolator(new LinearInterpolator());
        animation.start();

        // the animation only lasts for (less than) one second, so restart it after that
        _handler.postDelayed(this::refresh, realAnimDuration);
    }
}
