package com.beemdevelopment.aegis.importers;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;

import com.beemdevelopment.aegis.db.DatabaseEntry;
import com.beemdevelopment.aegis.util.ByteInputStream;
import com.topjohnwu.superuser.io.SuFile;
import com.topjohnwu.superuser.io.SuFileInputStream;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class DatabaseImporter {
    private Context _context;

    private static Map<String, Class<? extends DatabaseImporter>> _importers;
    private static Map<String, Class<? extends DatabaseImporter>> _appImporters;

    static {
        // note: keep these lists sorted alphabetically
        _importers = new LinkedHashMap<>();
        _importers.put("Aegis", AegisImporter.class);
        _importers.put("Authy", AuthyImporter.class);
        _importers.put("andOTP", AndOtpImporter.class);
        _importers.put("FreeOTP", FreeOtpImporter.class);
        _importers.put("Google Authenticator", GoogleAuthImporter.class);
        _importers.put("Steam", SteamImporter.class);

        _appImporters = new LinkedHashMap<>();
        _appImporters.put("Authy", AuthyImporter.class);
        _appImporters.put("FreeOTP", FreeOtpImporter.class);
        _appImporters.put("Google Authenticator", GoogleAuthImporter.class);
        _appImporters.put("Steam", SteamImporter.class);
    }

    public DatabaseImporter(Context context) {
        _context = context;
    }

    protected Context getContext() {
        return _context;
    }

    public SuFile getAppPath() throws DatabaseImporterException, PackageManager.NameNotFoundException {
        return getAppPath(getAppPkgName(), getAppSubPath());
    }

    protected SuFile getAppPath(String pkgName, String subPath) throws PackageManager.NameNotFoundException {
        PackageManager man = getContext().getPackageManager();
        return new SuFile(man.getApplicationInfo(pkgName, 0).dataDir, subPath);
    }

    protected abstract String getAppPkgName();

    protected abstract String getAppSubPath() throws DatabaseImporterException, PackageManager.NameNotFoundException;

    public abstract State read(FileReader reader) throws DatabaseImporterException;

    public static DatabaseImporter create(Context context, Class<? extends DatabaseImporter> type) {
        try {
            return type.getConstructor(Context.class).newInstance(context);
        } catch (IllegalAccessException | InstantiationException
                | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Class<? extends DatabaseImporter>> getImporters() {
        return Collections.unmodifiableMap(_importers);
    }

    public static Map<String, Class<? extends DatabaseImporter>> getAppImporters() {
        return Collections.unmodifiableMap(_appImporters);
    }

    public static abstract class State {
        private boolean _encrypted;

        public State(boolean encrypted) {
            _encrypted = encrypted;
        }

        public boolean isEncrypted() {
            return _encrypted;
        }

        public void decrypt(Context context, DecryptListener listener) throws DatabaseImporterException {
            if (!_encrypted) {
                throw new RuntimeException("Attempted to decrypt a plain text database");
            }

            throw new UnsupportedOperationException();
        }

        public Result convert() throws DatabaseImporterException {
            if (_encrypted) {
                throw new RuntimeException("Attempted to convert database before decrypting it");
            }

            throw new UnsupportedOperationException();
        }
    }

    public static class Result {
        private List<DatabaseEntry> _entries = new ArrayList<>();
        private List<DatabaseImporterEntryException> _errors = new ArrayList<>();

        public void addEntry(DatabaseEntry entry) {
            _entries.add(entry);
        }

        public void addError(DatabaseImporterEntryException error) {
            _errors.add(error);
        }

        public List<DatabaseEntry> getEntries() {
            return _entries;
        }

        public List<DatabaseImporterEntryException> getErrors() {
            return _errors;
        }
    }

    public static class FileReader implements Closeable {
        private InputStream _stream;

        private FileReader(InputStream stream) {
            _stream = stream;
        }

        public static FileReader open(String filename)
                throws FileNotFoundException {
            FileInputStream stream = new FileInputStream(filename);
            return new FileReader(stream);
        }

        public static FileReader open(SuFile file)
                throws FileNotFoundException {
            SuFileInputStream stream = new SuFileInputStream(file);
            return new FileReader(stream);
        }

        public static FileReader open(Context context, Uri uri)
                throws FileNotFoundException {
            InputStream stream = context.getContentResolver().openInputStream(uri);
            return new FileReader(stream);
        }

        public byte[] readAll() throws IOException {
            ByteInputStream stream = ByteInputStream.create(_stream);
            return stream.getBytes();
        }

        public InputStream getStream() {
            return _stream;
        }

        @Override
        public void close() throws IOException {
            _stream.close();
        }
    }

    public interface DecryptListener {
        void onStateDecrypted(State state);
        void onError(Exception e);
    }
}
