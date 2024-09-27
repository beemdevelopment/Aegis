package com.beemdevelopment.aegis.importers;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Xml;

import androidx.lifecycle.Lifecycle;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.helpers.ContextHelper;
import com.beemdevelopment.aegis.otp.HotpInfo;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.SteamInfo;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.ui.tasks.PBKDFTask;
import com.beemdevelopment.aegis.util.PreferenceParser;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.topjohnwu.superuser.io.SuFile;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class FreeOtpImporter extends DatabaseImporter {
    private static final String _subPath = "shared_prefs/tokens.xml";
    private static final String _pkgName = "org.fedorahosted.freeotp";

    public FreeOtpImporter(Context context) {
        super(context);
    }

    @Override
    protected SuFile getAppPath() throws PackageManager.NameNotFoundException {
        return getAppPath(_pkgName, _subPath);
    }

    @Override
    public State read(InputStream stream, boolean isInternal) throws DatabaseImporterException {
        try (BufferedInputStream bufInStream = new BufferedInputStream(stream);
             DataInputStream dataInStream = new DataInputStream(bufInStream)) {

            dataInStream.mark(2);
            int magic = dataInStream.readUnsignedShort();
            dataInStream.reset();

            if (magic == SerializedHashMapParser.MAGIC) {
                return readV2(dataInStream);
            } else {
                return readV1(bufInStream);
            }
        } catch (IOException e) {
            throw new DatabaseImporterException(e);
        }
    }

    private DecryptedStateV1 readV1(InputStream stream) throws DatabaseImporterException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(stream, null);
            parser.nextTag();

            List<JSONObject> entries = new ArrayList<>();
            for (PreferenceParser.XmlEntry entry : PreferenceParser.parse(parser)) {
                if (!entry.Name.equals("tokenOrder")) {
                    entries.add(new JSONObject(entry.Value));
                }
            }
            return new DecryptedStateV1(entries);
        } catch (XmlPullParserException | IOException | JSONException e) {
            throw new DatabaseImporterException(e);
        }
    }

    private EncryptedState readV2(DataInputStream stream) throws DatabaseImporterException {
        try {
            Map<String, String> entries = SerializedHashMapParser.parse(stream);
            JSONObject mkObj = new JSONObject(entries.get("masterKey"));
            return new EncryptedState(mkObj, entries);
        } catch (IOException | JSONException | SerializedHashMapParser.ParseException e) {
            throw new DatabaseImporterException(e);
        }
    }

    public static class EncryptedState extends State {
        private static final int MASTER_KEY_SIZE = 32 * 8;

        private final String _mkAlgo;
        private final String _mkCipher;
        private final byte[] _mkCipherText;
        private final byte[] _mkParameters;
        private final byte[] _mkToken;
        private final byte[] _mkSalt;
        private final int _mkIterations;
        private final Map<String, String> _entries;

        private EncryptedState(JSONObject mkObj, Map<String, String> entries)
                throws DatabaseImporterException, JSONException {
            super(true);

            _mkAlgo = mkObj.getString("mAlgorithm");
            if (!_mkAlgo.equals("PBKDF2withHmacSHA1") && !_mkAlgo.equals("PBKDF2withHmacSHA512")) {
                throw new DatabaseImporterException(String.format("Unexpected master key KDF: %s", _mkAlgo));
            }
            JSONObject keyObj = mkObj.getJSONObject("mEncryptedKey");
            _mkCipher = keyObj.getString("mCipher");
            if (!_mkCipher.equals("AES/GCM/NoPadding")) {
                throw new DatabaseImporterException(String.format("Unexpected master key cipher: %s", _mkCipher));
            }
            _mkCipherText = toBytes(keyObj.getJSONArray("mCipherText"));
            _mkParameters = toBytes(keyObj.getJSONArray("mParameters"));
            _mkToken = keyObj.getString("mToken").getBytes(StandardCharsets.UTF_8);
            _mkSalt = toBytes(mkObj.getJSONArray("mSalt"));
            _mkIterations = mkObj.getInt("mIterations");
            _entries = entries;
        }

        public State decrypt(char[] password) throws DatabaseImporterException {
            PBKDFTask.Params params = new PBKDFTask.Params(_mkAlgo, MASTER_KEY_SIZE, password, _mkSalt, _mkIterations);
            SecretKey passKey = PBKDFTask.deriveKey(params);
            return decrypt(passKey);
        }

        public State decrypt(SecretKey passKey) throws DatabaseImporterException {
            byte[] masterKeyBytes;
            try {
                byte[] nonce = parseNonce(_mkParameters);
                IvParameterSpec spec = new IvParameterSpec(nonce);
                Cipher cipher = Cipher.getInstance(_mkCipher);
                cipher.init(Cipher.DECRYPT_MODE, passKey, spec);
                cipher.updateAAD(_mkToken);
                masterKeyBytes = cipher.doFinal(_mkCipherText);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException |
                     IllegalBlockSizeException | InvalidKeyException |
                     InvalidAlgorithmParameterException | IOException e) {
                throw new DatabaseImporterException(e);
            }

            SecretKey masterKey = new SecretKeySpec(masterKeyBytes, 0, masterKeyBytes.length, "AES");
            return new DecryptedStateV2(_entries, masterKey);
        }

        @Override
        public void decrypt(Context context, DecryptListener listener) {
            Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Aegis_AlertDialog_Warning)
                    .setTitle(R.string.importer_warning_title_freeotp2)
                    .setMessage(R.string.importer_warning_message_freeotp2)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        Dialogs.showPasswordInputDialog(context, R.string.enter_password_aegis_title, 0, password -> {
                            PBKDFTask.Params params = getKeyDerivationParams(password, _mkAlgo);
                            PBKDFTask task = new PBKDFTask(context, key -> {
                                try {
                                    State state = decrypt(key);
                                    listener.onStateDecrypted(state);
                                } catch (DatabaseImporterException e) {
                                    listener.onError(e);
                                }
                            });
                            Lifecycle lifecycle = ContextHelper.getLifecycle(context);
                            task.execute(lifecycle, params);
                        }, dialog1 -> listener.onCanceled());
                    })
                    .create());
        }

        private PBKDFTask.Params getKeyDerivationParams(char[] password, String algo) {
            return new PBKDFTask.Params(algo, MASTER_KEY_SIZE, password, _mkSalt, _mkIterations);
        }
    }

    public static class DecryptedStateV2 extends DatabaseImporter.State {
        private final Map<String, String> _entries;
        private final SecretKey _masterKey;

        public DecryptedStateV2(Map<String, String> entries, SecretKey masterKey) {
            super(false);
            _entries = entries;
            _masterKey = masterKey;
        }

        @Override
        public Result convert() throws DatabaseImporterException {
            Result result = new Result();

            for (Map.Entry<String, String> entry : _entries.entrySet()) {
                if (entry.getKey().endsWith("-token") || entry.getKey().equals("masterKey")) {
                    continue;
                }

                try {
                    JSONObject encObj = new JSONObject(entry.getValue());
                    String tokenKey = String.format("%s-token", entry.getKey());
                    JSONObject tokenObj = new JSONObject(_entries.get(tokenKey));

                    VaultEntry vaultEntry = convertEntry(encObj, tokenObj);
                    result.addEntry(vaultEntry);
                } catch (DatabaseImporterEntryException e) {
                    result.addError(e);
                } catch (JSONException ignored) {
                }
            }

            return result;
        }

        private VaultEntry convertEntry(JSONObject encObj, JSONObject tokenObj)
                throws DatabaseImporterEntryException {
            try {
                JSONObject keyObj = new JSONObject(encObj.getString("key"));
                String cipherName = keyObj.getString("mCipher");
                if (!cipherName.equals("AES/GCM/NoPadding")) {
                    throw new DatabaseImporterException(String.format("Unexpected cipher: %s", cipherName));
                }
                byte[] cipherText = toBytes(keyObj.getJSONArray("mCipherText"));
                byte[] parameters = toBytes(keyObj.getJSONArray("mParameters"));
                byte[] token = keyObj.getString("mToken").getBytes(StandardCharsets.UTF_8);

                byte[] nonce = parseNonce(parameters);
                IvParameterSpec spec = new IvParameterSpec(nonce);
                Cipher cipher = Cipher.getInstance(cipherName);
                cipher.init(Cipher.DECRYPT_MODE, _masterKey, spec);
                cipher.updateAAD(token);
                byte[] secretBytes = cipher.doFinal(cipherText);

                JSONArray secretArray = new JSONArray();
                for (byte b : secretBytes) {
                    secretArray.put(b);
                }
                tokenObj.put("secret", secretArray);

                return DecryptedStateV1.convertEntry(tokenObj);
            } catch (DatabaseImporterException | JSONException | NoSuchAlgorithmException |
                     NoSuchPaddingException | InvalidAlgorithmParameterException |
                     InvalidKeyException | BadPaddingException | IllegalBlockSizeException |
                     IOException e) {
                throw new DatabaseImporterEntryException(e, tokenObj.toString());
            }
        }
    }

    public static class DecryptedStateV1 extends DatabaseImporter.State {
        private final List<JSONObject> _entries;

        public DecryptedStateV1(List<JSONObject> entries) {
            super(false);
            _entries = entries;
        }

        @Override
        public Result convert() {
            Result result = new Result();

            for (JSONObject obj : _entries) {
                try {
                    VaultEntry entry = convertEntry(obj);
                    result.addEntry(entry);
                } catch (DatabaseImporterEntryException e) {
                    result.addError(e);
                }
            }

            return result;
        }

        private static VaultEntry convertEntry(JSONObject obj) throws DatabaseImporterEntryException {
            try {
                String type = obj.getString("type").toLowerCase(Locale.ROOT);
                String algo = obj.getString("algo");
                int digits = obj.getInt("digits");
                byte[] secret = toBytes(obj.getJSONArray("secret"));

                String issuer = obj.getString("issuerExt");
                String name = obj.optString("label");

                OtpInfo info;
                switch (type) {
                    case "totp":
                        int period = obj.getInt("period");
                        if (issuer.equals("Steam")) {
                            info = new SteamInfo(secret, algo, digits, period);
                        } else {
                            info = new TotpInfo(secret, algo, digits, period);
                        }
                        break;
                    case "hotp":
                        info = new HotpInfo(secret, algo, digits, obj.getLong("counter"));
                        break;
                    default:
                        throw new DatabaseImporterException("unsupported otp type: " + type);
                }

                return new VaultEntry(info, name, issuer);
            } catch (DatabaseImporterException | OtpInfoException | JSONException e) {
                throw new DatabaseImporterEntryException(e, obj.toString());
            }
        }
    }

    private static byte[] parseNonce(byte[] parameters) throws IOException {
        ASN1Primitive prim = ASN1Sequence.fromByteArray(parameters);
        if (prim instanceof ASN1OctetString) {
            return ((ASN1OctetString) prim).getOctets();
        }

        if (prim instanceof ASN1Sequence) {
            for (ASN1Encodable enc : (ASN1Sequence) prim) {
                if (enc instanceof ASN1OctetString) {
                    return ((ASN1OctetString) enc).getOctets();
                }
            }
        }

        throw new IOException("Unable to find nonce in parameters");
    }

    private static byte[] toBytes(JSONArray array) throws JSONException {
        byte[] bytes = new byte[array.length()];
        for (int i = 0; i < array.length(); i++) {
            bytes[i] = (byte)array.getInt(i);
        }
        return bytes;
    }
    private static class SerializedHashMapParser {
        private static final int MAGIC = 0xaced;
        private static final int VERSION = 5;
        private static final long SERIAL_VERSION_UID = 362498820763181265L;

        private static final byte TC_NULL = 0x70;
        private static final byte TC_CLASSDESC = 0x72;
        private static final byte TC_OBJECT = 0x73;
        private static final byte TC_STRING = 0x74;

        private SerializedHashMapParser() {

        }

        public static Map<String, String> parse(DataInputStream inStream)
                throws IOException, ParseException {
            Map<String, String> map = new HashMap<>();

            // Read/validate the magic number and version
            int magic = inStream.readUnsignedShort();
            int version = inStream.readUnsignedShort();
            if (magic != MAGIC || version != VERSION) {
                throw new ParseException("Not a serialized Java Object");
            }

            // Read the class descriptor info for HashMap
            byte b = inStream.readByte();
            if (b != TC_OBJECT) {
                throw new ParseException("Expected an object, found: " + b);
            }
            b = inStream.readByte();
            if (b != TC_CLASSDESC) {
                throw new ParseException("Expected a class desc, found: " + b);
            }
            parseClassDescriptor(inStream);

            // Not interested in the capacity of the map
            inStream.readInt();
            // Read the number of elements in the HashMap
            int size = inStream.readInt();

            // Parse each key-value pair in the map
            for (int i = 0; i < size; i++) {
                String key = parseStringObject(inStream);
                String value = parseStringObject(inStream);
                map.put(key, value);
            }

            return map;
        }

        private static void parseClassDescriptor(DataInputStream inputStream)
                throws IOException, ParseException {
            // Check whether we're dealing with a HashMap and a version we support
            String className = parseUTF(inputStream);
            if (!className.equals(HashMap.class.getName())) {
                throw new ParseException(String.format("Unexpected class name: %s", className));
            }
            long serialVersionUID = inputStream.readLong();
            if (serialVersionUID != SERIAL_VERSION_UID) {
                throw new ParseException(String.format("Unexpected serial version UID: %d", serialVersionUID));
            }

            // Read past all of the fields in the class
            byte fieldDescriptor = inputStream.readByte();
            if (fieldDescriptor == TC_NULL) {
                return;
            }
            int totalFieldSkip = 0;
            int fieldCount = inputStream.readUnsignedShort();
            for (int i = 0; i < fieldCount; i++) {
                char fieldType = (char) inputStream.readByte();
                parseUTF(inputStream);
                switch (fieldType) {
                    case 'F': // float (4 bytes)
                    case 'I': // int (4 bytes)
                        totalFieldSkip += 4;
                        break;
                    default:
                        throw new ParseException(String.format("Unexpected field type: %s", fieldType));
                }
            }
            inputStream.skipBytes(totalFieldSkip);

            // Not sure what these bytes are, just skip them
            inputStream.skipBytes(4);
        }

        private static String parseStringObject(DataInputStream inputStream)
                throws IOException, ParseException {
            byte objectType = inputStream.readByte();
            if (objectType != TC_STRING) {
                throw new ParseException(String.format("Expected a string object, found: %d", objectType));
            }

            int length = inputStream.readUnsignedShort();
            byte[] strBytes = new byte[length];
            inputStream.readFully(strBytes);

            return new String(strBytes, StandardCharsets.UTF_8);
        }

        private static String parseUTF(DataInputStream inputStream) throws IOException {
            int length = inputStream.readUnsignedShort();
            byte[] strBytes = new byte[length];
            inputStream.readFully(strBytes);
            return new String(strBytes, StandardCharsets.UTF_8);
        }

        private static class ParseException extends Exception {
            public ParseException(String message) {
                super(message);
            }
        }
    }
}
