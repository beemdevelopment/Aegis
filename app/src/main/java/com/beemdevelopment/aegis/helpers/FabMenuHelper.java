package com.beemdevelopment.aegis.helpers;

import android.animation.ValueAnimator;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;


import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class FabMenuHelper {
    private final static long ANIMATION_DURATION = 300L;
    private final static long ANIMATION_ACTION_DELAY = 50L;
    private final View _scrim;
    private final View _menuItemsContainer;
    private final FloatingActionButton _mainFab;
    private final List<View> _actions;
    private Consumer<Boolean> _stateListener;
    private boolean _isOpen = false;

    public FabMenuHelper(
        View scrim,
        ViewGroup menuItemsContainer,
        FloatingActionButton fab,
        Map<View, Runnable> actions
    ) {
        _scrim = scrim;
        _menuItemsContainer = menuItemsContainer;
        _mainFab = fab;
        _actions = new ArrayList<>(actions.keySet());

        for (View action : _actions) {
            action.setVisibility(View.GONE);
            action.setAlpha(0f);
            action.setScaleX(0f);
            action.setScaleY(0f);
        }

        setupClickListeners(actions);
    }

    public void setOnFabMenuStateChangeListener(Consumer<Boolean> listener) {
        _stateListener = listener;
    }

    private void setupClickListeners(Map<View, Runnable> actions) {
        _mainFab.setOnClickListener(v -> toggle());
        _scrim.setOnClickListener(v -> close());

        actions.forEach((action, onClick) -> {
            action.setOnClickListener(v -> {
                if (onClick != null) {
                    onClick.run();
                }
                close();
            });
        });
    }

    public void toggle() {
        if (_isOpen) {
            close();
        } else {
            open();
        }
    }

    public void open() {
        if (_isOpen) {
            return;
        }

        _isOpen = true;

        _scrim.animate()
            .alpha(0.5f)
            .setDuration(ANIMATION_DURATION)
            .withStartAction(() -> _scrim.setVisibility(View.VISIBLE))
            .start();

        _menuItemsContainer.setVisibility(View.VISIBLE);

        long delay = 0L;
        for (int i = _actions.size() - 1; i >= 0; i--) {
            animateActionIn(_actions.get(i), delay);
            delay += ANIMATION_ACTION_DELAY;
        }

        animateFabIconForward(_mainFab);

        if (_stateListener != null) {
            _stateListener.accept(true);
        }
    }

    public void close() {
        if (!_isOpen) {
            return;
        }

        _isOpen = false;

        _scrim.animate()
            .alpha(0f)
            .setDuration(ANIMATION_DURATION)
            .withEndAction(() -> _scrim.setVisibility(View.GONE))
            .start();

        long delay = 0L;
        for (View action : _actions) {
            animateActionOut(action, delay);
            delay += ANIMATION_ACTION_DELAY;
        }

        animateFabIconBackward(_mainFab);

        _mainFab.postDelayed(() -> {
            if (!_isOpen) {
                _menuItemsContainer.setVisibility(View.GONE);
            }
        }, ANIMATION_DURATION);

        if (_stateListener != null) {
            _stateListener.accept(false);
        }
    }

    private void animateFabIconForward(FloatingActionButton fab) {
        animateFabIcon(fab, 0f, 45f);
    }

    private void animateFabIconBackward(FloatingActionButton fab) {
        animateFabIcon(fab, 45f, 0f);
    }

    private void animateFabIcon(FloatingActionButton fab, float from, float to) {
        Drawable drawable = _mainFab.getDrawable();
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        fab.setScaleType(ImageView.ScaleType.MATRIX);
        Matrix matrix = new Matrix();
        ValueAnimator anim = ValueAnimator.ofFloat(from, to);
        anim.setDuration(100L);

        anim.addUpdateListener(valueAnimator -> {
            Float angle = (Float) valueAnimator.getAnimatedValue();
            matrix.reset();
            matrix.postRotate(angle, width / 2f, height / 2f);
            fab.setImageMatrix(matrix);
        });

        anim.start();
    }

    private void animateActionIn(View action, long delay) {
        action.setVisibility(View.VISIBLE);
        action.setAlpha(0f);
        action.setScaleX(0.4f);
        action.setScaleY(0.4f);

        action.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(ANIMATION_DURATION)
                .setStartDelay(delay)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();
    }

    private void animateActionOut(View action, long delay) {
        action.animate()
            .alpha(0f)
            .scaleX(0f)
            .scaleY(0f)
            .setDuration(ANIMATION_DURATION)
            .setStartDelay(delay)
            .withEndAction(() -> action.setVisibility(View.GONE))
            .start();
    }

    public boolean isOpen() {
        return _isOpen;
    }
}
