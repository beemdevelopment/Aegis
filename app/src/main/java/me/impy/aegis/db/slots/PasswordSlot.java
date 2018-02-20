package me.impy.aegis.db.slots;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKey;

import me.impy.aegis.crypto.CryptoUtils;
import me.impy.aegis.encoding.Hex;

public class PasswordSlot extends RawSlot {
    private int _n;
    private int _r;
    private int _p;
    private byte[] _salt;

    public PasswordSlot() {
        super();
    }

    @Override
    public JSONObject serialize() throws JSONException {
        JSONObject obj = super.serialize();
        obj.put("n", _n);
        obj.put("r", _r);
        obj.put("p", _p);
        obj.put("salt", Hex.toString(_salt));
        return obj;
    }

    @Override
    public void deserialize(JSONObject obj) throws Exception {
        super.deserialize(obj);
        _n = obj.getInt("n");
        _r = obj.getInt("r");
        _p = obj.getInt("p");
        _salt = Hex.toBytes(obj.getString("salt"));
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
    public byte getType() {
        return TYPE_DERIVED;
    }
}
