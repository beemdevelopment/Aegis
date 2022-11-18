package com.beemdevelopment.aegis.crypto;

import android.os.Build;

import com.beemdevelopment.aegis.crypto.bc.SCrypt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtils {
    public static final String CRYPTO_AEAD = "AES/GCM/NoPadding";
    public static final byte CRYPTO_AEAD_KEY_SIZE = 32;
    public static final byte CRYPTO_AEAD_TAG_SIZE = 16;
    public static final byte CRYPTO_AEAD_NONCE_SIZE = 12;

    public static final int CRYPTO_SCRYPT_N = 1 << 15;
    public static final int CRYPTO_SCRYPT_r = 8;
    public static final int CRYPTO_SCRYPT_p = 1;

    public static SecretKey deriveKey(byte[] input, SCryptParameters params) {
        byte[] keyBytes = SCrypt.generate(input, params.getSalt(), params.getN(), params.getR(), params.getP(), CRYPTO_AEAD_KEY_SIZE);
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, "AES");
    }

    public static SecretKey deriveKey(char[] password, SCryptParameters params) {
        byte[] bytes = toBytes(password);
        return deriveKey(bytes, params);
    }

    public static Cipher createEncryptCipher(SecretKey key)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException {
        return createCipher(key, Cipher.ENCRYPT_MODE, null);
    }

    public static Cipher createDecryptCipher(SecretKey key, byte[] nonce)
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException,
            InvalidKeyException, NoSuchPaddingException {
        return createCipher(key, Cipher.DECRYPT_MODE, nonce);
    }

    private static Cipher createCipher(SecretKey key, int opmode, byte[] nonce)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance(CRYPTO_AEAD);

        // generate the nonce if none is given
        // we are not allowed to do this ourselves as "setRandomizedEncryptionRequired" is set to true
        if (nonce != null) {
            AlgorithmParameterSpec spec;
            // apparently kitkat doesn't support GCMParameterSpec
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                spec = new IvParameterSpec(nonce);
            } else {
                spec = new GCMParameterSpec(CRYPTO_AEAD_TAG_SIZE * 8, nonce);
            }
            cipher.init(opmode, key, spec);
        } else {
            cipher.init(opmode, key);
        }

        return cipher;
    }

    public static CryptResult encrypt(byte[] data, Cipher cipher)
            throws BadPaddingException, IllegalBlockSizeException {
        // split off the tag to store it separately
        byte[] result = cipher.doFinal(data);
        byte[] tag = Arrays.copyOfRange(result, result.length - CRYPTO_AEAD_TAG_SIZE, result.length);
        byte[] encrypted = Arrays.copyOfRange(result, 0, result.length - CRYPTO_AEAD_TAG_SIZE);

        return new CryptResult(encrypted, new CryptParameters(cipher.getIV(), tag));
    }

    public static CryptResult decrypt(byte[] encrypted, Cipher cipher, CryptParameters params)
            throws IOException, BadPaddingException, IllegalBlockSizeException {
        return decrypt(encrypted, 0, encrypted.length, cipher, params);
    }

    public static CryptResult decrypt(byte[] encrypted, int encryptedOffset, int encryptedLen, Cipher cipher, CryptParameters params)
            throws IOException, BadPaddingException, IllegalBlockSizeException {
        // append the tag to the ciphertext
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(encrypted, encryptedOffset, encryptedLen);
        stream.write(params.getTag());

        encrypted = stream.toByteArray();
        byte[] decrypted = cipher.doFinal(encrypted);

        return new CryptResult(decrypted, params);
    }

    public static SecretKey generateKey() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(CRYPTO_AEAD_KEY_SIZE * 8);
            return generator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    public static byte[] generateSalt() {
        return generateRandomBytes(CRYPTO_AEAD_KEY_SIZE);
    }

    public static byte[] generateRandomBytes(int length) {
        SecureRandom random = new SecureRandom();
        byte[] data = new byte[length];
        random.nextBytes(data);
        return data;
    }

    public static byte[] toBytes(char[] chars) {
        CharBuffer charBuf = CharBuffer.wrap(chars);
        ByteBuffer byteBuf = StandardCharsets.UTF_8.encode(charBuf);
        byte[] bytes = new byte[byteBuf.limit()];
        byteBuf.get(bytes);
        return bytes;
    }

    @Deprecated
    public static byte[] toBytesOld(char[] chars) {
        CharBuffer charBuf = CharBuffer.wrap(chars);
        ByteBuffer byteBuf = StandardCharsets.UTF_8.encode(charBuf);
        return byteBuf.array();
    }
}
