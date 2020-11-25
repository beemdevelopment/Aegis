package com.beemdevelopment.aegis.importers;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.Lifecycle;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.crypto.CryptParameters;
import com.beemdevelopment.aegis.crypto.CryptResult;
import com.beemdevelopment.aegis.crypto.CryptoUtils;
import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.encoding.EncodingException;
import com.beemdevelopment.aegis.helpers.ContextHelper;
import com.beemdevelopment.aegis.otp.HotpInfo;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.SteamInfo;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.beemdevelopment.aegis.ui.Dialogs;
import com.beemdevelopment.aegis.ui.tasks.ProgressDialogTask;
import com.beemdevelopment.aegis.util.IOUtils;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.topjohnwu.superuser.io.SuFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class AndOtpImporter extends DatabaseImporter {
    private static final int INT_SIZE = 4;
    private static final int NONCE_SIZE = 12;
    private static final int TAG_SIZE = 16;
    private static final int SALT_SIZE = 12;
    private static final int KEY_SIZE = 256; // bits

    public AndOtpImporter(Context context) {
        super(context);
    }

    @Override
    protected SuFile getAppPath() {
        throw new UnsupportedOperationException();
    }

    @Override
    public State read(InputStream stream, boolean isInternal) throws DatabaseImporterException {
        byte[] bytes;
        try {
            bytes = IOUtils.readAll(stream);
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

        private DecryptedState decryptData(SecretKey key, int offset) throws DatabaseImporterException {
            byte[] nonce = Arrays.copyOfRange(_data, offset, offset + NONCE_SIZE);
            byte[] tag = Arrays.copyOfRange(_data, _data.length - TAG_SIZE, _data.length);
            CryptParameters params = new CryptParameters(nonce, tag);

            try {
                Cipher cipher = CryptoUtils.createDecryptCipher(key, nonce);
                int len = _data.length - offset - NONCE_SIZE - TAG_SIZE;
                CryptResult result = CryptoUtils.decrypt(_data, offset + NONCE_SIZE, len, cipher, params);
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

        private void decrypt(Context context, char[] password, boolean oldFormat, DecryptListener listener) throws DatabaseImporterException {
            if (oldFormat) {
                // WARNING: DON'T DO THIS IN YOUR OWN CODE
                // this exists solely to support the old andOTP backup format
                // it is not a secure way to derive a key from a password
                MessageDigest hash;
                try {
                    hash = MessageDigest.getInstance("SHA-256");
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
                byte[] keyBytes = hash.digest(CryptoUtils.toBytes(password));
                SecretKey key = new SecretKeySpec(keyBytes, "AES");
                DecryptedState state = decryptData(key, 0);
                listener.onStateDecrypted(state);
            } else {
                int offset = INT_SIZE + SALT_SIZE;

                byte[] iterBytes = Arrays.copyOfRange(_data, 0, INT_SIZE);
                int iterations = ByteBuffer.wrap(iterBytes).getInt();
                if (iterations < 1) {
                    throw new DatabaseImporterException(String.format("Invalid number of iterations for PBKDF: %d", iterations));
                }

                byte[] salt = Arrays.copyOfRange(_data, INT_SIZE, offset);
                AndOtpKeyDerivationTask.Params params = new AndOtpKeyDerivationTask.Params(password, salt, iterations);
                AndOtpKeyDerivationTask task = new AndOtpKeyDerivationTask(context, key1 -> {
                    try {
                        DecryptedState state = decryptData(key1, offset);
                        listener.onStateDecrypted(state);
                    } catch (DatabaseImporterException e) {
                        listener.onError(e);
                    }
                });
                Lifecycle lifecycle = ContextHelper.getLifecycle(context);
                task.execute(lifecycle, params);
            }
        }

        @Override
        public void decrypt(Context context, DecryptListener listener) {
            String[] choices = new String[]{
                    context.getResources().getString(R.string.andotp_new_format),
                    context.getResources().getString(R.string.andotp_old_format)
            };

            Dialogs.showSecureDialog(new AlertDialog.Builder(context)
                    .setTitle(R.string.choose_andotp_importer)
                    .setSingleChoiceItems(choices, 0, null)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        int i = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        Dialogs.showPasswordInputDialog(context, password -> {
                            try {
                                decrypt(context, password, i != 0, listener);
                            } catch (DatabaseImporterException e) {
                                listener.onError(e);
                            }
                        });
                    })
                    .create());
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
                    VaultEntry entry = convertEntry(obj);
                    result.addEntry(entry);
                } catch (JSONException e) {
                    throw new DatabaseImporterException(e);
                } catch (DatabaseImporterEntryException e) {
                    result.addError(e);
                }
            }

            return result;
        }

        private static VaultEntry convertEntry(JSONObject obj) throws DatabaseImporterEntryException {
            try {
                String type = obj.getString("type").toLowerCase();
                String algo = obj.getString("algorithm");
                int digits = obj.getInt("digits");
                byte[] secret = Base32.decode(obj.getString("secret"));

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

                if (obj.has("issuer")) {
                    name = obj.getString("label");
                    issuer = obj.getString("issuer");
                } else {
                    String[] parts = obj.getString("label").split(" - ");
                    if (parts.length > 1) {
                        issuer = parts[0];
                        name = parts[1];
                    } else {
                        name = parts[0];
                    }
                }

                return new VaultEntry(info, name, issuer);
            } catch (DatabaseImporterException | EncodingException | OtpInfoException | JSONException e) {
                throw new DatabaseImporterEntryException(e, obj.toString());
            }
        }
    }

    private static class AndOtpKeyDerivationTask extends ProgressDialogTask<AndOtpKeyDerivationTask.Params, SecretKey> {
        private Callback _cb;

        public AndOtpKeyDerivationTask(Context context, Callback cb) {
            super(context, context.getString(R.string.unlocking_vault));
            _cb = cb;
        }

        @Override
        protected SecretKey doInBackground(AndOtpKeyDerivationTask.Params... args) {
            setPriority();

            AndOtpKeyDerivationTask.Params params = args[0];
            SecretKey key;
            try {
                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                KeySpec spec = new PBEKeySpec(params.getPassword(), params.getSalt(), params.getIterations(), KEY_SIZE);
                key = factory.generateSecret(spec);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                throw new RuntimeException(e);
            }

            return key;
        }

        @Override
        protected void onPostExecute(SecretKey key) {
            super.onPostExecute(key);
            _cb.onTaskFinished(key);
        }

        public static class Params {
            private char[] _password;
            private byte[] _salt;
            private int _iterations;

            public Params(char[] password, byte[] salt, int iterations) {
                _iterations = iterations;
                _password = password;
                _salt = salt;
            }

            public char[] getPassword() {
                return _password;
            }

            public int getIterations() {
                return _iterations;
            }

            public byte[] getSalt() {
                return _salt;
            }
        }

        public interface Callback {
            void onTaskFinished(SecretKey key);
        }
    }
}
