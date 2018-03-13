package me.impy.aegis.db;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import me.impy.aegis.crypto.CryptParameters;
import me.impy.aegis.crypto.CryptResult;
import me.impy.aegis.crypto.MasterKey;
import me.impy.aegis.db.slots.SlotCollection;
import me.impy.aegis.encoding.Hex;

public class DatabaseFile {
    public static final byte VERSION = 1;

    private Object _content;
    private CryptParameters _cryptParameters;
    private SlotCollection _slots;

    public DatabaseFile() {
        _slots = new SlotCollection();
    }

    public byte[] serialize() throws JSONException, UnsupportedEncodingException {
        JSONObject cryptObj = null;
        if (_cryptParameters != null) {
            cryptObj = new JSONObject();
            cryptObj.put("nonce", Hex.toString(_cryptParameters.Nonce));
            cryptObj.put("tag", Hex.toString(_cryptParameters.Tag));
        }

        // don't write the crypt parameters if the content is not encrypted
        boolean plain = _content instanceof JSONObject || _slots.isEmpty() || cryptObj == null;
        JSONObject headerObj = new JSONObject();
        headerObj.put("slots", plain ? JSONObject.NULL : SlotCollection.serialize(_slots));
        headerObj.put("params", plain ? JSONObject.NULL : cryptObj);

        JSONObject obj = new JSONObject();
        obj.put("version", VERSION);
        obj.put("header", headerObj);
        obj.put("db", _content);

        String string = obj.toString(4);
        return string.getBytes("UTF-8");
    }

    public void deserialize(byte[] data) throws Exception {
        JSONObject obj = new JSONObject(new String(data, "UTF-8"));
        JSONObject headerObj = obj.getJSONObject("header");
        if (obj.getInt("version") > VERSION) {
            throw new Exception("unsupported version");
        }

        JSONObject slotObj = headerObj.optJSONObject("slots");
        if (slotObj != null) {
            _slots = SlotCollection.deserialize(slotObj);
        }

        JSONObject cryptObj = headerObj.optJSONObject("params");
        if (cryptObj != null) {
            _cryptParameters = new CryptParameters() {{
                Nonce = Hex.toBytes(cryptObj.getString("nonce"));
                Tag = Hex.toBytes(cryptObj.getString("tag"));
            }};
        }

        if (cryptObj == null || slotObj == null) {
            _content = obj.getJSONObject("db");
        } else {
            _content = obj.getString("db");
        }
    }

    public boolean isEncrypted() {
        return !_slots.isEmpty() && _cryptParameters != null;
    }

    public JSONObject getContent() {
        return (JSONObject) _content;
    }

    public JSONObject getContent(MasterKey key)
            throws NoSuchPaddingException, InvalidKeyException,
            NoSuchAlgorithmException, IllegalBlockSizeException,
            BadPaddingException, InvalidAlgorithmParameterException, IOException, JSONException {
        byte[] bytes = Base64.decode((String) _content, Base64.NO_WRAP);
        CryptResult result = key.decrypt(bytes, _cryptParameters);
        return new JSONObject(new String(result.Data, "UTF-8"));
    }

    public void setContent(JSONObject dbObj) {
        _content = dbObj;
        _cryptParameters = null;
    }

    public void setContent(JSONObject dbObj, MasterKey key)
            throws JSONException, UnsupportedEncodingException,
            NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException,
            IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        String string = dbObj.toString(4);
        byte[] dbBytes = string.getBytes("UTF-8");

        CryptResult result = key.encrypt(dbBytes);
        _content = new String(Base64.encode(result.Data, Base64.NO_WRAP), "UTF-8");
        _cryptParameters = result.Parameters;
    }

    public SlotCollection getSlots() {
        return _slots;
    }

    public void setSlots(SlotCollection slots) {
        _slots = slots;
    }
}
