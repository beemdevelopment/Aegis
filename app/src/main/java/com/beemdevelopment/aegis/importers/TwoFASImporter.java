package com.beemdevelopment.aegis.importers;

import android.content.Context;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.crypto.CryptoUtils;
import com.beemdevelopment.aegis.encoding.Base64;
import com.beemdevelopment.aegis.encoding.EncodingException;
import com.beemdevelopment.aegis.otp.GoogleAuthInfo;
import com.beemdevelopment.aegis.otp.HotpInfo;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.SteamInfo;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.util.IOUtils;
import com.beemdevelopment.aegis.util.JsonUtils;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.google.common.base.Strings;
import com.topjohnwu.superuser.io.SuFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class TwoFASImporter extends DatabaseImporter {
    private static final int ITERATION_COUNT = 10_000;
    private static final int KEY_SIZE = 256; // bits

    public TwoFASImporter(Context context) {
        super(context);
    }

    @Override
    protected SuFile getAppPath() {
        throw new UnsupportedOperationException();
    }

    @Override
    public State read(InputStream stream, boolean isInternal) throws DatabaseImporterException {
        try {
            String json = new String(IOUtils.readAll(stream), StandardCharsets.UTF_8);
            JSONObject obj = new JSONObject(json);
            int version = obj.getInt("schemaVersion");
            if (version > 4) {
                throw new DatabaseImporterException(String.format("Unsupported schema version: %d", version));
            }

            String encryptedString = JsonUtils.optString(obj, "servicesEncrypted");
            if (encryptedString == null) {
                JSONArray array = obj.getJSONArray("services");
                List<JSONObject> entries = arrayToList(array);
                return new DecryptedState(entries);
            }

            String[] parts = encryptedString.split(":");
            if (parts.length < 3) {
                throw new DatabaseImporterException(String.format("Unexpected format of encrypted data (parts: %d)", parts.length));
            }

            byte[] data = Base64.decode(parts[0]);
            byte[] salt = Base64.decode(parts[1]);
            byte[] iv = Base64.decode(parts[2]);
            return new EncryptedState(data, salt, iv);
        } catch (IOException | JSONException e) {
            throw new DatabaseImporterException(e);
        }
    }

    private static List<JSONObject> arrayToList(JSONArray array) throws JSONException {
        List<JSONObject> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            list.add(array.getJSONObject(i));
        }

        return list;
    }

    public static class EncryptedState extends State {
        private final byte[] _data;
        private final byte[] _salt;
        private final byte[] _iv;

        private EncryptedState(byte[] data, byte[] salt, byte[] iv) {
            super(true);
            _data = data;
            _salt = salt;
            _iv = iv;
        }

        private SecretKey deriveKey(char[] password)
                throws NoSuchAlgorithmException, InvalidKeySpecException {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(password, _salt, ITERATION_COUNT, KEY_SIZE);
            SecretKey key = factory.generateSecret(spec);
            return new SecretKeySpec(key.getEncoded(), "AES");
        }

        public DecryptedState decrypt(char[] password) throws DatabaseImporterException {
            try {
                SecretKey key = deriveKey(password);
                Cipher cipher = CryptoUtils.createDecryptCipher(key, _iv);
                byte[] decrypted = cipher.doFinal(_data);
                String json = new String(decrypted, StandardCharsets.UTF_8);
                return new DecryptedState(arrayToList(new JSONArray(json)));
            } catch (BadPaddingException | JSONException e) {
                throw new DatabaseImporterException(e);
            } catch (NoSuchAlgorithmException
                    | InvalidKeySpecException
                    | InvalidAlgorithmParameterException
                    | NoSuchPaddingException
                    | InvalidKeyException
                    | IllegalBlockSizeException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void decrypt(Context context, DecryptListener listener) {
            Dialogs.showPasswordInputDialog(context, R.string.enter_password_2fas_message, 0, password -> {
                try {
                    DecryptedState state = decrypt(password);
                    listener.onStateDecrypted(state);
                } catch (DatabaseImporterException e) {
                    listener.onError(e);
                }
            }, dialog -> listener.onCanceled());
        }
    }

    public static class DecryptedState extends DatabaseImporter.State {
        private final List<JSONObject> _entries;

        public DecryptedState(List<JSONObject> entries) {
            super(false);
            _entries = entries;
        }

        @Override
        public Result convert() {
            Result result = new Result();

            for (JSONObject obj : _entries) {
                try {
                    VaultEntry entry = convertEntry(obj);
                    result.addEntry(entry);
                } catch (DatabaseImporterEntryException e) {
                    result.addError(e);
                }
            }

            return result;
        }

        private static VaultEntry convertEntry(JSONObject obj) throws DatabaseImporterEntryException {
            try {
                byte[] secret = GoogleAuthInfo.parseSecret(obj.getString("secret"));
                JSONObject info = obj.getJSONObject("otp");
                String issuer = obj.optString("name");
                if (Strings.isNullOrEmpty(issuer)) {
                    issuer = info.optString("issuer");
                }
                String name = info.optString("account");
                int digits = info.optInt("digits", TotpInfo.DEFAULT_DIGITS);
                String algorithm = info.optString("algorithm", TotpInfo.DEFAULT_ALGORITHM);

                OtpInfo otp;
                String tokenType = JsonUtils.optString(info, "tokenType");
                if (tokenType == null || tokenType.equals("TOTP")) {
                    int period = info.optInt("period", TotpInfo.DEFAULT_PERIOD);
                    otp = new TotpInfo(secret, algorithm, digits, period);
                } else if (tokenType.equals("HOTP")) {
                    long counter = info.optLong("counter", 0);
                    otp = new HotpInfo(secret, algorithm, digits, counter);
                } else if (tokenType.equals("STEAM")) {
                    int period = info.optInt("period", TotpInfo.DEFAULT_PERIOD);
                    otp = new SteamInfo(secret, algorithm, digits, period);
                } else {
                    throw new DatabaseImporterEntryException(String.format("Unrecognized tokenType: %s", tokenType), obj.toString());
                }

                return new VaultEntry(otp, name, issuer);
            } catch (OtpInfoException | JSONException | EncodingException e) {
                throw new DatabaseImporterEntryException(e, obj.toString());
            }
        }
    }
}
