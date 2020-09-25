package com.beemdevelopment.aegis.ui.tasks;

import android.content.Context;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.crypto.CryptoUtils;
import com.beemdevelopment.aegis.crypto.SCryptParameters;
import com.beemdevelopment.aegis.vault.slots.PasswordSlot;

import javax.crypto.SecretKey;

public class KeyDerivationTask extends ProgressDialogTask<KeyDerivationTask.Params, KeyDerivationTask.Result> {
    private Callback _cb;

    public KeyDerivationTask(Context context, Callback cb) {
        super(context, context.getString(R.string.encrypting_vault));
        _cb = cb;
    }

    @Override
    protected Result doInBackground(KeyDerivationTask.Params... args) {
        setPriority();

        Params params = args[0];
        byte[] salt = CryptoUtils.generateSalt();
        SCryptParameters scryptParams = new SCryptParameters(
                CryptoUtils.CRYPTO_SCRYPT_N,
                CryptoUtils.CRYPTO_SCRYPT_r,
                CryptoUtils.CRYPTO_SCRYPT_p,
                salt
        );

        PasswordSlot slot = params.getSlot();
        SecretKey key = slot.deriveKey(params.getPassword(), scryptParams);
        return new Result(slot, key);
    }

    @Override
    protected void onPostExecute(Result result) {
        super.onPostExecute(result);
        _cb.onTaskFinished(result.getSlot(), result.getKey());
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

    public static class Result {
        private PasswordSlot _slot;
        private SecretKey _key;

        public Result(PasswordSlot slot, SecretKey key) {
            _slot = slot;
            _key = key;
        }

        public PasswordSlot getSlot() {
            return _slot;
        }

        public SecretKey getKey() {
            return _key;
        }
    }

    public interface Callback {
        void onTaskFinished(PasswordSlot slot, SecretKey key);
    }
}
