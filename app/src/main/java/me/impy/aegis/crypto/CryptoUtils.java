package me.impy.aegis.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.spongycastle.crypto.generators.SCrypt;

public class CryptoUtils {
    public static final String CRYPTO_HASH = "SHA-256";
    public static final byte CRYPTO_HASH_SIZE = 32;

    public static final String CRYPTO_CIPHER_RAW = "AES/ECB/NoPadding";
    public static final byte CRYPTO_KEY_SIZE = 32;

    public static final String CRYPTO_CIPHER_AEAD = "AES/GCM/NoPadding";
    public static final byte CRYPTO_TAG_SIZE = 16;
    public static final byte CRYPTO_NONCE_SIZE = 12;
    public static final byte CRYPTO_SALT_SIZE = 32;

    public static final int CRYPTO_SCRYPT_N = 1 << 15;
    public static final int CRYPTO_SCRYPT_r = 8;
    public static final int CRYPTO_SCRYPT_p = 1;

    public static SecretKey deriveKey(char[] password, byte[] salt, int n, int r, int p) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] bytes = toBytes(password);
        byte[] keyBytes = SCrypt.generate(bytes, salt, n, r, p, CRYPTO_KEY_SIZE);
        zero(bytes);
        SecretKey key = new SecretKeySpec(keyBytes, 0, keyBytes.length, "AES");
        zero(keyBytes);
        return key;
    }

    public static Cipher createCipher(SecretKey key, int opmode) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        byte[] nonce = generateNonce();
        return createCipher(key, opmode, nonce);
    }

    public static Cipher createCipher(SecretKey key, int opmode, byte[] nonce) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        IvParameterSpec spec = new IvParameterSpec(nonce);
        Cipher cipher = Cipher.getInstance(CRYPTO_CIPHER_AEAD);
        cipher.init(opmode, key, spec);
        return cipher;
    }

    public static CryptResult encrypt(byte[] data, Cipher cipher) throws BadPaddingException, IllegalBlockSizeException {
        // split off the tag to store it separately
        byte[] result = cipher.doFinal(data);
        byte[] tag = Arrays.copyOfRange(result, result.length - CRYPTO_TAG_SIZE, result.length);
        byte[] encrypted = Arrays.copyOfRange(result, 0, result.length - CRYPTO_TAG_SIZE);

        return new CryptResult() {{
            Parameters = new CryptParameters() {{
                Nonce = cipher.getIV();
                Tag = tag;
            }};
            Data = encrypted;
        }};
    }

    public static CryptResult decrypt(byte[] encrypted, Cipher cipher, CryptParameters params) throws IOException, BadPaddingException, IllegalBlockSizeException {
        // append the tag to the ciphertext
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(encrypted);
        stream.write(params.Tag);

        encrypted = stream.toByteArray();
        byte[] decrypted = cipher.doFinal(encrypted);

        return new CryptResult() {{
            Parameters = params;
            Data = decrypted;
        }};
    }

    public static byte[] hashKey(SecretKey key) {
        MessageDigest hash;
        try {
            hash = MessageDigest.getInstance(CRYPTO_HASH);
        } catch (NoSuchAlgorithmException e) {
            throw new UndeclaredThrowableException(e);
        }

        byte[] bytes = key.getEncoded();
        hash.update(bytes);
        CryptoUtils.zero(bytes);
        return hash.digest();
    }

    public static SecretKey generateKey() throws NoSuchAlgorithmException {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(CRYPTO_KEY_SIZE * 8);
        return generator.generateKey();
    }

    public static byte[] generateSalt() {
        return generateRandomBytes(CRYPTO_KEY_SIZE);
    }

    public static byte[] generateNonce() {
        return generateRandomBytes(CRYPTO_NONCE_SIZE);
    }

    public static byte[] generateRandomBytes(int length) {
        SecureRandom random = new SecureRandom();
        byte[] data = new byte[length];
        random.nextBytes(data);
        return data;
    }

    public static void zero(char[] data) {
        Arrays.fill(data, '\0');
    }

    public static void zero(byte[] data) {
        Arrays.fill(data, (byte) 0);
    }

    private static byte[] toBytes(char[] chars) {
        CharBuffer charBuf = CharBuffer.wrap(chars);
        ByteBuffer byteBuf = Charset.forName("UTF-8").encode(charBuf);
        return byteBuf.array();
    }
}
