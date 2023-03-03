package com.beemdevelopment.aegis.importers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.encoding.EncodingException;
import com.beemdevelopment.aegis.otp.*;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.util.IOUtils;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.topjohnwu.superuser.io.SuFile;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.List;

public class AuthenticatorProImporter extends DatabaseImporter {
    private static final String _HEADER = "AuthenticatorPro";
    private static final int _ITERATIONS = 64000;
    private static final int _KEY_SIZE = 32 * Byte.SIZE;
    private static final String _PKG_NAME = "me.jmh.authenticatorpro";
    private static final String _PKG_DB_PATH = "files/proauth.db3";

    private enum Algorithm {
        SHA1,
        SHA256,
        SHA512
    }

    public AuthenticatorProImporter(Context context) {
        super(context);
    }

    @Override
    protected SuFile getAppPath() throws DatabaseImporterException, PackageManager.NameNotFoundException {
        return getAppPath(_PKG_NAME, _PKG_DB_PATH);
    }

    @Override
    protected State read(InputStream stream, boolean isInternal) throws DatabaseImporterException {
        return isInternal ? readInternal(stream) : readExternal(stream);
    }

    private State readInternal(InputStream stream) throws DatabaseImporterException {
        List<SqlEntry> entries = new SqlImporterHelper(requireContext()).read(SqlEntry.class, stream, "authenticator");
        return new SqlState(entries);
    }

    private State readExternal(InputStream stream) throws DatabaseImporterException {
        byte[] data;
        try {
            data = IOUtils.readAll(stream);
        } catch (IOException e) {
            throw new DatabaseImporterException(e);
        }

        try {
            return new JsonState(new JSONObject(new String(data, StandardCharsets.UTF_8)));
        } catch (JSONException e) {
            return readEncrypted(new DataInputStream(new ByteArrayInputStream(data)));
        }
    }

    private EncryptedState readEncrypted(DataInputStream stream) throws DatabaseImporterException {
        try {
            byte[] headerBytes = new byte[_HEADER.getBytes(StandardCharsets.UTF_8).length];
            stream.readFully(headerBytes);
            String header = new String(headerBytes, StandardCharsets.UTF_8);
            if (!header.equals(_HEADER)) {
                throw new DatabaseImporterException("Invalid encryption header: " + header);
            }
            int saltSize = 20;
            byte[] salt = new byte[saltSize];
            stream.readFully(salt);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            int ivSize = cipher.getBlockSize();
            byte[] iv = new byte[ivSize];
            stream.readFully(iv);
            return new EncryptedState(cipher, salt, iv, IOUtils.readAll(stream));
        } catch (UTFDataFormatException e) {
            throw new DatabaseImporterException("Encryption header does not exist");
        } catch (IOException | NoSuchPaddingException | NoSuchAlgorithmException e) {
            throw new DatabaseImporterException(e);
        }
    }

    private static VaultEntry fromAny(
            int type,
            String issuer,
            String username,
            byte[] secret,
            Algorithm algo,
            int digits,
            int period,
            int counter,
            Object obj
    ) throws OtpInfoException, DatabaseImporterEntryException {
        OtpInfo info;
        switch (type) {
            case 1:
                info = new HotpInfo(secret, algo.name(), digits, counter);
                break;
            case 2:
                info = new TotpInfo(secret, algo.name(), digits, period);
                break;
            case 4:
                info = new SteamInfo(secret, algo.name(), digits, period);
                break;
            default:
                throw new DatabaseImporterEntryException("Unsupported otp type: " + type, obj.toString());
        }

        return new VaultEntry(info, username, issuer);
    }

    private static VaultEntry convertEntry(JSONObject authenticator) throws JSONException, EncodingException, OtpInfoException, DatabaseImporterEntryException {
        int type = authenticator.getInt("Type");
        String issuer = authenticator.getString("Issuer");
        Object nullableUsername = authenticator.get("Username");
        String username = nullableUsername == JSONObject.NULL ? "" : nullableUsername.toString();
        byte[] secret = Base32.decode(authenticator.getString("Secret"));
        Algorithm algo = Algorithm.values()[authenticator.getInt("Algorithm")];
        int digits = authenticator.getInt("Digits");
        int period = authenticator.getInt("Period");
        int counter = authenticator.getInt("Counter");

        return fromAny(type, issuer, username, secret, algo, digits, period, counter, authenticator);
    }

    static class EncryptedState extends State {
        private final Cipher _cipher;
        private final byte[] _salt;
        private final byte[] _iv;
        private final byte[] _data;

        public EncryptedState(Cipher cipher, byte[] salt, byte[] iv, byte[] data) {
            super(true);
            _cipher = cipher;
            _salt = salt;
            _iv = iv;
            _data = data;
        }

        public JsonState decrypt(char[] password) throws NoSuchAlgorithmException,
            InvalidKeySpecException,
            InvalidAlgorithmParameterException,
            InvalidKeyException,
            IllegalBlockSizeException,
            BadPaddingException,
            JSONException {
            KeySpec spec = new PBEKeySpec(password, _salt, _ITERATIONS, _KEY_SIZE);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            SecretKey key = keyFactory.generateSecret(spec);
            _cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(_iv));
            byte[] decrypted = _cipher.doFinal(_data);
            return new JsonState(new JSONObject(new String(decrypted, StandardCharsets.UTF_8)));
        }

        @Override
        public void decrypt(Context context, DecryptListener listener) throws DatabaseImporterException {
            Dialogs.showPasswordInputDialog(context, R.string.enter_password_aegis_title, password -> {
                try {
                    listener.onStateDecrypted(decrypt(password));
                } catch (InvalidAlgorithmParameterException | IllegalBlockSizeException | JSONException |
                         InvalidKeyException | BadPaddingException | InvalidKeySpecException |
                         NoSuchAlgorithmException e) {
                    listener.onError(e);
                }
            }, dialog -> listener.onCanceled());
        }
    }

    private static class JsonState extends State {
        private final JSONObject _obj;

        public JsonState(JSONObject obj) {
            super(false);
            _obj = obj;
        }

        @Override
        public Result convert() throws DatabaseImporterException {
            try {
                return convertThrowing();
            } catch (OtpInfoException | EncodingException | JSONException e) {
                throw new DatabaseImporterException(e);
            }
        }

        private Result convertThrowing() throws JSONException, OtpInfoException, EncodingException {
            Result ret = new Result();
            JSONArray authenticators = _obj.getJSONArray("Authenticators");
            for (int i = 0; i < authenticators.length(); i++) {
                JSONObject authenticator = authenticators.getJSONObject(i);
                try {
                    ret.addEntry(convertEntry(authenticator));
                } catch (DatabaseImporterEntryException e) {
                    ret.addError(e);
                }
            }

            return ret;
        }
    }

    private static class SqlState extends State {
        private final List<SqlEntry> _entries;

        public SqlState(List<SqlEntry> entries) {
            super(false);
            _entries = entries;
        }

        @Override
        public Result convert() throws DatabaseImporterException {
            Result ret = new Result();
            for (SqlEntry entry : _entries) {
                try {
                    ret.addEntry(entry.convert());
                } catch (DatabaseImporterEntryException e) {
                    ret.addError(e);
                } catch (OtpInfoException e) {
                    throw new DatabaseImporterException(e);
                }
            }

            return ret;
        }
    }

    private static class SqlEntry extends SqlImporterHelper.Entry {
        private final int _type;
        private final String _issuer;
        private final String _username;
        private final byte[] _secret;
        private final Algorithm _algo;
        private final int _digits;
        private final int _period;
        private final int _counter;
        public SqlEntry(Cursor cursor) {
            super(cursor);
            _type = SqlImporterHelper.getInt(cursor, "type");
            _issuer = SqlImporterHelper.getString(cursor, "issuer");
            _username = SqlImporterHelper.getString(cursor, "username");
            String secret = SqlImporterHelper.getString(cursor, "secret");
            try {
                _secret = Base32.decode(secret);
            } catch (EncodingException e) {
                throw new SQLiteException(secret); // Rethrown upstream as DatabaseImporterException
            }
            _algo = Algorithm.values()[SqlImporterHelper.getInt(cursor, "algorithm")];
            _digits = SqlImporterHelper.getInt(cursor, "digits");
            _period = SqlImporterHelper.getInt(cursor, "period");
            _counter = SqlImporterHelper.getInt(cursor, "counter");
        }

        // Used when logging unsupported otp types
        @SuppressLint("DefaultLocale")
        @NotNull
        @Override
        public String toString() {
            return String.format(
                    "Type: %d, Issuer: %s, Username: %s, Secret: %s, Algo: %s, Digits: %d, Period: %d, Counter: %d",
                    _type,
                    _issuer,
                    _username,
                    Base32.encode(_secret),
                    _algo.name(),
                    _digits,
                    _period,
                    _counter
            );
        }

        public VaultEntry convert() throws DatabaseImporterEntryException, OtpInfoException {
            return fromAny(_type, _issuer, _username, _secret, _algo, _digits, _period, _counter, this);
        }
    }
}
