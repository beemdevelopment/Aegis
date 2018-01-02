package me.impy.aegis;

import android.content.Context;
import android.os.Process;

import javax.crypto.SecretKey;

import me.impy.aegis.crypto.CryptoUtils;
import me.impy.aegis.crypto.slots.PasswordSlot;

public class DerivationTask extends ProgressDialogTask<DerivationTask.Params, SecretKey> {
    private Callback _cb;

    public DerivationTask(Context context, Callback cb) {
        super(context, "Deriving key from password");
        _cb = cb;
    }

    @Override
    protected SecretKey doInBackground(DerivationTask.Params... args) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_MORE_FAVORABLE);

        DerivationTask.Params params = args[0];
        try {
            byte[] salt = CryptoUtils.generateSalt();
            SecretKey key = params.Slot.deriveKey(params.Password, salt, CryptoUtils.CRYPTO_SCRYPT_N, CryptoUtils.CRYPTO_SCRYPT_r, CryptoUtils.CRYPTO_SCRYPT_p);
            CryptoUtils.zero(params.Password);
            return key;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void onPostExecute(SecretKey key) {
        super.onPostExecute(key);
        _cb.onTaskFinished(key);
    }

    static class Params {
        public PasswordSlot Slot;
        public char[] Password;
    }

    interface Callback {
        void onTaskFinished(SecretKey key);
    }
}
