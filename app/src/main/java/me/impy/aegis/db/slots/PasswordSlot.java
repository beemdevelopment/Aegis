package me.impy.aegis.db.slots;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

import javax.crypto.SecretKey;

import me.impy.aegis.crypto.CryptParameters;
import me.impy.aegis.crypto.CryptoUtils;
import me.impy.aegis.crypto.SCryptParameters;
import me.impy.aegis.encoding.Hex;
import me.impy.aegis.encoding.HexException;

public class PasswordSlot extends RawSlot {
    private SCryptParameters _params;

    public PasswordSlot() {
        super();
    }

    protected PasswordSlot(UUID uuid, byte[] key, CryptParameters keyParams, SCryptParameters scryptParams) {
        super(uuid, key, keyParams);
        _params = scryptParams;
    }

    @Override
    public JSONObject toJson() {
        try {
            JSONObject obj = super.toJson();
            obj.put("n", _params.getN());
            obj.put("r", _params.getR());
            obj.put("p", _params.getP());
            obj.put("salt", Hex.encode(_params.getSalt()));
            return obj;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public SecretKey deriveKey(char[] password, SCryptParameters params) {
        SecretKey key = CryptoUtils.deriveKey(password, params);
        _params = params;
        return key;
    }

    public SecretKey deriveKey(char[] password) {
        return CryptoUtils.deriveKey(password, _params);
    }

    @Override
    public byte getType() {
        return TYPE_DERIVED;
    }
}
