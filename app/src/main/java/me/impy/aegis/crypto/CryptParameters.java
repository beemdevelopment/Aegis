package me.impy.aegis.crypto;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

import me.impy.aegis.encoding.Hex;
import me.impy.aegis.encoding.HexException;

public class CryptParameters implements Serializable {
    public byte[] Nonce;
    public byte[] Tag;

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();

        try {
            obj.put("nonce", Hex.encode(Nonce));
            obj.put("tag", Hex.encode(Tag));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return obj;
    }

    public static CryptParameters parseJson(JSONObject obj) throws JSONException, HexException {
        byte[] tag = Hex.decode(obj.getString("tag"));
        byte[] nonce = Hex.decode(obj.getString("nonce"));
        return new CryptParameters() {{
            Tag = tag;
            Nonce = nonce;
        }};
    }
}
