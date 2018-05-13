package me.impy.aegis.importers;

import android.util.Xml;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.impy.aegis.crypto.KeyInfo;
import me.impy.aegis.crypto.KeyInfoException;
import me.impy.aegis.db.DatabaseEntry;
import me.impy.aegis.util.ByteInputStream;

public class FreeOTPImporter extends DatabaseImporter {
    public FreeOTPImporter(ByteInputStream stream) {
        super(stream);
    }

    private static class Entry {
        String Name;
        String Value;
    }

    @Override
    public void parse() throws DatabaseImporterException {

    }

    @Override
    public List<DatabaseEntry> convert() throws DatabaseImporterException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(_stream, null);
            parser.nextTag();
            return parse(parser);
        } catch (KeyInfoException | XmlPullParserException | JSONException | IOException e) {
            throw new DatabaseImporterException(e);
        }
    }

    @Override
    public boolean isEncrypted() {
        return false;
    }

    private static List<DatabaseEntry> parse(XmlPullParser parser)
            throws IOException, XmlPullParserException, JSONException, KeyInfoException {
        List<Entry> entries = new ArrayList<>();

        parser.require(XmlPullParser.START_TAG, null, "map");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            if (!parser.getName().equals("string")) {
                skip(parser);
                continue;
            }

            entries.add(parseEntry(parser));
        }

        List<DatabaseEntry> profiles = new ArrayList<>();

        for (Entry entry : entries) {
            if (entry.Name.equals("tokenOrder")) {
                // TODO: order
                JSONArray array = new JSONArray(entry.Value);
            } else {
                JSONObject obj = new JSONObject(entry.Value);

                KeyInfo key = new KeyInfo();
                key.setAlgorithm(obj.getString("algo"));
                key.setCounter(obj.getLong("counter"));
                key.setDigits(obj.getInt("digits"));
                key.setIssuer(obj.getString("issuerExt"));
                key.setAccountName(obj.optString("label"));
                key.setPeriod(obj.getInt("period"));
                key.setType(obj.getString("type"));
                byte[] secret = toBytes(obj.getJSONArray("secret"));
                key.setSecret(secret);

                DatabaseEntry profile = new DatabaseEntry(key);
                profiles.add(profile);
            }
        }

        return profiles;
    }

    private static byte[] toBytes(JSONArray array) throws JSONException {
        byte[] bytes = new byte[array.length()];
        for (int i = 0; i < array.length(); i++) {
            bytes[i] = (byte)array.getInt(i);
        }
        return bytes;
    }

    private static Entry parseEntry(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, "string");
        String name = parser.getAttributeValue(null, "name");
        String value = parseText(parser);
        parser.require(XmlPullParser.END_TAG, null, "string");
        return new Entry() {{ Name = name; Value = value; }};
    }

    private static String parseText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String text = "";
        if (parser.next() == XmlPullParser.TEXT) {
            text = parser.getText();
            parser.nextTag();
        }
        return text;
    }

    private static void skip(XmlPullParser parser) throws IOException, XmlPullParserException {
        // source: https://developer.android.com/training/basics/network-ops/xml.html
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }

        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
