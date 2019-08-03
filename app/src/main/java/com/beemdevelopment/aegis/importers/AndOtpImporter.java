package com.beemdevelopment.aegis.importers;

import android.content.Context;

import com.beemdevelopment.aegis.crypto.CryptParameters;
import com.beemdevelopment.aegis.crypto.CryptResult;
import com.beemdevelopment.aegis.crypto.CryptoUtils;
import com.beemdevelopment.aegis.db.DatabaseEntry;
import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.encoding.Base32Exception;
import com.beemdevelopment.aegis.otp.HotpInfo;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.SteamInfo;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.beemdevelopment.aegis.ui.Dialogs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class AndOtpImporter extends DatabaseImporter {

    public AndOtpImporter(Context context) {
        super(context);
    }

    @Override
    protected String getAppPkgName() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected String getAppSubPath() {
        throw new UnsupportedOperationException();
    }

    @Override
    public State read(FileReader reader) throws DatabaseImporterException {
        byte[] bytes;
        try {
            bytes = reader.readAll();
        } catch (IOException e) {
            throw new DatabaseImporterException(e);
        }

        try {
            return read(bytes);
        } catch (JSONException e) {
            // andOTP doesn't have a proper way to indicate whether a file is encrypted
            // so, if we can't parse it as JSON, we'll have to assume it is
            return new EncryptedState(bytes);
        }
    }

    private static DecryptedState read(byte[] bytes) throws JSONException {
        JSONArray array = new JSONArray(new String(bytes, StandardCharsets.UTF_8));
        return new DecryptedState(array);
    }

    public static class EncryptedState extends DatabaseImporter.State {
        private byte[] _data;

        public EncryptedState(byte[] data) {
            super(true);
            _data = data;
        }

        public DecryptedState decrypt(char[] password) throws DatabaseImporterException {
            try {
                // WARNING: DON'T DO THIS IN YOUR OWN CODE
                // this exists solely to support encrypted andOTP backups
                // it is not a secure way to derive a key from a password
                MessageDigest hash = MessageDigest.getInstance("SHA-256");
                byte[] keyBytes = hash.digest(CryptoUtils.toBytes(password));
                SecretKey key = new SecretKeySpec(keyBytes, "AES");

                // extract nonce and tag
                byte[] nonce = Arrays.copyOfRange(_data, 0, CryptoUtils.CRYPTO_AEAD_NONCE_SIZE);
                byte[] tag = Arrays.copyOfRange(_data, _data.length - CryptoUtils.CRYPTO_AEAD_TAG_SIZE, _data.length);
                CryptParameters params = new CryptParameters(nonce, tag);

                Cipher cipher = CryptoUtils.createDecryptCipher(key, nonce);
                int offset = CryptoUtils.CRYPTO_AEAD_NONCE_SIZE;
                int len = _data.length - CryptoUtils.CRYPTO_AEAD_NONCE_SIZE - CryptoUtils.CRYPTO_AEAD_TAG_SIZE;
                CryptResult result = CryptoUtils.decrypt(_data, offset, len, cipher, params);
                return read(result.getData());
            } catch (IOException | BadPaddingException | JSONException e) {
                throw new DatabaseImporterException(e);
            } catch (NoSuchAlgorithmException
                    | InvalidAlgorithmParameterException
                    | InvalidKeyException
                    | NoSuchPaddingException
                    | IllegalBlockSizeException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void decrypt(Context context, DecryptListener listener) {
            Dialogs.showPasswordInputDialog(context, password -> {
                try {
                    DecryptedState state = decrypt(password);
                    listener.onStateDecrypted(state);
                } catch (DatabaseImporterException e) {
                    listener.onError(e);
                }
            });
        }
    }

    public static class DecryptedState extends DatabaseImporter.State {
        private JSONArray _obj;

        private DecryptedState(JSONArray obj) {
            super(false);
            _obj = obj;
        }

        @Override
        public Result convert() throws DatabaseImporterException {
            Result result = new Result();

            for (int i = 0; i < _obj.length(); i++) {
                try {
                    JSONObject obj = _obj.getJSONObject(i);
                    DatabaseEntry entry = convertEntry(obj);
                    result.addEntry(entry);
                } catch (JSONException e) {
                    throw new DatabaseImporterException(e);
                } catch (DatabaseImporterEntryException e) {
                    result.addError(e);
                }
            }

            return result;
        }

        private static DatabaseEntry convertEntry(JSONObject obj) throws DatabaseImporterEntryException {
            try {
                String type = obj.getString("type").toLowerCase();
                String algo = obj.getString("algorithm");
                int digits = obj.getInt("digits");
                byte[] secret = Base32.decode(obj.getString("secret").toCharArray());

                OtpInfo info;
                switch (type) {
                    case "hotp":
                        info = new HotpInfo(secret, algo, digits, obj.getLong("counter"));
                        break;
                    case "totp":
                        info = new TotpInfo(secret, algo, digits, obj.getInt("period"));
                        break;
                    case "steam":
                        info = new SteamInfo(secret, algo, digits, obj.optInt("period", 30));
                        break;
                    default:
                        throw new DatabaseImporterException("unsupported otp type: " + type);
                }

                String name;
                String issuer = "";

                String[] parts = obj.getString("label").split(" - ");
                if (parts.length > 1) {
                    issuer = parts[0];
                    name = parts[1];
                } else {
                    name = parts[0];
                }

                return new DatabaseEntry(info, name, issuer);
            } catch (DatabaseImporterException | Base32Exception | OtpInfoException | JSONException e) {
                throw new DatabaseImporterEntryException(e, obj.toString());
            }
        }
    }
}
