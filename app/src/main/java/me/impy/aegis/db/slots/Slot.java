package me.impy.aegis.db.slots;

import android.annotation.SuppressLint;

import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import me.impy.aegis.crypto.CryptoUtils;
import me.impy.aegis.crypto.MasterKey;
import me.impy.aegis.encoding.Hex;

public abstract class Slot implements Serializable {
    public final static byte TYPE_RAW = 0x00;
    public final static byte TYPE_DERIVED = 0x01;
    public final static byte TYPE_FINGERPRINT = 0x02;
    public final static int ID_SIZE = 16;

    protected byte[] _id;
    protected byte[] _encryptedMasterKey;

    protected Slot() {
        _id = CryptoUtils.generateRandomBytes(ID_SIZE);
    }

    // getKey decrypts the encrypted master key in this slot with the given key and returns it.
    public SecretKey getKey(Cipher cipher) throws BadPaddingException, IllegalBlockSizeException {
        byte[] decryptedKeyBytes = cipher.doFinal(_encryptedMasterKey);
        return new SecretKeySpec(decryptedKeyBytes, CryptoUtils.CRYPTO_CIPHER_AEAD);
    }

    // setKey encrypts the given master key with the given key and stores the result in this slot.
    public void setKey(MasterKey masterKey, Cipher cipher) throws BadPaddingException, IllegalBlockSizeException {
        byte[] masterKeyBytes = masterKey.getBytes();
        _encryptedMasterKey = cipher.doFinal(masterKeyBytes);
    }

    // suppress the AES ECB warning
    // this is perfectly safe because we discard this cipher after passing CryptoUtils.CRYPTO_KEY_SIZE bytes through it
    @SuppressLint("getInstance")
    public static Cipher createCipher(SecretKey key, int mode) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance(CryptoUtils.CRYPTO_CIPHER_RAW);
        cipher.init(mode, key);
        return cipher;
    }

    public String getID() {
        return Hex.toString(_id);
    }

    public abstract int getSize();
    public abstract byte getType();

    // a slot has a binary representation
    public abstract byte[] serialize();
    public abstract void deserialize(byte[] data) throws Exception;
}
