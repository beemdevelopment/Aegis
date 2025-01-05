package com.beemdevelopment.aegis.helpers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.beemdevelopment.aegis.icons.IconType;
import com.beemdevelopment.aegis.vault.VaultEntryIcon;

import java.io.ByteArrayOutputStream;
import java.util.Objects;

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

    public static boolean isVaultEntryIconOptimized(VaultEntryIcon icon) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(icon.getBytes(), 0, icon.getBytes().length, opts);
        return opts.outWidth <= VaultEntryIcon.MAX_DIMENS && opts.outHeight <= VaultEntryIcon.MAX_DIMENS;
    }

    public static VaultEntryIcon toVaultEntryIcon(Bitmap bitmap, IconType iconType) {
        if (bitmap.getWidth() > VaultEntryIcon.MAX_DIMENS
                || bitmap.getHeight() > VaultEntryIcon.MAX_DIMENS) {
            bitmap = resize(bitmap, VaultEntryIcon.MAX_DIMENS, VaultEntryIcon.MAX_DIMENS);
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        if (Objects.equals(iconType, IconType.PNG)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        } else {
            iconType = IconType.JPEG;
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        }

        byte[] data = stream.toByteArray();
        return new VaultEntryIcon(data, iconType);
    }
}
