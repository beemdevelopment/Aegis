package me.impy.aegis.crypto;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class KeyStoreHandle {
    private final KeyStore keyStore;
    private static final String KEY_NAME = "AegisKey";
    private static final String STORE_NAME = "AndroidKeyStore";

    public KeyStoreHandle() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        keyStore = KeyStore.getInstance(STORE_NAME);
        keyStore.load(null);
    }

    public boolean keyExists() throws KeyStoreException {
        return keyStore.containsAlias(KEY_NAME);
    }

    public SecretKey generateKey(boolean authRequired) throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, STORE_NAME);
            generator.init(new KeyGenParameterSpec.Builder(KEY_NAME,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(authRequired)
                .setRandomizedEncryptionRequired(false)
                .setKeySize(CryptoUtils.CRYPTO_KEY_SIZE * 8)
                .build());

            return generator.generateKey();
        } else {
            throw new Exception("Symmetric KeyStore keys are not supported in this version of Android");
        }
    }

    public SecretKey getKey() throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
        return (SecretKey) keyStore.getKey(KEY_NAME, null);
    }
}
