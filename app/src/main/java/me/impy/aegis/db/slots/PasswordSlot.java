package me.impy.aegis.db.slots;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKey;

import me.impy.aegis.crypto.CryptoUtils;
import me.impy.aegis.encoding.Hex;
import me.impy.aegis.encoding.HexException;

public class PasswordSlot extends RawSlot {
    private int _n;
    private int _r;
    private int _p;
    private byte[] _salt;

    public PasswordSlot() {
        super();
    }

    @Override
    public JSONObject serialize() throws SlotException {
        try {
            JSONObject obj = super.serialize();
            obj.put("n", _n);
            obj.put("r", _r);
            obj.put("p", _p);
            obj.put("salt", Hex.toString(_salt));
            return obj;
        } catch (JSONException e) {
            throw new SlotException(e);
        }
    }

    @Override
    public void deserialize(JSONObject obj) throws SlotException {
        try {
            super.deserialize(obj);
            _n = obj.getInt("n");
            _r = obj.getInt("r");
            _p = obj.getInt("p");
            _salt = Hex.toBytes(obj.getString("salt"));
        } catch (JSONException | HexException e) {
            throw new SlotException(e);
        }
    }

    public SecretKey deriveKey(char[] password, byte[] salt, int n, int r, int p) throws SlotException {
        try {
            SecretKey key = CryptoUtils.deriveKey(password, salt, n, r, p);
            _n = n;
            _r = r;
            _p = p;
            _salt = salt;
            return key;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new SlotException(e);
        }
    }

    public SecretKey deriveKey(char[] password) throws SlotException {
        try {
            return CryptoUtils.deriveKey(password, _salt, _n, _r, _p);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new SlotException(e);
        }
    }

    @Override
    public byte getType() {
        return TYPE_DERIVED;
    }
}
