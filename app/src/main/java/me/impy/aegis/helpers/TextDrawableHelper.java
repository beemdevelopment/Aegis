package me.impy.aegis.helpers;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;

public class TextDrawableHelper {
    private TextDrawableHelper() {

    }

    public static TextDrawable generate(String s) {
        if (s == null || s.length() <= 1) {
            return null;
        }

        ColorGenerator generator = ColorGenerator.MATERIAL;
        int color = generator.getColor(s);
        return TextDrawable.builder().beginConfig()
                .width(100)
                .height(100).endConfig().buildRect(s.substring(0, 1).toUpperCase(), color);
    }
}
