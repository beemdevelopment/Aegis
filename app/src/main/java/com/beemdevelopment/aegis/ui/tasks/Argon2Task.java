package com.beemdevelopment.aegis.ui.tasks;

import android.content.Context;

import com.beemdevelopment.aegis.R;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Argon2Task extends ProgressDialogTask<Argon2Task.Params, SecretKey> {
    private final Callback _cb;

    public Argon2Task(Context context, Callback cb) {
        super(context, context.getString(R.string.unlocking_vault));
        _cb = cb;
    }

    @Override
    protected SecretKey doInBackground(Params... args) {
        setPriority();

        Params params = args[0];
        return deriveKey(params);
    }

    public static SecretKey deriveKey(Params params) {
        Argon2BytesGenerator gen = new Argon2BytesGenerator();
        gen.init(params.getArgon2Params());

        byte[] key = new byte[params.getKeySize()];
        gen.generateBytes(params.getPassword(), key);
        return new SecretKeySpec(key, 0, key.length, "AES");
    }

    @Override
    protected void onPostExecute(SecretKey key) {
        super.onPostExecute(key);
        _cb.onTaskFinished(key);
    }

    public interface Callback {
        void onTaskFinished(SecretKey key);
    }

    public static class Params {
        private final char[] _password;
        private final Argon2Parameters _argon2Params;
        private final int _keySize;

        public Params(char[] password, Argon2Parameters argon2Params, int keySize) {
            _password = password;
            _argon2Params = argon2Params;
            _keySize = keySize;
        }

        public char[] getPassword() {
            return _password;
        }

        public Argon2Parameters getArgon2Params() {
            return _argon2Params;
        }

        public int getKeySize() {
            return _keySize;
        }
    }
}
