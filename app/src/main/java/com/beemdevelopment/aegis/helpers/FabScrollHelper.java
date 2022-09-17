package com.beemdevelopment.aegis.helpers;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

public class FabScrollHelper {
    private View _fabMenu;
    private boolean _isAnimating;

    public FabScrollHelper(View floatingActionsMenu) {
        _fabMenu = floatingActionsMenu;
    }

    public void onScroll(int dx, int dy) {
        if (dy > 2 && _fabMenu.getVisibility() == View.VISIBLE && !_isAnimating) {
            setVisible(false);
        } else if (dy < -2 && _fabMenu.getVisibility() != View.VISIBLE && !_isAnimating) {
            setVisible(true);
        }
    }

    public void setVisible(boolean visible) {
        if (visible) {
            _fabMenu.setVisibility(View.VISIBLE);
            _fabMenu.animate()
                    .translationY(0)
                    .setInterpolator(new DecelerateInterpolator(2))
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            _isAnimating = false;
                            super.onAnimationEnd(animation);
                        }
                    }).start();
        } else {
            _isAnimating = true;
            CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) _fabMenu.getLayoutParams();
            int fabBottomMargin = lp.bottomMargin;
            _fabMenu.animate()
                    .translationY(_fabMenu.getHeight() + fabBottomMargin)
                    .setInterpolator(new AccelerateInterpolator(2))
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            _isAnimating = false;
                            _fabMenu.setVisibility(View.INVISIBLE);
                            super.onAnimationEnd(animation);
                        }
                    }).start();
        }

    }
}
