package com.beemdevelopment.aegis;

import com.beemdevelopment.aegis.crypto.CryptoUtils;
import com.beemdevelopment.aegis.crypto.MasterKey;
import com.beemdevelopment.aegis.crypto.SCryptParameters;
import com.beemdevelopment.aegis.vault.slots.PasswordSlot;
import com.beemdevelopment.aegis.vault.slots.RawSlot;
import com.beemdevelopment.aegis.vault.slots.SlotException;
import com.beemdevelopment.aegis.vault.slots.SlotIntegrityException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SlotTest {
    private MasterKey _masterKey;

    @BeforeEach
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
            SlotException {
        RawSlot slot = new RawSlot();
        SecretKey rawKey = CryptoUtils.generateKey();
        Cipher cipher = CryptoUtils.createEncryptCipher(rawKey);
        slot.setKey(_masterKey, cipher);

        // garble the first byte of the key
        byte[] rawKeyBytes = rawKey.getEncoded();
        rawKeyBytes[0] = (byte) ~rawKeyBytes[0];
        rawKey = new SecretKeySpec(rawKeyBytes, "AES");

        Cipher decryptCipher = slot.createDecryptCipher(rawKey);
        assertThrows(SlotIntegrityException.class, () -> slot.getKey(decryptCipher));
    }
}
