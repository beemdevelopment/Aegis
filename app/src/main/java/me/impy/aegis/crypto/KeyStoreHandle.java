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

import me.impy.aegis.crypto.slots.FingerprintSlot;

public class KeyStoreHandle {
    private final KeyStore _keyStore;
    private static final String STORE_NAME = "AndroidKeyStore";

    public KeyStoreHandle() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        _keyStore = KeyStore.getInstance(STORE_NAME);
        _keyStore.load(null);
    }

    public boolean containsKey(String id) throws KeyStoreException {
        return _keyStore.containsAlias(id);
    }

    public SecretKey generateKey(String id) throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, STORE_NAME);
            generator.init(new KeyGenParameterSpec.Builder(id,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(true)
                .setRandomizedEncryptionRequired(false)
                .setKeySize(CryptoUtils.CRYPTO_KEY_SIZE * 8)
                .build());

            return generator.generateKey();
        } else {
            throw new Exception("Symmetric KeyStore keys are not supported in this version of Android");
        }
    }

    public SecretKey getKey(String id) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
        return (SecretKey) _keyStore.getKey(id, null);
    }
}
