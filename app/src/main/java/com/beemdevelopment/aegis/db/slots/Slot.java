package com.beemdevelopment.aegis.db.slots;

import com.beemdevelopment.aegis.crypto.CryptParameters;
import com.beemdevelopment.aegis.crypto.CryptResult;
import com.beemdevelopment.aegis.crypto.CryptoUtils;
import com.beemdevelopment.aegis.crypto.MasterKey;
import com.beemdevelopment.aegis.crypto.SCryptParameters;
import com.beemdevelopment.aegis.encoding.Hex;
import com.beemdevelopment.aegis.encoding.HexException;
import com.beemdevelopment.aegis.util.UUIDMap;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public abstract class Slot extends UUIDMap.Value {
    public final static byte TYPE_RAW = 0x00;
    public final static byte TYPE_DERIVED = 0x01;
    public final static byte TYPE_BIOMETRIC = 0x02;

    private byte[] _encryptedMasterKey;
    private CryptParameters _encryptedMasterKeyParams;

    protected Slot() {
        super();
    }

    protected Slot(UUID uuid, byte[] key, CryptParameters keyParams) {
        super(uuid);
        _encryptedMasterKey = key;
        _encryptedMasterKeyParams = keyParams;
    }

    /**
     * Decrypts the encrypted master key in this slot using the given cipher and returns it.
     * @throws SlotException if a generic crypto operation error occurred.
     * @throws SlotIntegrityException if an error occurred while verifying the integrity of the slot.
     */
    public MasterKey getKey(Cipher cipher) throws SlotException, SlotIntegrityException {
        try {
            CryptResult res = CryptoUtils.decrypt(_encryptedMasterKey, cipher, _encryptedMasterKeyParams);
            SecretKey key = new SecretKeySpec(res.getData(), CryptoUtils.CRYPTO_AEAD);
            return new MasterKey(key);
        } catch (BadPaddingException e) {
            throw new SlotIntegrityException(e);
        } catch (IOException | IllegalBlockSizeException e) {
            throw new SlotException(e);
        }
    }

    /**
     * Encrypts the given master key using the given cipher and stores the result in this slot.
     * @throws SlotException if a generic crypto operation error occurred.
     */
    public void setKey(MasterKey masterKey, Cipher cipher) throws SlotException {
        try {
            byte[] masterKeyBytes = masterKey.getBytes();
            CryptResult res = CryptoUtils.encrypt(masterKeyBytes, cipher);
            _encryptedMasterKey = res.getData();
            _encryptedMasterKeyParams = res.getParams();
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            throw new SlotException(e);
        }
    }

    public static Cipher createEncryptCipher(SecretKey key) throws SlotException {
        try {
            return CryptoUtils.createEncryptCipher(key);
        } catch (InvalidAlgorithmParameterException
                | NoSuchPaddingException
                | NoSuchAlgorithmException
                | InvalidKeyException e) {
            throw new SlotException(e);
        }
    }

    public Cipher createDecryptCipher(SecretKey key) throws SlotException {
        try {
            return CryptoUtils.createDecryptCipher(key, _encryptedMasterKeyParams.getNonce());
        } catch (InvalidAlgorithmParameterException
                | NoSuchAlgorithmException
                | InvalidKeyException
                | NoSuchPaddingException e) {
            throw new SlotException(e);
        }
    }

    public JSONObject toJson() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("type", getType());
            obj.put("uuid", getUUID().toString());
            obj.put("key", Hex.encode(_encryptedMasterKey));
            obj.put("key_params", _encryptedMasterKeyParams.toJson());
            return obj;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static Slot fromJson(JSONObject obj) throws SlotException {
        Slot slot;

        try {
            UUID uuid;
            if (!obj.has("uuid")) {
                uuid = UUID.randomUUID();
            } else {
                uuid = UUID.fromString(obj.getString("uuid"));
            }

            byte[] key = Hex.decode(obj.getString("key"));
            CryptParameters keyParams = CryptParameters.fromJson(obj.getJSONObject("key_params"));

            switch (obj.getInt("type")) {
                case Slot.TYPE_RAW:
                    slot = new RawSlot(uuid, key, keyParams);
                    break;
                case Slot.TYPE_DERIVED:
                    SCryptParameters scryptParams = new SCryptParameters(
                        obj.getInt("n"),
                        obj.getInt("r"),
                        obj.getInt("p"),
                        Hex.decode(obj.getString("salt"))
                    );
                    boolean repaired = obj.optBoolean("repaired", false);
                    slot = new PasswordSlot(uuid, key, keyParams, scryptParams, repaired);
                    break;
                case Slot.TYPE_BIOMETRIC:
                    slot = new BiometricSlot(uuid, key, keyParams);
                    break;
                default:
                    throw new SlotException("unrecognized slot type");
            }
        } catch (JSONException | HexException e) {
            throw new SlotException(e);
        }

        return slot;
    }

    public abstract byte getType();
}
