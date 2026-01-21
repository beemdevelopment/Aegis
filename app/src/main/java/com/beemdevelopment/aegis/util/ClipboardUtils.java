package com.beemdevelopment.aegis.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

public final class ClipboardUtils {
    public static String readText(Context context) {
        ClipboardManager clipboard =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        if (clipboard == null) {
            return null;
        }

        ClipData clip = clipboard.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) {
            return null;
        }

        ClipData.Item item = clip.getItemAt(0);
        CharSequence cs = item.coerceToText(context);
        if (cs == null) {
            return null;
        }

        String text = cs.toString().trim();
        if (text.isEmpty()) {
            return null;
        }

        return text;
    }
}
