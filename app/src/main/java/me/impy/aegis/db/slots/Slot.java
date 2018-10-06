package me.impy.aegis.db.slots;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
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

import me.impy.aegis.crypto.CryptParameters;
import me.impy.aegis.crypto.CryptResult;
import me.impy.aegis.crypto.CryptoUtils;
import me.impy.aegis.crypto.MasterKey;
import me.impy.aegis.encoding.Hex;
import me.impy.aegis.encoding.HexException;

public abstract class Slot implements Serializable {
    public final static byte TYPE_RAW = 0x00;
    public final static byte TYPE_DERIVED = 0x01;
    public final static byte TYPE_FINGERPRINT = 0x02;

    protected UUID _uuid;
    protected byte[] _encryptedMasterKey;
    protected CryptParameters _encryptedMasterKeyParams;

    protected Slot() {
        _uuid = UUID.randomUUID();
    }

    // getKey decrypts the encrypted master key in this slot using the given cipher and returns it.
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

    // setKey encrypts the given master key using the given cipher and stores the result in this slot.
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
            JSONObject paramObj = _encryptedMasterKeyParams.toJson();
            obj.put("type", getType());
            obj.put("uuid", _uuid.toString());
            obj.put("key", Hex.encode(_encryptedMasterKey));
            obj.put("key_params", paramObj);
            return obj;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void deserialize(JSONObject obj) throws SlotException {
        try {
            if (obj.getInt("type") != getType()) {
                throw new SlotException("slot type mismatch");
            }

            // if there is no uuid, generate a new one
            if (!obj.has("uuid")) {
                _uuid = UUID.randomUUID();
            } else {
                _uuid = UUID.fromString(obj.getString("uuid"));
            }

            JSONObject paramObj = obj.getJSONObject("key_params");
            _encryptedMasterKey = Hex.decode(obj.getString("key"));
            _encryptedMasterKeyParams = CryptParameters.fromJson(paramObj);
        } catch (JSONException | HexException e) {
            throw new SlotException(e);
        }
    }

    public abstract byte getType();

    public UUID getUUID() {
        return _uuid;
    }
}
