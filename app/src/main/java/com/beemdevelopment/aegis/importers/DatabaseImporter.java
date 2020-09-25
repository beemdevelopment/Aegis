package com.beemdevelopment.aegis.importers;

import android.content.Context;
import android.content.pm.PackageManager;

import com.beemdevelopment.aegis.util.UUIDMap;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.topjohnwu.superuser.io.SuFile;
import com.topjohnwu.superuser.io.SuFileInputStream;

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
        _importers.put("Authenticator Plus", AuthenticatorPlusImporter.class);
        _importers.put("Authy", AuthyImporter.class);
        _importers.put("andOTP", AndOtpImporter.class);
        _importers.put("FreeOTP", FreeOtpImporter.class);
        _importers.put("FreeOTP+", FreeOtpPlusImporter.class);
        _importers.put("Google Authenticator", GoogleAuthImporter.class);
        _importers.put("Microsoft Authenticator", MicrosoftAuthImporter.class);
        _importers.put("Plain text", GoogleAuthUriImporter.class);
        _importers.put("Steam", SteamImporter.class);
        _importers.put("TOTP Authenticator", TotpAuthenticatorImporter.class);
        _importers.put("WinAuth", WinAuthImporter.class);

        _appImporters = new LinkedHashMap<>();
        _appImporters.put("Authy", AuthyImporter.class);
        _appImporters.put("FreeOTP", FreeOtpImporter.class);
        _appImporters.put("FreeOTP+", FreeOtpPlusImporter.class);
        _appImporters.put("Google Authenticator", GoogleAuthImporter.class);
        _appImporters.put("Microsoft Authenticator", MicrosoftAuthImporter.class);
        _appImporters.put("Steam", SteamImporter.class);
        _appImporters.put("TOTP Authenticator", TotpAuthenticatorImporter.class);
    }

    public DatabaseImporter(Context context) {
        _context = context;
    }

    protected Context getContext() {
        return _context;
    }

    protected abstract SuFile getAppPath() throws DatabaseImporterException, PackageManager.NameNotFoundException;

    protected SuFile getAppPath(String pkgName, String subPath) throws PackageManager.NameNotFoundException {
        PackageManager man = getContext().getPackageManager();
        return new SuFile(man.getApplicationInfo(pkgName, 0).dataDir, subPath);
    }

    protected abstract State read(InputStream stream, boolean isInternal) throws DatabaseImporterException;

    public State read(InputStream stream) throws DatabaseImporterException {
        return read(stream, false);
    }

    public State readFromApp() throws PackageManager.NameNotFoundException, DatabaseImporterException {
        SuFile file = getAppPath();
        try (SuFileInputStream stream = new SuFileInputStream(file)) {
            return read(stream, true);
        } catch (IOException e) {
            throw new DatabaseImporterException(e);
        }
    }

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
        private UUIDMap<VaultEntry> _entries = new UUIDMap<>();
        private List<DatabaseImporterEntryException> _errors = new ArrayList<>();

        public void addEntry(VaultEntry entry) {
            _entries.add(entry);
        }

        public void addError(DatabaseImporterEntryException error) {
            _errors.add(error);
        }

        public UUIDMap<VaultEntry> getEntries() {
            return _entries;
        }

        public List<DatabaseImporterEntryException> getErrors() {
            return _errors;
        }
    }

    public interface DecryptListener {
        void onStateDecrypted(State state);
        void onError(Exception e);
    }
}
