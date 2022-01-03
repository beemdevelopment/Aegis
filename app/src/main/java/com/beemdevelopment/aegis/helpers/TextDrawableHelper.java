package com.beemdevelopment.aegis.helpers;

import android.view.View;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;

import java.text.BreakIterator;
import java.util.Arrays;

public class TextDrawableHelper {
    // taken from: https://materialuicolors.co (level 700)
    private static ColorGenerator _generator = ColorGenerator.create(Arrays.asList(
            0xFFD32F2F,
            0xFFC2185B,
            0xFF7B1FA2,
            0xFF512DA8,
            0xFF303F9F,
            0xFF1976D2,
            0xFF0288D1,
            0xFF0097A7,
            0xFF00796B,
            0xFF388E3C,
            0xFF689F38,
            0xFFAFB42B,
            0xFFFBC02D,
            0xFFFFA000,
            0xFFF57C00,
            0xFFE64A19,
            0xFF5D4037,
            0xFF616161,
            0xFF455A64
    ));

    private TextDrawableHelper() {

    }

    public static TextDrawable generate(String text, String fallback, View view) {
        if (text == null || text.isEmpty()) {
            if (fallback == null || fallback.isEmpty()) {
                return null;
            }
            text = fallback;
        }

        int color = _generator.getColor(text);
        return TextDrawable.builder().beginConfig()
                .width(view.getLayoutParams().width)
                .height(view.getLayoutParams().height)
                .endConfig()
                .buildRound(getFirstGrapheme(text).toUpperCase(), color);
    }

    private static String getFirstGrapheme(String text) {
        BreakIterator iter = BreakIterator.getCharacterInstance();
        iter.setText(text);

        int start = iter.first(), end = iter.next();
        if (end == BreakIterator.DONE) {
            return "";
        }

        return text.substring(start, end);
    }
}
