package me.impy.aegis.db;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import me.impy.aegis.crypto.CryptParameters;
import me.impy.aegis.crypto.CryptResult;
import me.impy.aegis.crypto.MasterKey;
import me.impy.aegis.crypto.MasterKeyException;
import me.impy.aegis.db.slots.SlotCollection;
import me.impy.aegis.db.slots.SlotCollectionException;
import me.impy.aegis.encoding.Base64;
import me.impy.aegis.encoding.Base64Exception;
import me.impy.aegis.encoding.Hex;
import me.impy.aegis.encoding.HexException;

public class DatabaseFile {
    public static final byte VERSION = 1;

    private Object _content;
    private CryptParameters _cryptParameters;
    private SlotCollection _slots;

    public DatabaseFile() {
        _slots = new SlotCollection();
    }

    public byte[] serialize() throws DatabaseFileException {
        try {
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
        } catch (SlotCollectionException | UnsupportedEncodingException | JSONException e) {
            throw new DatabaseFileException(e);
        }
    }

    public void deserialize(byte[] data) throws DatabaseFileException {
        try {
            JSONObject obj = new JSONObject(new String(data, "UTF-8"));
            JSONObject headerObj = obj.getJSONObject("header");
            if (obj.getInt("version") > VERSION) {
                throw new DatabaseFileException("unsupported version");
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
        } catch (SlotCollectionException | UnsupportedEncodingException | JSONException | HexException e) {
            throw new DatabaseFileException(e);
        }
    }

    public boolean isEncrypted() {
        return !_slots.isEmpty() && _cryptParameters != null;
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

    public SlotCollection getSlots() {
        return _slots;
    }

    public void setSlots(SlotCollection slots) {
        _slots = slots;
    }
}
