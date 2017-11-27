package me.impy.aegis;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import javax.crypto.SecretKey;

import me.impy.aegis.crypto.CryptoUtils;
import me.impy.aegis.crypto.slots.PasswordSlot;

public class DerivationTask extends AsyncTask<DerivationTask.Params, Void, SecretKey> {
    private Callback _cb;
    private ProgressDialog _dialog;

    public DerivationTask(Context context, Callback cb) {
        _cb = cb;
        _dialog = new ProgressDialog(context);
        _dialog.setCancelable(false);
    }

    @Override
    protected SecretKey doInBackground(DerivationTask.Params... args) {
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
    protected void onPreExecute() {
        _dialog.setMessage("Deriving key from password");
        _dialog.show();
    }

    @Override
    protected void onPostExecute(SecretKey key) {
        if (_dialog.isShowing()) {
            _dialog.dismiss();
        }
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
