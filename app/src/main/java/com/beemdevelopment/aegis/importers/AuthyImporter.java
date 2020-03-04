package com.beemdevelopment.aegis.importers;

import android.content.Context;
import android.util.Xml;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.encoding.Base64;
import com.beemdevelopment.aegis.encoding.EncodingException;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.beemdevelopment.aegis.ui.Dialogs;
import com.beemdevelopment.aegis.util.PreferenceParser;
import com.beemdevelopment.aegis.vault.VaultEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;

public class AuthyImporter extends DatabaseImporter {
    private static final String _subPath = "shared_prefs/com.authy.storage.tokens.authenticator.xml";
    private static final String _pkgName = "com.authy.authy";

    private static final int ITERATIONS = 1000;
    private static final int KEY_SIZE = 256;
    private static final byte[] IV = new byte[]{
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };

    public AuthyImporter(Context context) {
        super(context);
    }

    @Override
    protected String getAppPkgName() {
        return _pkgName;
    }

    @Override
    protected String getAppSubPath() {
        return _subPath;
    }

    @Override
    public State read(FileReader reader) throws DatabaseImporterException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(reader.getStream(), null);
            parser.nextTag();

            JSONArray array = new JSONArray();
            for (PreferenceParser.XmlEntry entry : PreferenceParser.parse(parser)) {
                if (entry.Name.equals("com.authy.storage.tokens.authenticator.key")) {
                    array = new JSONArray(entry.Value);
                }
            }

            for (int i = 0; i < array.length(); i++) {
                if (!array.getJSONObject(i).has("decryptedSecret")) {
                    return new EncryptedState(array);
                }
            }

            return new DecryptedState(array);
        } catch (XmlPullParserException | JSONException | IOException e) {
            throw new DatabaseImporterException(e);
        }
    }

    public static class EncryptedState extends DatabaseImporter.State {
        private JSONArray _array;

        private EncryptedState(JSONArray array) {
            super(true);
            _array = array;
        }

        @Override
        public void decrypt(Context context, DecryptListener listener) {
            Dialogs.showPasswordInputDialog(context, R.string.enter_password_authy_message, password -> {
                try {
                    for (int i = 0; i < _array.length(); i++) {
                        JSONObject obj = _array.getJSONObject(i);
                        String secretString = obj.optString("encryptedSecret", null);
                        if (secretString == null) {
                            continue;
                        }

                        byte[] encryptedSecret = Base64.decode(secretString);
                        byte[] salt = obj.getString("salt").getBytes(StandardCharsets.UTF_8);
                        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                        KeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_SIZE);
                        SecretKey key = factory.generateSecret(spec);

                        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
                        IvParameterSpec ivSpec = new IvParameterSpec(IV);
                        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);

                        byte[] secret = cipher.doFinal(encryptedSecret);
                        obj.remove("encryptedSecret");
                        obj.remove("salt");
                        obj.put("decryptedSecret", new String(secret, StandardCharsets.UTF_8));
                    }

                    DecryptedState state = new DecryptedState(_array);
                    listener.onStateDecrypted(state);
                } catch (JSONException
                        | EncodingException
                        | NoSuchAlgorithmException
                        | InvalidKeySpecException
                        | InvalidAlgorithmParameterException
                        | InvalidKeyException
                        | NoSuchPaddingException
                        | BadPaddingException
                        | IllegalBlockSizeException e) {
                    listener.onError(e);
                }
            });
        }
    }

    public static class DecryptedState extends DatabaseImporter.State {
        private JSONArray _array;

        private DecryptedState(JSONArray array) {
            super(false);
            _array = array;
        }

        @Override
        public Result convert() throws DatabaseImporterException {
            Result result = new Result();

            try {
                for (int i = 0; i < _array.length(); i++) {
                    JSONObject entryObj = _array.getJSONObject(i);
                    try {
                        VaultEntry entry = convertEntry(entryObj);
                        result.addEntry(entry);
                    } catch (DatabaseImporterEntryException e) {
                        result.addError(e);
                    }
                }
            } catch (JSONException e) {
                throw new DatabaseImporterException(e);
            }

            return result;
        }

        private static VaultEntry convertEntry(JSONObject entry) throws DatabaseImporterEntryException {
            try {
                AuthyEntryInfo authyEntryInfo = new AuthyEntryInfo();
                authyEntryInfo.OriginalName = entry.optString("originalName", null);
                authyEntryInfo.OriginalIssuer = entry.optString("originalIssuer", null);
                authyEntryInfo.AccountType = entry.getString("accountType");
                authyEntryInfo.Name = entry.optString("name");

                sanitizeEntryInfo(authyEntryInfo);

                int digits = entry.getInt("digits");
                byte[] secret = Base32.decode(entry.getString("decryptedSecret"));

                OtpInfo info = new TotpInfo(secret, "SHA1", digits, 30);

                return new VaultEntry(info, authyEntryInfo.Name, authyEntryInfo.Issuer);
            } catch (OtpInfoException | JSONException | EncodingException e) {
                throw new DatabaseImporterEntryException(e, entry.toString());
            }
        }

        private static void sanitizeEntryInfo(AuthyEntryInfo info) {
            String separator = "";

            if (info.OriginalIssuer != null) {
                info.Issuer = info.OriginalIssuer;
            } else if (info.OriginalName != null && info.OriginalName.contains(":")) {
                info.Issuer = info.OriginalName.substring(0, info.OriginalName.indexOf(":"));
                separator = ":";
            } else if (info.Name.contains(" - ")) {
                info.Issuer = info.Name.substring(0, info.Name.indexOf(" - "));
                separator = " - ";
            } else {
                info.Issuer = info.AccountType.substring(0, 1).toUpperCase() + info.AccountType.substring(1);
            }

            info.Name = info.Name.replace(info.Issuer + separator, "");
        }
    }

    private static class AuthyEntryInfo {
        String OriginalName;
        String OriginalIssuer;
        String AccountType;
        String Issuer;
        String Name;
    }
}
