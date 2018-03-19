package me.impy.aegis.ui.tasks;

import android.content.Context;
import android.os.Process;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKey;

import me.impy.aegis.crypto.CryptoUtils;
import me.impy.aegis.db.slots.PasswordSlot;
import me.impy.aegis.db.slots.SlotException;

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
            return params.Slot.deriveKey(params.Password, salt, CryptoUtils.CRYPTO_SCRYPT_N, CryptoUtils.CRYPTO_SCRYPT_r, CryptoUtils.CRYPTO_SCRYPT_p);
        } catch (SlotException e) {
            return null;
        }
    }

    @Override
    protected void onPostExecute(SecretKey key) {
        super.onPostExecute(key);
        _cb.onTaskFinished(key);
    }

    public static class Params {
        public PasswordSlot Slot;
        public char[] Password;
    }

    public interface Callback {
        void onTaskFinished(SecretKey key);
    }
}
