package com.beemdevelopment.aegis.vault.slots;

import com.beemdevelopment.aegis.crypto.CryptParameters;
import com.beemdevelopment.aegis.crypto.CryptoUtils;
import com.beemdevelopment.aegis.crypto.MasterKey;
import com.beemdevelopment.aegis.encoding.Hex;
import com.beemdevelopment.sodium.SCrypt;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

public class PasswordSlot extends RawSlot {
    private boolean _repaired;
    private SCrypt.Parameters _params;

    public PasswordSlot() {
        super();
    }

    protected PasswordSlot(UUID uuid, byte[] key, CryptParameters keyParams, SCrypt.Parameters scryptParams, boolean repaired) {
        super(uuid, key, keyParams);
        _params = scryptParams;
        _repaired = repaired;
    }

    @Override
    public JSONObject toJson() {
        try {
            JSONObject obj = super.toJson();
            SCrypt.CostParameters cost = _params.getCost();
            obj.put("n", cost.N);
            obj.put("r", cost.r);
            obj.put("p", cost.p);
            obj.put("salt", Hex.encode(_params.getSalt()));
            obj.put("repaired", _repaired);
            return obj;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public SecretKey deriveKey(char[] password, SCrypt.Parameters params) {
        SecretKey key = CryptoUtils.deriveKey(password, params);
        _params = params;
        return key;
    }

    public SecretKey deriveKey(char[] password) {
        return CryptoUtils.deriveKey(password, _params);
    }

    public SecretKey deriveKey(byte[] data) {
        return CryptoUtils.deriveKey(data, _params);
    }

    @Override
    public void setKey(MasterKey masterKey, Cipher cipher) throws SlotException {
        super.setKey(masterKey, cipher);
        _repaired = true;
    }

    /**
     * Reports whether this slot was repaired and is no longer affected by issue #95.
     */
    public boolean isRepaired() {
        return _repaired;
    }

    @Override
    public byte getType() {
        return TYPE_DERIVED;
    }
}
