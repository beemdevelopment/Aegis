package com.beemdevelopment.aegis.helpers;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.AppBarLayout;

public class ViewHelper {
    private ViewHelper() {

    }

    public static void setupAppBarInsets(AppBarLayout appBar) {
        ViewCompat.setOnApplyWindowInsetsListener(appBar, (targetView, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            targetView.setPadding(
                    insets.left,
                    insets.top,
                    insets.right,
                    0
            );
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
