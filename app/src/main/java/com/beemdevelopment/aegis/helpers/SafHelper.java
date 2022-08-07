package com.beemdevelopment.aegis.helpers;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

public class SafHelper {
    private SafHelper() {

    }

    public static String getFileName(Context context, Uri uri) {
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int i = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (i != -1) {
                        return cursor.getString(i);
                    }
                }
            }
        }

        return uri.getLastPathSegment();
    }
}
