package me.impy.aegis.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;

public class CryptoUtils {
    public static final byte CRYPTO_TAG_SIZE = 16;
    public static final byte CRYPTO_KEY_SIZE = 32;
    public static final byte CRYPTO_NONCE_SIZE = 12;
    public static final byte CRYPTO_SALT_SIZE = 32;
    // TODO: decide on a 'secure-enough' iteration count
    public static final short CRYPTO_ITERATION_COUNT = 10000;
    public static final String CRYPTO_CIPHER_RAW = "AES/ECB/NoPadding";
    public static final String CRYPTO_CIPHER_AEAD = "AES/GCM/NoPadding";
    // TODO: use a separate library for an HMAC-SHA256 implementation
    public static final String CRYPTO_DERIVE_ALGO = "PBKDF2WithHmacSHA1";

    public static SecretKey deriveKey(char[] password, byte[] salt, int iterations) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(CRYPTO_DERIVE_ALGO);
        KeySpec spec = new PBEKeySpec(password, salt, iterations, CRYPTO_KEY_SIZE * 8);
        return factory.generateSecret(spec);
    }

    public static Cipher createCipher(SecretKey key, int opmode) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        byte[] nonce = generateNonce();
        return createCipher(key, opmode, nonce);
    }

    public static Cipher createCipher(SecretKey key, int opmode, byte[] nonce) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        GCMParameterSpec spec = new GCMParameterSpec(CRYPTO_TAG_SIZE * 8, nonce);
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

    private static byte[] generateRandomBytes(int length) {
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
}
