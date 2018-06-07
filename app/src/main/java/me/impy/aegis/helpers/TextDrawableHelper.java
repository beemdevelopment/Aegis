package me.impy.aegis.helpers;

import android.view.View;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;

public class TextDrawableHelper {
    private TextDrawableHelper() {

    }

    public static TextDrawable generate(String text, String fallback, View view) {
        if (text == null || text.isEmpty()) {
            if (fallback == null || fallback.isEmpty()) {
                return null;
            }
            text = fallback;
        }

        int color = ColorGenerator.MATERIAL.getColor(text);
        return TextDrawable.builder().beginConfig()
                .width(view.getWidth())
                .height(view.getHeight())
                .endConfig()
                .buildRect(text.substring(0, 1).toUpperCase(), color);
    }
}
