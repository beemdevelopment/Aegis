package com.beemdevelopment.aegis.importers;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Xml;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.encoding.Base64;
import com.beemdevelopment.aegis.encoding.EncodingException;
import com.beemdevelopment.aegis.encoding.Hex;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.util.JsonUtils;
import com.beemdevelopment.aegis.util.PreferenceParser;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.io.SuFile;
import com.topjohnwu.superuser.io.SuFileInputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
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
import javax.crypto.spec.SecretKeySpec;

public class AuthyImporter extends DatabaseImporter {
    private static final String _subPath = "shared_prefs";
    private static final String _pkgName = "com.authy.authy";
    private static final String _authFilename = "com.authy.storage.tokens.authenticator";
    private static final String _authyFilename = "com.authy.storage.tokens.authy";

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
    protected SuFile getAppPath() throws PackageManager.NameNotFoundException {
        return getAppPath(_pkgName, _subPath);
    }

    @Override
    public State readFromApp(Shell shell) throws PackageManager.NameNotFoundException, DatabaseImporterException {
        SuFile path = getAppPath();
        path.setShell(shell);

        JSONArray array;
        JSONArray authyArray;
        try {
            SuFile file1 = new SuFile(path, String.format("%s.xml", _authFilename));
            file1.setShell(shell);
            SuFile file2 = new SuFile(path, String.format("%s.xml", _authyFilename));
            file2.setShell(shell);

            array = readFile(file1, String.format("%s.key", _authFilename));
            authyArray = readFile(file2, String.format("%s.key", _authyFilename));
        } catch (IOException | XmlPullParserException e) {
            throw new DatabaseImporterException(e);
        }

        try {
            for (int i = 0; i < authyArray.length(); i++) {
                array.put(authyArray.getJSONObject(i));
            }
        } catch (JSONException e) {
            throw new DatabaseImporterException(e);
        }

        return read(array);
    }

    @Override
    public State read(InputStream stream, boolean isInternal) throws DatabaseImporterException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(stream, null);
            parser.nextTag();

            JSONArray array = new JSONArray();
            for (PreferenceParser.XmlEntry entry : PreferenceParser.parse(parser)) {
                if (entry.Name.equals(String.format("%s.key", _authFilename))
                        || entry.Name.equals(String.format("%s.key", _authyFilename))) {
                    array = new JSONArray(entry.Value);
                    break;
                }
            }

            return read(array);
        } catch (XmlPullParserException | JSONException | IOException e) {
            throw new DatabaseImporterException(e);
        }
    }

    private State read(JSONArray array) throws DatabaseImporterException {
        try {
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (!obj.has("decryptedSecret") && !obj.has("secretSeed")) {
                    return new EncryptedState(array);
                }
            }
        } catch (JSONException e) {
            throw new DatabaseImporterException(e);
        }

        return new DecryptedState(array);
    }

    private JSONArray readFile(SuFile file, String key) throws IOException, XmlPullParserException {
        try (InputStream inStream = SuFileInputStream.open(file)) {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(inStream, null);
            parser.nextTag();

            for (PreferenceParser.XmlEntry entry : PreferenceParser.parse(parser)) {
                if (entry.Name.equals(key)) {
                    return new JSONArray(entry.Value);
                }
            }
        } catch (JSONException ignored) {

        }

        return new JSONArray();
    }

    public static class EncryptedState extends DatabaseImporter.State {
        private JSONArray _array;

        private EncryptedState(JSONArray array) {
            super(true);
            _array = array;
        }

        protected DecryptedState decrypt(char[] password) throws DatabaseImporterException {
            try {
                for (int i = 0; i < _array.length(); i++) {
                    JSONObject obj = _array.getJSONObject(i);
                    String secretString = JsonUtils.optString(obj, "encryptedSecret");
                    if (secretString == null) {
                        continue;
                    }

                    byte[] encryptedSecret = Base64.decode(secretString);
                    byte[] salt = obj.getString("salt").getBytes(StandardCharsets.UTF_8);
                    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                    KeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_SIZE);
                    SecretKey key = factory.generateSecret(spec);
                    key = new SecretKeySpec(key.getEncoded(), "AES");

                    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                    IvParameterSpec ivSpec = new IvParameterSpec(IV);
                    cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);

                    byte[] secret = cipher.doFinal(encryptedSecret);
                    obj.remove("encryptedSecret");
                    obj.remove("salt");
                    obj.put("decryptedSecret", new String(secret, StandardCharsets.UTF_8));
                }

                return new DecryptedState(_array);
            } catch (JSONException
                    | EncodingException
                    | NoSuchAlgorithmException
                    | InvalidKeySpecException
                    | InvalidAlgorithmParameterException
                    | InvalidKeyException
                    | NoSuchPaddingException
                    | BadPaddingException
                    | IllegalBlockSizeException e) {
                throw new DatabaseImporterException(e);
            }
        }

        @Override
        public void decrypt(Context context, DecryptListener listener) {
            Dialogs.showPasswordInputDialog(context, R.string.enter_password_authy_message, password -> {
                try {
                    DecryptedState state = decrypt(password);
                    listener.onStateDecrypted(state);
                } catch (DatabaseImporterException e) {
                    listener.onError(e);
                }
            }, dialog1 -> listener.onCanceled());
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
                authyEntryInfo.OriginalName = JsonUtils.optString(entry, "originalName");
                authyEntryInfo.OriginalIssuer = JsonUtils.optString(entry, "originalIssuer");
                authyEntryInfo.AccountType = JsonUtils.optString(entry, "accountType");
                authyEntryInfo.Name = entry.optString("name");

                boolean isAuthy = entry.has("secretSeed");
                sanitizeEntryInfo(authyEntryInfo, isAuthy);

                byte[] secret;
                if (isAuthy) {
                    secret = Hex.decode(entry.getString("secretSeed"));
                } else {
                    secret = Base32.decode(entry.getString("decryptedSecret"));
                }

                int digits = entry.getInt("digits");
                OtpInfo info = new TotpInfo(secret, OtpInfo.DEFAULT_ALGORITHM, digits, isAuthy ? 10 : TotpInfo.DEFAULT_PERIOD);
                return new VaultEntry(info, authyEntryInfo.Name, authyEntryInfo.Issuer);
            } catch (OtpInfoException | JSONException | EncodingException e) {
                throw new DatabaseImporterEntryException(e, entry.toString());
            }
        }

        private static void sanitizeEntryInfo(AuthyEntryInfo info, boolean isAuthy) {
            if (!isAuthy) {
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
            } else {
                info.Issuer = info.Name;
                info.Name = "";
            }

            if (info.Name.startsWith(": ")) {
                info.Name = info.Name.substring(2);
            }
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
