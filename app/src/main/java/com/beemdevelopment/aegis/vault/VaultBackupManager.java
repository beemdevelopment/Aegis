package com.beemdevelopment.aegis.vault;

import android.content.Context;
import android.content.UriPermission;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import com.beemdevelopment.aegis.util.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VaultBackupManager {
    private static final String TAG = VaultBackupManager.class.getSimpleName();

    private static final StrictDateFormat _dateFormat =
            new StrictDateFormat("yyyyMMdd-HHmmss", Locale.ENGLISH);

    public static final String FILENAME_PREFIX = "aegis-backup";

    private Context _context;

    public VaultBackupManager(Context context) {
        _context = context;
    }

    public void create(Uri dirUri, int versionsToKeep) throws VaultManagerException {
        FileInfo fileInfo = new FileInfo(FILENAME_PREFIX);
        DocumentFile dir = DocumentFile.fromTreeUri(_context, dirUri);

        Log.i(TAG, String.format("Creating backup at %s: %s", Uri.decode(dir.getUri().toString()), fileInfo.toString()));

        if (!hasPermissionsAt(dirUri)) {
            Log.e(TAG, "Unable to create file for backup, no persisted URI permissions");
            throw new VaultManagerException("No persisted URI permissions");
        }

        // If we create a file with a name that already exists, SAF will append a number
        // to the filename and write to that instead. We can't overwrite existing files, so
        // just avoid that altogether by checking beforehand.
        if (dir.findFile(fileInfo.toString()) != null) {
            throw new VaultManagerException("Backup file already exists");
        }

        DocumentFile file = dir.createFile("application/json", fileInfo.toString());
        if (file == null) {
            Log.e(TAG, "Unable to create file for backup, createFile returned null");
            throw new VaultManagerException("createFile returned null");
        }

        try (FileInputStream inStream = _context.openFileInput(VaultManager.FILENAME);
             OutputStream outStream = _context.getContentResolver().openOutputStream(file.getUri())) {
            IOUtils.copy(inStream, outStream);
        } catch (IOException e) {
            Log.e(TAG, "Unable to create backup", e);
            throw new VaultManagerException(e);
        }

        enforceVersioning(dir, versionsToKeep);
    }

    public boolean hasPermissionsAt(Uri uri) {
        for (UriPermission perm : _context.getContentResolver().getPersistedUriPermissions()) {
            if (perm.getUri().equals(uri)) {
                return perm.isReadPermission() && perm.isWritePermission();
            }
        }

        return false;
    }

    private void enforceVersioning(DocumentFile dir, int versionsToKeep) {
        Log.i(TAG, String.format("Scanning directory %s for backup files", Uri.decode(dir.getUri().toString())));

        List<File> files = new ArrayList<>();
        for (DocumentFile docFile : dir.listFiles()) {
            if (docFile.isFile() && !docFile.isVirtual()) {
                try {
                    files.add(new File(docFile));
                } catch (ParseException ignored) { }
            }
        }

        Collections.sort(files, new FileComparator());
        for (File file : files) {
            Log.i(TAG, file.getFile().getName());
        }

        if (files.size() > versionsToKeep) {
            for (File file : files.subList(0, files.size() - versionsToKeep)) {
                Log.i(TAG, String.format("Deleting %s", file.getFile().getName()));
                if (!file.getFile().delete()) {
                    Log.e(TAG, String.format("Unable to delete %s", file.getFile().getName()));
                }
            }
        }
    }

    public static class FileInfo {
        private String _filename;
        private Date _date;

        public FileInfo(String filename, Date date) {
            _filename = filename;
            _date = date;
        }

        public FileInfo(String filename) {
            this(filename, Calendar.getInstance().getTime());
        }

        public static FileInfo parseFilename(String filename) throws ParseException {
            if (filename == null) {
                throw new ParseException("The filename must not be null", 0);
            }

            final String ext = ".json";
            if (!filename.endsWith(ext)) {
                throwBadFormat(filename);
            }
            filename = filename.substring(0, filename.length() - ext.length());

            final String delim = "-";
            String[] parts = filename.split(delim);
            if (parts.length < 3) {
                throwBadFormat(filename);
            }

            filename = TextUtils.join(delim, Arrays.copyOf(parts, parts.length - 2));
            if (!filename.equals(FILENAME_PREFIX)) {
                throwBadFormat(filename);
            }

            Date date = _dateFormat.parse(parts[parts.length - 2] + delim + parts[parts.length - 1]);
            if (date == null) {
                throwBadFormat(filename);
            }

            return new FileInfo(filename, date);
        }

        private static void throwBadFormat(String filename) throws ParseException {
            throw new ParseException(String.format("Bad backup filename format: %s", filename), 0);
        }

        public String getFilename() {
            return _filename;
        }

        public Date getDate() {
            return _date;
        }

        @NonNull
        @Override
        public String toString() {
            return String.format("%s-%s.json", _filename, _dateFormat.format(_date));
        }
    }

    private static class File {
        private DocumentFile _file;
        private FileInfo _info;

        public File(DocumentFile file) throws ParseException {
            _file = file;
            _info = FileInfo.parseFilename(file.getName());
        }

        public DocumentFile getFile() {
            return _file;
        }

        public FileInfo getInfo() {
            return _info;
        }
    }

    private static class FileComparator implements Comparator<File> {
        @Override
        public int compare(File o1, File o2) {
            return o1.getInfo().getDate().compareTo(o2.getInfo().getDate());
        }
    }

    // https://stackoverflow.com/a/19503019
    private static class StrictDateFormat extends SimpleDateFormat {
        public StrictDateFormat(String pattern, Locale locale) {
            super(pattern, locale);
            setLenient(false);
        }

        @Override
        public Date parse(@NonNull String text, ParsePosition pos) {
            int posIndex = pos.getIndex();
            Date d = super.parse(text, pos);
            if (!isLenient() && d != null) {
                String format = this.format(d);
                if (posIndex + format.length() != text.length() ||
                        !text.endsWith(format)) {
                    d = null; // Not exact match
                }
            }
            return d;
        }
    }
}
