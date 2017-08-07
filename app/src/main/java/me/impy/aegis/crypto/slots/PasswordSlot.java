package me.impy.aegis.crypto.slots;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKey;

import me.impy.aegis.crypto.CryptoUtils;
import me.impy.aegis.util.LittleByteBuffer;

public class PasswordSlot extends RawSlot {
    private int _n;
    private int _r;
    private int _p;
    private byte[] _salt;

    public PasswordSlot() {
        super();
    }

    @Override
    public byte[] serialize() {
        byte[] bytes = super.serialize();
        LittleByteBuffer buffer = LittleByteBuffer.wrap(bytes);
        buffer.position(super.getSize());
        buffer.putInt(_n);
        buffer.putInt(_r);
        buffer.putInt(_p);
        buffer.put(_salt);
        return buffer.array();
    }

    @Override
    public void deserialize(byte[] data) throws Exception {
        super.deserialize(data);
        LittleByteBuffer buffer = LittleByteBuffer.wrap(data);
        buffer.position(super.getSize());
        _n = buffer.getInt();
        _r = buffer.getInt();
        _p = buffer.getInt();
        _salt = new byte[CryptoUtils.CRYPTO_SALT_SIZE];
        buffer.get(_salt);
    }

    public SecretKey deriveKey(char[] password, byte[] salt, int n, int r, int p) throws InvalidKeySpecException, NoSuchAlgorithmException {
        SecretKey key = CryptoUtils.deriveKey(password, salt, n, r, p);
        _n = n;
        _r = r;
        _p = p;
        _salt = salt;
        return key;
    }

    public SecretKey deriveKey(char[] password) throws InvalidKeySpecException, NoSuchAlgorithmException {
        return CryptoUtils.deriveKey(password, _salt, _n, _r, _p);
    }

    @Override
    public int getSize() {
        return 1 + CryptoUtils.CRYPTO_KEY_SIZE + /* _n, _r, _p */ 4 + 4 + 4 + CryptoUtils.CRYPTO_SALT_SIZE;
    }

    @Override
    public byte getType() {
        return TYPE_DERIVED;
    }
}
