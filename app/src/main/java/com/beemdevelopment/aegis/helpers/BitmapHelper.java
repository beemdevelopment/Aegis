package com.beemdevelopment.aegis.helpers;

import android.graphics.Bitmap;

public class BitmapHelper {
    private BitmapHelper() {

    }

    /**
     * Scales the given Bitmap to the given maximum width/height, while keeping the aspect ratio intact.
     */
    public static Bitmap resize(Bitmap bitmap, int maxWidth, int maxHeight) {
        if (maxHeight <= 0 || maxWidth <= 0) {
            return bitmap;
        }

        float maxRatio = (float) maxWidth / maxHeight;
        float ratio = (float) bitmap.getWidth() / bitmap.getHeight();

        int width = maxWidth;
        int height = maxHeight;
        if (maxRatio > 1) {
            width = (int) ((float) maxHeight * ratio);
        } else {
            height = (int) ((float) maxWidth / ratio);
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }
}
