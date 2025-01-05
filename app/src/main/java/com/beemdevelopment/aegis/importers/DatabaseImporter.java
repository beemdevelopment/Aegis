package com.beemdevelopment.aegis.importers;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.StringRes;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.util.UUIDMap;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.beemdevelopment.aegis.vault.VaultGroup;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.io.SuFile;
import com.topjohnwu.superuser.io.SuFileInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract class DatabaseImporter {
    private Context _context;

    private static List<Definition> _importers;

    static {
        // note: keep these lists sorted alphabetically
        _importers = new ArrayList<>();
        _importers.add(new Definition("2FAS Authenticator", TwoFASImporter.class, R.string.importer_help_2fas, false));
        _importers.add(new Definition("Aegis", AegisImporter.class, R.string.importer_help_aegis, false));
        _importers.add(new Definition("andOTP", AndOtpImporter.class, R.string.importer_help_andotp, false));
        _importers.add(new Definition("Authenticator Plus", AuthenticatorPlusImporter.class, R.string.importer_help_authenticator_plus, false));
        _importers.add(new Definition("Authy", AuthyImporter.class, R.string.importer_help_authy, true));
        _importers.add(new Definition("Battle.net Authenticator", BattleNetImporter.class, R.string.importer_help_battle_net_authenticator, true));
        _importers.add(new Definition("Bitwarden", BitwardenImporter.class, R.string.importer_help_bitwarden, false));
        _importers.add(new Definition("Duo", DuoImporter.class, R.string.importer_help_duo, true));
        _importers.add(new Definition("Ente Auth", EnteAuthImporter.class, R.string.importer_help_ente_auth, false));
        _importers.add(new Definition("FreeOTP", FreeOtpImporter.class, R.string.importer_help_freeotp, true));
        _importers.add(new Definition("FreeOTP+ (JSON)", FreeOtpPlusImporter.class, R.string.importer_help_freeotp_plus, true));
        _importers.add(new Definition("Google Authenticator", GoogleAuthImporter.class, R.string.importer_help_google_authenticator, true));
        _importers.add(new Definition("Microsoft Authenticator", MicrosoftAuthImporter.class, R.string.importer_help_microsoft_authenticator, true));
        _importers.add(new Definition("Plain text", GoogleAuthUriImporter.class, R.string.importer_help_plain_text, false));
        _importers.add(new Definition("Steam", SteamImporter.class, R.string.importer_help_steam, true));
        _importers.add(new Definition("Stratum (Authenticator Pro)", StratumImporter.class, R.string.importer_help_stratum, true));
        _importers.add(new Definition("TOTP Authenticator", TotpAuthenticatorImporter.class, R.string.importer_help_totp_authenticator, true));
        _importers.add(new Definition("WinAuth", WinAuthImporter.class, R.string.importer_help_winauth, false));
    }

    public DatabaseImporter(Context context) {
        _context = context;
    }

    protected Context requireContext() {
        return _context;
    }

    protected abstract SuFile getAppPath() throws DatabaseImporterException, PackageManager.NameNotFoundException;

    protected SuFile getAppPath(String pkgName, String subPath) throws PackageManager.NameNotFoundException {
        PackageManager man = requireContext().getPackageManager();
        return new SuFile(man.getApplicationInfo(pkgName, 0).dataDir, subPath);
    }

    public boolean isInstalledAppVersionSupported() {
        return true;
    }

    protected abstract State read(InputStream stream, boolean isInternal) throws DatabaseImporterException;

    public State read(InputStream stream) throws DatabaseImporterException {
        return read(stream, false);
    }

    public State readFromApp(Shell shell) throws PackageManager.NameNotFoundException, DatabaseImporterException {
        SuFile file = getAppPath();
        file.setShell(shell);

        try (InputStream stream = SuFileInputStream.open(file)) {
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

    public static List<Definition> getImporters(boolean isDirect) {
        if (isDirect) {
            return Collections.unmodifiableList(_importers.stream().filter(Definition::supportsDirect).collect(Collectors.toList()));
        }

        return Collections.unmodifiableList(_importers);
    }

    public static class Definition implements Serializable {
        private final String _name;
        private final Class<? extends DatabaseImporter> _type;
        private final @StringRes int _help;
        private final boolean _supportsDirect;

        /**
         *
         * @param name The name of the Authenticator the importer handles.
         * @param type The class which does the importing.
         * @param help The string that explains the type of file needed (and optionally where it can be obtained).
         * @param supportsDirect Whether the importer can directly import the entries from the app's internal storage using root access.
         */
        public Definition(String name, Class<? extends DatabaseImporter> type, @StringRes int help, boolean supportsDirect) {
            _name = name;
            _type = type;
            _help = help;
            _supportsDirect = supportsDirect;
        }

        public String getName() {
            return _name;
        }

        public Class<? extends DatabaseImporter> getType() {
            return _type;
        }

        public @StringRes int getHelp() {
            return _help;
        }

        public boolean supportsDirect() {
            return _supportsDirect;
        }
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
        private UUIDMap<VaultGroup> _groups = new UUIDMap<>();
        private List<DatabaseImporterEntryException> _errors = new ArrayList<>();

        public void addEntry(VaultEntry entry) {
            _entries.add(entry);
        }

        public void addGroup(VaultGroup group) {
            _groups.add(group);
        }

        public void addError(DatabaseImporterEntryException error) {
            _errors.add(error);
        }

        public UUIDMap<VaultEntry> getEntries() {
            return _entries;
        }

        public UUIDMap<VaultGroup> getGroups() {
            return _groups;
        }

        public List<DatabaseImporterEntryException> getErrors() {
            return _errors;
        }
    }

    public static abstract class DecryptListener {
        protected abstract void onStateDecrypted(State state);
        protected abstract void onError(Exception e);
        protected abstract void onCanceled();
    }
}
