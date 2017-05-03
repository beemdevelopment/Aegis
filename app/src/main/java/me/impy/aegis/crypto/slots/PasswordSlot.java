package me.impy.aegis.crypto.slots;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKey;

import me.impy.aegis.crypto.CryptoUtils;
import me.impy.aegis.util.LittleByteBuffer;

public class PasswordSlot extends RawSlot {
    private long _iterationCount;
    private byte[] _salt;

    public PasswordSlot() {
        super();
    }

    @Override
    public byte[] serialize() {
        byte[] bytes = super.serialize();
        LittleByteBuffer buffer = LittleByteBuffer.wrap(bytes);
        buffer.position(super.getSize());
        buffer.putLong(_iterationCount);
        buffer.put(_salt);
        return buffer.array();
    }

    @Override
    public void deserialize(byte[] data) throws Exception {
        super.deserialize(data);
        LittleByteBuffer buffer = LittleByteBuffer.wrap(data);
        buffer.position(super.getSize());
        _iterationCount = buffer.getLong();
        _salt = new byte[CryptoUtils.CRYPTO_SALT_SIZE];
        buffer.get(_salt);
    }

    public SecretKey deriveKey(char[] password, byte[] salt, int iterations) throws InvalidKeySpecException, NoSuchAlgorithmException {
        SecretKey key = CryptoUtils.deriveKey(password, salt, iterations);
        _iterationCount = iterations;
        _salt = salt;
        return key;
    }

    public SecretKey deriveKey(char[] password) throws InvalidKeySpecException, NoSuchAlgorithmException {
        SecretKey key = CryptoUtils.deriveKey(password, _salt, (int)_iterationCount);
        return key;
    }

    @Override
    public int getSize() {
        return 1 + CryptoUtils.CRYPTO_KEY_SIZE + /* iterations */ 8 + CryptoUtils.CRYPTO_SALT_SIZE;
    }

    @Override
    public byte getType() {
        return TYPE_DERIVED;
    }
}
