package com.beemdevelopment.aegis.importers;

import android.content.Context;
import android.database.Cursor;

import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.encoding.EncodingException;
import com.beemdevelopment.aegis.otp.HotpInfo;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.beemdevelopment.aegis.ui.Dialogs;
import com.beemdevelopment.aegis.vault.VaultEntry;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class AuthenticatorPlusImporter extends DatabaseImporter {
    private static final String _pkgName = "com.mufri.authenticatorplus";

    public AuthenticatorPlusImporter(Context context) {
        super(context);
    }

    @Override
    protected String getAppPkgName() {
        return _pkgName;
    }

    @Override
    protected String getAppSubPath() {
        throw new UnsupportedOperationException();
    }

    @Override
    public State read(FileReader reader) throws DatabaseImporterException {
        try {
            return new EncryptedState(reader.readAll());
        } catch (IOException e) {
            throw new DatabaseImporterException(e);
        }
    }

    public static class EncryptedState extends DatabaseImporter.State {
        private byte[] _data;

        private EncryptedState(byte[] data) {
            super(true);
            _data = data;
        }

        @Override
        public void decrypt(Context context, DecryptListener listener) {
            Dialogs.showPasswordInputDialog(context, password -> {
                try {
                    // recreate InputStream from saved byte array
                    InputStream stream = new ByteArrayInputStream(_data);

                    SqlImporterHelper helper = new SqlImporterHelper(context, password, true);
                    List<Entry> entries = helper.read(Entry.class, stream, "accounts");

                    DecryptedState state = new DecryptedState(entries);
                    listener.onStateDecrypted(state);
                } catch (DatabaseImporterException e) {
                    listener.onError(e);
                }
            });
        }
    }

    public static class DecryptedState extends DatabaseImporter.State {
        private List<Entry> _entries;

        private DecryptedState(List<Entry> entries) {
            super(false);
            _entries = entries;
        }

        private static VaultEntry convertEntry(Entry entry) throws DatabaseImporterEntryException {
            try {
                String secretString = entry.getSecret().replaceAll("\\s", "");
                byte[] secret = Base32.decode(secretString);

                OtpInfo info;
                switch (entry.getType()) {
                    case 0:
                        info = new TotpInfo(secret);
                        break;
                    case 1:
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

                // Authenticator Plus saves all entries to the "All Accounts" category by default
                // If an entry is in this category, we can consider it as uncategorized
                String group = entry.getGroup();
                if (group.equals("All Accounts")) {
                    return new VaultEntry(info, name, entry.getIssuer());
                }

                return new VaultEntry(info, name, entry.getIssuer(), entry.getGroup());
            } catch (EncodingException | OtpInfoException | DatabaseImporterException e) {
                throw new DatabaseImporterEntryException(e, entry.toString());
            }
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
    }

    private static class Entry extends SqlImporterHelper.Entry {
        private int _type;
        private String _secret;
        private String _email;
        private String _issuer;
        private String _group;
        private long _counter;

        public Entry(Cursor cursor) {
            super(cursor);
            _type = SqlImporterHelper.getInt(cursor, "type");
            _secret = SqlImporterHelper.getString(cursor, "secret");
            _email = SqlImporterHelper.getString(cursor, "email", "");
            _issuer = SqlImporterHelper.getString(cursor, "issuer", "");
            _group = SqlImporterHelper.getString(cursor, "category", "");
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

        public String getGroup() {
            return _group;
        }

        public long getCounter() {
            return _counter;
        }
    }
}
