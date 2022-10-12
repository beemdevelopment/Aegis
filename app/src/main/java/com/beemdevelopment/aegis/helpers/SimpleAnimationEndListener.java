package com.beemdevelopment.aegis.helpers;

import android.view.animation.Animation;

public class SimpleAnimationEndListener implements Animation.AnimationListener {
    private final Listener _listener;

    public SimpleAnimationEndListener(Listener listener) {
        _listener = listener;
    }

    @Override
    public void onAnimationStart(Animation animation) {

    }

    @Override
    public void onAnimationEnd(Animation animation) {
        if (_listener != null) {
            _listener.onAnimationEnd(animation);
        }
    }

    @Override
    public void onAnimationRepeat(Animation animation) {

    }

    public interface Listener {
        void onAnimationEnd(Animation animation);
    }
}
