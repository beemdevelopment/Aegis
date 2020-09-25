package com.beemdevelopment.aegis.crypto;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.ProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Collections;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class KeyStoreHandle {
    private final KeyStore _keyStore;
    private static final String STORE_NAME = "AndroidKeyStore";

    public KeyStoreHandle() throws KeyStoreHandleException {
        try {
            _keyStore = KeyStore.getInstance(STORE_NAME);
            _keyStore.load(null);
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            throw new KeyStoreHandleException(e);
        }
    }

    public boolean containsKey(String id) throws KeyStoreHandleException {
        try {
            return _keyStore.containsAlias(id);
        } catch (KeyStoreException e) {
            throw new KeyStoreHandleException(e);
        }
    }

    public SecretKey generateKey(String id) throws KeyStoreHandleException {
        if (!isSupported()) {
            throw new KeyStoreHandleException("Symmetric KeyStore keys are not supported in this version of Android");
        }

        try {
            KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, STORE_NAME);
            generator.init(new KeyGenParameterSpec.Builder(id,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setUserAuthenticationRequired(true)
                    .setRandomizedEncryptionRequired(true)
                    .setKeySize(CryptoUtils.CRYPTO_AEAD_KEY_SIZE * 8)
                    .build());

            return generator.generateKey();
        } catch (ProviderException e) {
            // a ProviderException can occur at runtime with buggy Keymaster HAL implementations
            // so if this was caused by an android.security.KeyStoreException, throw a KeyStoreHandleException instead
            Throwable cause = e.getCause();
            if (cause != null && cause.getClass().getName().equals("android.security.KeyStoreException")) {
                throw new KeyStoreHandleException(cause);
            }
            throw e;
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
            throw new KeyStoreHandleException(e);
        }
    }

    public SecretKey getKey(String id) throws KeyStoreHandleException {
        SecretKey key;

        try {
            key = (SecretKey) _keyStore.getKey(id, null);
        } catch (UnrecoverableKeyException e) {
            return null;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (KeyStoreException e) {
            throw new KeyStoreHandleException(e);
        }

        if (isSupported() && isKeyPermanentlyInvalidated(key)) {
            return null;
        }

        return key;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static boolean isKeyPermanentlyInvalidated(SecretKey key) {
        // try to initialize a dummy cipher and see if an InvalidKeyException is thrown
        try {
            Cipher cipher = Cipher.getInstance(CryptoUtils.CRYPTO_AEAD);
            cipher.init(Cipher.ENCRYPT_MODE, key);
        } catch (InvalidKeyException e) {
            // some devices throw a plain InvalidKeyException, not KeyPermanentlyInvalidatedException
            return true;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }

        return false;
    }

    public void deleteKey(String id) throws KeyStoreHandleException {
        try {
            _keyStore.deleteEntry(id);
        } catch (KeyStoreException e) {
            throw new KeyStoreHandleException(e);
        }
    }

    public void clear() throws KeyStoreHandleException {
        try {
            for (String alias : Collections.list(_keyStore.aliases())) {
                deleteKey(alias);
            }
        } catch (KeyStoreException e) {
            throw new KeyStoreHandleException(e);
        }
    }

    public static boolean isSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }
}
