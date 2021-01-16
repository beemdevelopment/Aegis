package com.beemdevelopment.aegis.vault.slots;

import com.beemdevelopment.aegis.crypto.CryptoUtils;
import com.beemdevelopment.aegis.crypto.MasterKey;
import com.beemdevelopment.aegis.crypto.SCryptParameters;

import org.junit.Before;
import org.junit.Test;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;

public class SlotTest {
    private MasterKey _masterKey;

    @Before
    public void init() {
        _masterKey = MasterKey.generate();
    }

    @Test
    public void testRawSlotCrypto() throws
            InvalidAlgorithmParameterException, NoSuchAlgorithmException,
            InvalidKeyException, NoSuchPaddingException,
            SlotException, SlotIntegrityException {
        RawSlot slot = new RawSlot();
        SecretKey rawKey = CryptoUtils.generateKey();
        Cipher cipher = CryptoUtils.createEncryptCipher(rawKey);
        slot.setKey(_masterKey, cipher);

        cipher = slot.createDecryptCipher(rawKey);
        MasterKey decryptedKey = slot.getKey(cipher);

        assertArrayEquals(_masterKey.getBytes(), decryptedKey.getBytes());
    }

    @Test
    public void testPasswordSlotCrypto() throws
            InvalidAlgorithmParameterException, NoSuchAlgorithmException,
            InvalidKeyException, NoSuchPaddingException,
            SlotException, SlotIntegrityException {
        final char[] password = "test".toCharArray();
        final SCryptParameters scryptParams = new SCryptParameters(
                CryptoUtils.CRYPTO_SCRYPT_N,
                CryptoUtils.CRYPTO_SCRYPT_p,
                CryptoUtils.CRYPTO_SCRYPT_r,
                new byte[CryptoUtils.CRYPTO_AEAD_KEY_SIZE]
        );

        PasswordSlot slot = new PasswordSlot();
        SecretKey passwordKey = slot.deriveKey(password, scryptParams);
        Cipher cipher = CryptoUtils.createEncryptCipher(passwordKey);
        slot.setKey(_masterKey, cipher);

        cipher = slot.createDecryptCipher(passwordKey);
        MasterKey decryptedKey = slot.getKey(cipher);

        assertArrayEquals(_masterKey.getBytes(), decryptedKey.getBytes());
    }

    @Test
    public void testSlotIntegrity() throws
            InvalidAlgorithmParameterException, NoSuchAlgorithmException,
            InvalidKeyException, NoSuchPaddingException,
            SlotException, SlotIntegrityException {
        RawSlot slot = new RawSlot();
        SecretKey rawKey = CryptoUtils.generateKey();
        Cipher cipher = CryptoUtils.createEncryptCipher(rawKey);
        slot.setKey(_masterKey, cipher);

        // try to decrypt with good key/ciphertext first
        final Cipher decryptCipher = slot.createDecryptCipher(rawKey);
        slot.getKey(decryptCipher);

        // garble the first byte of the key and try to decrypt
        byte[] garbledKeyBytes = rawKey.getEncoded();
        garbledKeyBytes[0] = (byte) ~garbledKeyBytes[0];
        SecretKey garbledKey = new SecretKeySpec(garbledKeyBytes, "AES");
        final Cipher garbledDecryptCipher = slot.createDecryptCipher(garbledKey);
        assertThrows(SlotIntegrityException.class, () -> slot.getKey(garbledDecryptCipher));

        // garble the first byte of the ciphertext and try to decrypt
        byte[] garbledCiphertext = slot.getEncryptedMasterKey();
        garbledCiphertext[0] = (byte) ~garbledCiphertext[0];
        assertThrows(SlotIntegrityException.class, () -> slot.getKey(decryptCipher));
    }
}
