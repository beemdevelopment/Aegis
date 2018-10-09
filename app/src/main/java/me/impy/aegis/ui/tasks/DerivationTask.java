package me.impy.aegis.ui.tasks;

import android.content.Context;

import javax.crypto.SecretKey;

import me.impy.aegis.R;
import me.impy.aegis.crypto.CryptoUtils;
import me.impy.aegis.crypto.SCryptParameters;
import me.impy.aegis.db.slots.PasswordSlot;

public class DerivationTask extends ProgressDialogTask<DerivationTask.Params, SecretKey> {
    private Callback _cb;

    public DerivationTask(Context context, Callback cb) {
        super(context, context.getString(R.string.encrypting_vault));
        _cb = cb;
    }

    @Override
    protected SecretKey doInBackground(DerivationTask.Params... args) {
        setPriority();

        Params params = args[0];
        byte[] salt = CryptoUtils.generateSalt();
        SCryptParameters scryptParams = new SCryptParameters(
                CryptoUtils.CRYPTO_SCRYPT_N,
                CryptoUtils.CRYPTO_SCRYPT_r,
                CryptoUtils.CRYPTO_SCRYPT_p,
                salt
        );
        return params.getSlot().deriveKey(params.getPassword(), scryptParams);
    }

    @Override
    protected void onPostExecute(SecretKey key) {
        super.onPostExecute(key);
        _cb.onTaskFinished(key);
    }

    public static class Params {
        private PasswordSlot _slot;
        private char[] _password;

        public Params(PasswordSlot slot, char[] password) {
            _slot = slot;
            _password = password;
        }

        public PasswordSlot getSlot() {
            return _slot;
        }

        public char[] getPassword() {
            return _password;
        }
    }

    public interface Callback {
        void onTaskFinished(SecretKey key);
    }
}
