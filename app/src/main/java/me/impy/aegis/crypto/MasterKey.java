package me.impy.aegis.crypto;

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

import me.impy.aegis.crypto.slots.Slot;

public class MasterKey implements Serializable {
    private SecretKey _key;

    public MasterKey(SecretKey key)  {
        if (key == null) {
            throw new NullPointerException();
        }
        _key = key;
    }

    public static MasterKey generate() throws NoSuchAlgorithmException {
        return new MasterKey(CryptoUtils.generateKey());
    }

    public void encryptSlot(Slot slot, Cipher cipher)
            throws BadPaddingException, IllegalBlockSizeException {
        slot.setKey(_key, cipher);
    }

    public static MasterKey decryptSlot(Slot slot, Cipher cipher)
            throws BadPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException {
        return new MasterKey(slot.getKey(cipher));
    }

    public CryptResult encrypt(byte[] bytes)
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, InvalidKeyException,
            NoSuchPaddingException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = CryptoUtils.createCipher(_key, Cipher.ENCRYPT_MODE);
        return CryptoUtils.encrypt(bytes, cipher);
    }

    public CryptResult decrypt(byte[] bytes, CryptParameters params)
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, InvalidKeyException,
            NoSuchPaddingException, BadPaddingException, IllegalBlockSizeException, IOException {
        Cipher cipher = CryptoUtils.createCipher(_key, Cipher.DECRYPT_MODE, params.Nonce);
        return CryptoUtils.decrypt(bytes, cipher, params);
    }
}
