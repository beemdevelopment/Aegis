package com.beemdevelopment.aegis.helpers;

import android.content.Context;
import android.provider.Settings;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;

public class AnimationsHelper {
    private AnimationsHelper() {

    }

    public static Animation loadScaledAnimation(Context context, int animationResId) {
        return loadScaledAnimation(context, animationResId, Scale.ANIMATOR);
    }

    public static Animation loadScaledAnimation(Context context, int animationResId, Scale scale) {
        Animation animation = AnimationUtils.loadAnimation(context, animationResId);
        long newDuration = (long) (animation.getDuration() * scale.getValue(context));
        animation.setDuration(newDuration);
        return animation;
    }

    public static LayoutAnimationController loadScaledLayoutAnimation(Context context, int animationResId) {
        return loadScaledLayoutAnimation(context, animationResId, Scale.ANIMATOR);
    }

    public static LayoutAnimationController loadScaledLayoutAnimation(Context context, int animationResId, Scale scale) {
        LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(context, animationResId);
        Animation animation = controller.getAnimation();
        animation.setDuration((long) (animation.getDuration() * scale.getValue(context)));
        return controller;
    }

    public enum Scale {
        ANIMATOR(Settings.Global.ANIMATOR_DURATION_SCALE),
        TRANSITION(Settings.Global.TRANSITION_ANIMATION_SCALE);

        private final String _setting;

        Scale(String setting) {
            _setting = setting;
        }

        public float getValue(Context context) {
            return Settings.Global.getFloat(context.getContentResolver(), _setting, 1.0f);
        }

        public boolean isZero(Context context) {
            return getValue(context) == 0;
        }
    }
}
