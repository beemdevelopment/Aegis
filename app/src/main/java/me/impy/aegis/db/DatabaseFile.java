package me.impy.aegis.db;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import me.impy.aegis.crypto.CryptParameters;
import me.impy.aegis.crypto.CryptResult;
import me.impy.aegis.crypto.MasterKey;
import me.impy.aegis.crypto.MasterKeyException;
import me.impy.aegis.db.slots.SlotList;
import me.impy.aegis.db.slots.SlotListException;
import me.impy.aegis.encoding.Base64;
import me.impy.aegis.encoding.Base64Exception;
import me.impy.aegis.encoding.HexException;

public class DatabaseFile {
    public static final byte VERSION = 1;

    private Object _content;
    private CryptParameters _cryptParameters;
    private SlotList _slots;

    public byte[] serialize() {
        try {
            // don't write the crypt parameters and slots if the content is not encrypted
            boolean plain = _content instanceof JSONObject || !isEncrypted();
            JSONObject headerObj = new JSONObject();
            headerObj.put("slots", plain ? JSONObject.NULL : SlotList.serialize(_slots));
            headerObj.put("params", plain ? JSONObject.NULL : _cryptParameters.toJson());

            JSONObject obj = new JSONObject();
            obj.put("version", VERSION);
            obj.put("header", headerObj);
            obj.put("db", _content);

            String string = obj.toString(4);
            return string.getBytes("UTF-8");
        } catch (JSONException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public void deserialize(byte[] data) throws DatabaseFileException {
        try {
            JSONObject obj = new JSONObject(new String(data, "UTF-8"));
            JSONObject headerObj = obj.getJSONObject("header");
            if (obj.getInt("version") > VERSION) {
                throw new DatabaseFileException("unsupported version");
            }

            JSONArray slotObj = headerObj.optJSONArray("slots");
            if (slotObj != null) {
                _slots = SlotList.deserialize(slotObj);
            }

            JSONObject cryptObj = headerObj.optJSONObject("params");
            if (cryptObj != null) {
                _cryptParameters = CryptParameters.parseJson(cryptObj);
            }

            if (cryptObj == null || slotObj == null) {
                _content = obj.getJSONObject("db");
            } else {
                _content = obj.getString("db");
            }
        } catch (SlotListException | UnsupportedEncodingException | JSONException | HexException e) {
            throw new DatabaseFileException(e);
        }
    }

    public boolean isEncrypted() {
        return _slots != null;
    }

    public JSONObject getContent() {
        return (JSONObject) _content;
    }

    public JSONObject getContent(MasterKey key) throws DatabaseFileException {
        try {
            byte[] bytes = Base64.decode((String) _content);
            CryptResult result = key.decrypt(bytes, _cryptParameters);
            return new JSONObject(new String(result.Data, "UTF-8"));
        } catch (MasterKeyException | JSONException | UnsupportedEncodingException | Base64Exception e) {
            throw new DatabaseFileException(e);
        }
    }

    public void setContent(JSONObject dbObj) {
        _content = dbObj;
        _cryptParameters = null;
        _slots = null;
    }

    public void setContent(JSONObject dbObj, MasterKey key) throws DatabaseFileException {
        try {
            String string = dbObj.toString(4);
            byte[] dbBytes = string.getBytes("UTF-8");

            CryptResult result = key.encrypt(dbBytes);
            _content = Base64.encode(result.Data);
            _cryptParameters = result.Parameters;
        } catch (MasterKeyException | UnsupportedEncodingException | JSONException e) {
            throw new DatabaseFileException(e);
        }
    }

    public SlotList getSlots() {
        return _slots;
    }

    public void setSlots(SlotList slots) {
        _slots = slots;
    }
}
