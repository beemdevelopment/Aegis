package com.beemdevelopment.aegis.importers;

import android.content.Context;
import android.database.Cursor;

import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.encoding.EncodingException;
import com.beemdevelopment.aegis.otp.HotpInfo;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.beemdevelopment.aegis.vault.VaultEntry;

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
    protected String getAppPkgName() {
        return _pkgName;
    }

    @Override
    protected String getAppSubPath() {
        return _subPath;
    }

    @Override
    public State read(FileReader reader) throws DatabaseImporterException {
        SqlImporterHelper helper = new SqlImporterHelper(getContext());
        List<Entry> entries = helper.read(Entry.class, reader.getStream(), "accounts");
        return new State(entries);
    }

    public static class State extends DatabaseImporter.State {
        private List<Entry> _entries;

        private State(List<Entry> entries) {
            super(false);
            _entries = entries;
        }

        @Override
        public Result convert() {
            Result result = new Result();

            for (Entry sqlEntry : _entries) {
                try {
                    VaultEntry entry = convertEntry(sqlEntry);
                    result.addEntry(entry);
                } catch (DatabaseImporterEntryException e) {
                    result.addError(e);
                }
            }

            return result;
        }

        private static VaultEntry convertEntry(Entry entry) throws DatabaseImporterEntryException {
            try {
                byte[] secret = Base32.decode(entry.getSecret());

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
        }

        public int getType() {
            return _type;
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
