package com.beemdevelopment.aegis.vault;

import android.content.ContentResolver;
import android.content.Context;
import android.content.UriPermission;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import com.beemdevelopment.aegis.BackupsVersioningStrategy;
import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.database.AuditLogRepository;
import com.beemdevelopment.aegis.util.IOUtils;

import java.io.File;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VaultBackupManager {
    private static final String TAG = VaultBackupManager.class.getSimpleName();

    private static final StrictDateFormat _dateFormat =
            new StrictDateFormat("yyyyMMdd-HHmmss", Locale.ENGLISH);

    public static final String FILENAME_PREFIX = "aegis-backup";
    public static final String FILENAME_SINGLE = String.format("%s.json", FILENAME_PREFIX);

    private final Context _context;
    private final Preferences _prefs;
    private final ExecutorService _executor;
    private final AuditLogRepository _auditLogRepository;

    public VaultBackupManager(Context context, AuditLogRepository auditLogRepository) {
        _context = context;
        _prefs = new Preferences(context);
        _executor = Executors.newSingleThreadExecutor();
        _auditLogRepository = auditLogRepository;
    }

    public void scheduleBackup(File tempFile, BackupsVersioningStrategy strategy, Uri uri, int versionsToKeep) {
        _executor.execute(() -> {
            try {
                createBackup(tempFile, strategy, uri, versionsToKeep);
                _auditLogRepository.addBackupCreatedEvent();
                _prefs.setBuiltInBackupResult(new Preferences.BackupResult(null));
            } catch (VaultRepositoryException | VaultBackupPermissionException e) {
                e.printStackTrace();
                _prefs.setBuiltInBackupResult(new Preferences.BackupResult(e));
            }
        });
    }

    private void createBackup(File tempFile, BackupsVersioningStrategy strategy, Uri uri, int versionsToKeep)
            throws VaultRepositoryException, VaultBackupPermissionException {
        if (uri == null) {
            throw new VaultRepositoryException("getBackupsLocation returned null");
        }
        if (strategy == BackupsVersioningStrategy.SINGLE_BACKUP) {
            createBackup(tempFile, uri);
        } else if (strategy == BackupsVersioningStrategy.MULTIPLE_BACKUPS) {
            createBackup(tempFile, uri, versionsToKeep);
        } else {
            throw new VaultRepositoryException("Invalid backups versioning strategy");
        }
    }

    private void createBackup(File tempFile, Uri fileUri)
            throws VaultRepositoryException, VaultBackupPermissionException {
        Log.i(TAG, String.format("Creating backup at %s", fileUri));
        try {
            if (!hasPermissionsAt(fileUri)) {
                throw new VaultBackupPermissionException("No persisted URI permissions");
            }
            ContentResolver resolver = _context.getContentResolver();
            try (FileInputStream inStream = new FileInputStream(tempFile);
                 OutputStream outStream = resolver.openOutputStream(fileUri, "wt")
            ) {
                if (outStream == null) {
                    throw new IOException("openOutputStream returned null");
                }
                IOUtils.copy(inStream, outStream);
            } catch (IOException exception) {
                throw new VaultRepositoryException(exception);
            }
        } catch (VaultRepositoryException | VaultBackupPermissionException exception) {
            Log.e(TAG, String.format("Unable to create backup: %s", exception));
            throw exception;
        } finally {
            tempFile.delete();
        }
    }

    private void createBackup(File tempFile, Uri dirUri, int versionsToKeep)
            throws VaultRepositoryException, VaultBackupPermissionException {
        FileInfo fileInfo = new FileInfo(FILENAME_PREFIX);
        DocumentFile dir = DocumentFile.fromTreeUri(_context, dirUri);

        try {
            Log.i(TAG, String.format("Creating backup at %s: %s", Uri.decode(dir.getUri().toString()), fileInfo.toString()));

            if (!hasPermissionsAt(dirUri)) {
                throw new VaultBackupPermissionException("No persisted URI permissions");
            }

            // If we create a file with a name that already exists, SAF will append a number
            // to the filename and write to that instead. We can't overwrite existing files, so
            // just avoid that altogether by checking beforehand.
            if (dir.findFile(fileInfo.toString()) != null) {
                throw new VaultRepositoryException("Backup file already exists");
            }

            DocumentFile file = dir.createFile("application/json", fileInfo.toString());
            if (file == null) {
                throw new VaultRepositoryException("createFile returned null");
            }

            try (FileInputStream inStream = new FileInputStream(tempFile);
                 OutputStream outStream = _context.getContentResolver().openOutputStream(file.getUri())) {
                if (outStream == null) {
                    throw new IOException("openOutputStream returned null");
                }
                IOUtils.copy(inStream, outStream);
            } catch (IOException e) {
                throw new VaultRepositoryException(e);
            }
        } catch (VaultRepositoryException | VaultBackupPermissionException e) {
            Log.e(TAG, String.format("Unable to create backup: %s", e.toString()));
            throw e;
        } finally {
            tempFile.delete();
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
        if (versionsToKeep <= 0) {
            return;
        }

        Log.i(TAG, String.format("Scanning directory %s for backup files", Uri.decode(dir.getUri().toString())));

        List<BackupFile> files = new ArrayList<>();
        for (DocumentFile docFile : dir.listFiles()) {
            if (docFile.isFile() && !docFile.isVirtual()) {
                try {
                    files.add(new BackupFile(docFile));
                } catch (ParseException ignored) { }
            }
        }

        Log.i(TAG, String.format("Found %d backup files, keeping the %d most recent", files.size(), versionsToKeep));

        Collections.sort(files, new FileComparator());
        if (files.size() > versionsToKeep) {
            for (BackupFile file : files.subList(0, files.size() - versionsToKeep)) {
                Log.i(TAG, String.format("Deleting %s", file.getFile().getName()));
                if (!file.getFile().delete()) {
                    Log.e(TAG, String.format("Unable to delete %s", file.getFile().getName()));
                }
            }
        }
    }

    public static class FileInfo {
        private String _filename;
        private String _ext;
        private Date _date;

        public FileInfo(String filename, String extension, Date date) {
            _filename = filename;
            _ext = extension;
            _date = date;
        }

        public FileInfo(String filename, Date date) {
            this(filename, "json", date);
        }

        public FileInfo(String filename) {
            this(filename, Calendar.getInstance().getTime());
        }

        public FileInfo(String filename, String extension) {
            this(filename, extension, Calendar.getInstance().getTime());
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

        public String getExtension() {
            return _ext;
        }

        public Date getDate() {
            return _date;
        }

        @NonNull
        @Override
        public String toString() {
            return String.format("%s-%s.%s", _filename, _dateFormat.format(_date), _ext);
        }
    }

    private static class BackupFile {
        private DocumentFile _file;
        private FileInfo _info;

        public BackupFile(DocumentFile file) throws ParseException {
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

    private static class FileComparator implements Comparator<BackupFile> {
        @Override
        public int compare(BackupFile o1, BackupFile o2) {
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
                String format = format(d);
                if (posIndex + format.length() != text.length() ||
                        !text.endsWith(format)) {
                    d = null; // Not exact match
                }
            }
            return d;
        }
    }
}
