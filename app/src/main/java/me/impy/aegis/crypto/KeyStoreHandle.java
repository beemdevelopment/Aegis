package me.impy.aegis.crypto;

import android.annotation.SuppressLint;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

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
                    .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setUserAuthenticationRequired(true)
                    .setRandomizedEncryptionRequired(false)
                    .setKeySize(CryptoUtils.CRYPTO_KEY_SIZE * 8)
                    .build());

            return generator.generateKey();
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

        // try to initialize a dummy cipher
        // and see if KeyPermanentlyInvalidatedException is thrown
        if (isSupported()) {
            try {
                @SuppressLint("GetInstance")
                Cipher cipher = Cipher.getInstance(CryptoUtils.CRYPTO_CIPHER_RAW);
                cipher.init(Cipher.ENCRYPT_MODE, key);
            } catch (KeyPermanentlyInvalidatedException e) {
                return null;
            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
                throw new RuntimeException(e);
            }
        }

        return key;
    }

    public void deleteKey(String id) throws KeyStoreHandleException {
        try {
            _keyStore.deleteEntry(id);
        } catch (KeyStoreException e) {
            throw new KeyStoreHandleException(e);
        }
    }

    public static boolean isSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }
}
