package com.beemdevelopment.aegis.helpers;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import androidx.documentfile.provider.DocumentFile;

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

    public static String getMimeType(Context context, Uri uri) {
        DocumentFile file = DocumentFile.fromSingleUri(context, uri);
        if (file != null) {
            String fileType = file.getType();
            if (fileType != null) {
                return fileType;
            }

            String ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            if (ext != null) {
                return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
            }
        }

        return null;
    }
}
