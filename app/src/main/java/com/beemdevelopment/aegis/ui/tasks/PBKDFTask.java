package com.beemdevelopment.aegis.ui.tasks;

import android.content.Context;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.crypto.CryptoUtils;

import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class PBKDFTask extends ProgressDialogTask<PBKDFTask.Params, SecretKey> {
    private final Callback _cb;

    public PBKDFTask(Context context, Callback cb) {
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
        try {
            // Some older versions of Android (< 26) do not support PBKDF2withHmacSHA512, so use
            // BouncyCastle's implementation instead.
            if (params.getAlgorithm().equals("PBKDF2withHmacSHA512")) {
                PKCS5S2ParametersGenerator gen = new PKCS5S2ParametersGenerator(new SHA512Digest());
                gen.init(CryptoUtils.toBytes(params.getPassword()), params.getSalt(), params.getIterations());
                byte[] key = ((KeyParameter) gen.generateDerivedParameters(params.getKeySize())).getKey();
                return new SecretKeySpec(key, "AES");
            }

            SecretKeyFactory factory = SecretKeyFactory.getInstance(params.getAlgorithm());
            KeySpec spec = new PBEKeySpec(params.getPassword(), params.getSalt(), params.getIterations(), params.getKeySize());
            SecretKey key = factory.generateSecret(spec);
            return new SecretKeySpec(key.getEncoded(), "AES");
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
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
        private final String _algorithm;
        private final int _keySize;
        private final char[] _password;
        private final byte[] _salt;
        private final int _iterations;

        public Params(String algorithm, int keySize, char[] password, byte[] salt, int iterations) {
            _algorithm = algorithm;
            _keySize = keySize;
            _iterations = iterations;
            _password = password;
            _salt = salt;
        }

        public String getAlgorithm() {
            return _algorithm;
        }

        public int getKeySize() {
            return _keySize;
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
}
