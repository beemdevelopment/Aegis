package me.impy.aegis.helpers;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;

public class TextDrawableHelper {
    private TextDrawableHelper() {

    }

    public static TextDrawable generate(String text, String fallback) {
        if (text == null || text.isEmpty()) {
            if (fallback == null || fallback.isEmpty()) {
                return null;
            }
            text = fallback;
        }

        ColorGenerator generator = ColorGenerator.MATERIAL;
        int color = generator.getColor(text);
        return TextDrawable.builder().buildRound(text.substring(0, 1).toUpperCase(), color);
    }
}
