package me.impy.aegis.helpers;

import android.view.View;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;

public class TextDrawableHelper {
    private TextDrawableHelper() {

    }

    public static TextDrawable generate(String s, View view) {
        if (s == null || s.length() <= 1) {
            return null;
        }

        ColorGenerator generator = ColorGenerator.MATERIAL;
        int color = generator.getColor(s);
        return TextDrawable.builder().beginConfig()
                .width(view.getWidth())
                .height(view.getHeight()).endConfig().buildRect(s.substring(0, 1).toUpperCase(), color);
    }
}
