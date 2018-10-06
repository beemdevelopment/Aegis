package me.impy.aegis.db;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import me.impy.aegis.crypto.CryptParameters;
import me.impy.aegis.crypto.CryptResult;
import me.impy.aegis.crypto.MasterKeyException;
import me.impy.aegis.db.slots.SlotList;
import me.impy.aegis.db.slots.SlotListException;
import me.impy.aegis.encoding.Base64;
import me.impy.aegis.encoding.Base64Exception;
import me.impy.aegis.encoding.HexException;

public class DatabaseFile {
    public static final byte VERSION = 1;

    private Object _content;
    private Header _header;

    public DatabaseFile() {

    }

    private DatabaseFile(Object content, Header header) {
        _content = content;
        _header = header;
    }

    public Header getHeader() {
        return _header;
    }

    public boolean isEncrypted() {
        return !_header.isEmpty();
    }

    public JSONObject toJson() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("version", VERSION);
            obj.put("header", _header.toJson());
            obj.put("db", _content);
            return obj;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] toBytes() {
        JSONObject obj = toJson();

        try {
            String string = obj.toString(4);
            return string.getBytes("UTF-8");
        } catch (JSONException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static DatabaseFile fromJson(JSONObject obj) throws DatabaseFileException {
        try {
            if (obj.getInt("version") > VERSION) {
                throw new DatabaseFileException("unsupported version");
            }

            Header header = Header.fromJson(obj.getJSONObject("header"));
            if (!header.isEmpty()) {
                return new DatabaseFile(obj.getString("db"), header);
            }

            return new DatabaseFile(obj.getJSONObject("db"), header);
        } catch (JSONException e) {
            throw new DatabaseFileException(e);
        }
    }

    public static DatabaseFile fromBytes(byte[] data) throws DatabaseFileException {
        try {
            JSONObject obj = new JSONObject(new String(data, "UTF-8"));
            return DatabaseFile.fromJson(obj);
        } catch (UnsupportedEncodingException | JSONException e) {
            throw new DatabaseFileException(e);
        }
    }

    public JSONObject getContent() {
        return (JSONObject) _content;
    }

    public JSONObject getContent(DatabaseFileCredentials creds) throws DatabaseFileException {
        try {
            byte[] bytes = Base64.decode((String) _content);
            CryptResult result = creds.decrypt(bytes, _header.getParams());
            return new JSONObject(new String(result.getData(), "UTF-8"));
        } catch (MasterKeyException | JSONException | UnsupportedEncodingException | Base64Exception e) {
            throw new DatabaseFileException(e);
        }
    }

    public void setContent(JSONObject obj) {
        _content = obj;
        _header = new Header(null, null);
    }

    public void setContent(JSONObject obj, DatabaseFileCredentials creds) throws DatabaseFileException {
        try {
            String string = obj.toString(4);
            byte[] dbBytes = string.getBytes("UTF-8");

            CryptResult result = creds.encrypt(dbBytes);
            _content = Base64.encode(result.getData());
            _header = new Header(creds.getSlots(), result.getParams());
        } catch (MasterKeyException | UnsupportedEncodingException | JSONException e) {
            throw new DatabaseFileException(e);
        }
    }

    public static class Header {
        private SlotList _slots;
        private CryptParameters _params;

        public Header(SlotList slots, CryptParameters params) {
            _slots = slots;
            _params = params;
        }

        public static Header fromJson(JSONObject obj) throws DatabaseFileException {
            if (obj.isNull("slots") && obj.isNull("params")) {
                return new Header(null, null);
            }

            try {
                SlotList slots = SlotList.fromJson(obj.getJSONArray("slots"));
                CryptParameters params = CryptParameters.fromJson(obj.getJSONObject("params"));
                return new Header(slots, params);
            } catch (SlotListException | JSONException | HexException e) {
                throw new DatabaseFileException(e);
            }
        }

        public JSONObject toJson() {
            try {
                JSONObject obj = new JSONObject();
                obj.put("slots", _slots != null ? _slots.toJson() : JSONObject.NULL);
                obj.put("params", _params != null ? _params.toJson() : JSONObject.NULL);
                return obj;
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        public SlotList getSlots() {
            return _slots;
        }

        public CryptParameters getParams() {
            return _params;
        }

        public boolean isEmpty() {
            return _slots == null && _params == null;
        }
    }
}
