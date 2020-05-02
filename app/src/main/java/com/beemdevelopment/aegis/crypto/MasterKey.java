package com.beemdevelopment.aegis.crypto;

import java.io.IOException;
import java.io.Serializable;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class MasterKey implements Serializable {
    private SecretKey _key;

    public MasterKey(SecretKey key)  {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        _key = key;
    }

    public static MasterKey generate() {
        return new MasterKey(CryptoUtils.generateKey());
    }

    public CryptResult encrypt(byte[] bytes) throws MasterKeyException {
        try {
            Cipher cipher = CryptoUtils.createEncryptCipher(_key);
            return CryptoUtils.encrypt(bytes, cipher);
        } catch (NoSuchPaddingException
                | NoSuchAlgorithmException
                | InvalidAlgorithmParameterException
                | InvalidKeyException
                | BadPaddingException
                | IllegalBlockSizeException e) {
            throw new MasterKeyException(e);
        }
    }

    public CryptResult decrypt(byte[] bytes, CryptParameters params) throws MasterKeyException {
        try {
            Cipher cipher = CryptoUtils.createDecryptCipher(_key, params.getNonce());
            return CryptoUtils.decrypt(bytes, cipher, params);
        } catch (NoSuchPaddingException
                | NoSuchAlgorithmException
                | InvalidAlgorithmParameterException
                | InvalidKeyException
                | BadPaddingException
                | IOException
                | IllegalBlockSizeException e) {
            throw new MasterKeyException(e);
        }
    }

    public byte[] getBytes() {
        return _key.getEncoded();
    }
}
