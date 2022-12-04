package com.beemdevelopment.aegis.importers;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.encoding.EncodingException;
import com.beemdevelopment.aegis.otp.GoogleAuthInfo;
import com.beemdevelopment.aegis.otp.HotpInfo;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.io.SuFile;

import java.io.InputStream;
import java.util.List;

public class GoogleAuthImporter extends DatabaseImporter {
    private static final int TYPE_TOTP = 0;
    private static final int TYPE_HOTP = 1;

    private static final String _subPath = "databases/databases";
    private static final String _pkgName = "com.google.android.apps.authenticator2";

    public GoogleAuthImporter(Context context) {
        super(context);
    }

    @Override
    protected SuFile getAppPath() throws PackageManager.NameNotFoundException {
        SuFile file = getAppPath(_pkgName, _subPath);
        return file;
    }

    @Override
    public boolean isInstalledAppVersionSupported() {
        PackageInfo info;
        try {
            info = requireContext().getPackageManager().getPackageInfo(_pkgName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }

        return info.versionCode <= 5000100;
    }

    @Override
    public State read(InputStream stream, boolean isInternal) throws DatabaseImporterException {
        final Context context = requireContext();
        SqlImporterHelper helper = new SqlImporterHelper(context);
        List<Entry> entries = helper.read(Entry.class, stream, "accounts");
        return new State(entries, context);
    }

    @Override
    public DatabaseImporter.State readFromApp(Shell shell) throws PackageManager.NameNotFoundException, DatabaseImporterException {
        SuFile path = getAppPath();
        path.setShell(shell);

        final Context context = requireContext();
        SqlImporterHelper helper = new SqlImporterHelper(context);
        List<Entry> entries = helper.read(Entry.class, path, "accounts");
        return new State(entries, context);
    }

    public static class State extends DatabaseImporter.State {
        private List<Entry> _entries;
        private Context _context;

        private State(List<Entry> entries, Context context) {
            super(false);
            _entries = entries;
            _context = context;
        }

        @Override
        public Result convert() {
            Result result = new Result();

            for (Entry sqlEntry : _entries) {
                try {
                    VaultEntry entry = convertEntry(sqlEntry, _context);
                    result.addEntry(entry);
                } catch (DatabaseImporterEntryException e) {
                    result.addError(e);
                }
            }

            return result;
        }

        private static VaultEntry convertEntry(Entry entry, Context context) throws DatabaseImporterEntryException {
            try {
                if (entry.isEncrypted()) {
                    throw new DatabaseImporterException(context.getString(R.string.importer_encrypted_exception_google_authenticator, entry.getEmail()));
                }
                byte[] secret = GoogleAuthInfo.parseSecret(entry.getSecret());

                OtpInfo info;
                switch (entry.getType()) {
                    case TYPE_TOTP:
                        info = new TotpInfo(secret);
                        break;
                    case TYPE_HOTP:
                        info = new HotpInfo(secret, entry.getCounter());
                        break;
                    default:
                        throw new DatabaseImporterException("unsupported otp type: " + entry.getType());
                }

                String name = entry.getEmail();
                String[] parts = name.split(":");
                if (parts.length == 2) {
                    name = parts[1];
                }

                return new VaultEntry(info, name, entry.getIssuer());
            } catch (EncodingException | OtpInfoException | DatabaseImporterException e) {
                throw new DatabaseImporterEntryException(e, entry.toString());
            }
        }
    }

    private static class Entry extends SqlImporterHelper.Entry {
        private int _type;
        private boolean _isEncrypted;
        private String _secret;
        private String _email;
        private String _issuer;
        private long _counter;

        public Entry(Cursor cursor) {
            super(cursor);
            _type = SqlImporterHelper.getInt(cursor, "type");
            _secret = SqlImporterHelper.getString(cursor, "secret");
            _email = SqlImporterHelper.getString(cursor, "email", "");
            _issuer = SqlImporterHelper.getString(cursor, "issuer", "");
            _counter = SqlImporterHelper.getLong(cursor, "counter");
            _isEncrypted = (cursor.getColumnIndex("isencrypted") != -1 && SqlImporterHelper.getInt(cursor, "isencrypted") > 0);
        }


        public int getType() {
            return _type;
        }

        public boolean isEncrypted() {
            return _isEncrypted;
        }

        public String getSecret() {
            return _secret;
        }

        public String getEmail() {
            return _email;
        }

        public String getIssuer() {
            return _issuer;
        }

        public long getCounter() {
            return _counter;
        }
    }
}
