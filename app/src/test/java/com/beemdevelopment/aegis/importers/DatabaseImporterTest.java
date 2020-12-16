package com.beemdevelopment.aegis.importers;

import android.content.Context;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.encoding.EncodingException;
import com.beemdevelopment.aegis.otp.HotpInfo;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.SteamInfo;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.beemdevelopment.aegis.util.UUIDMap;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Config(sdk = { Build.VERSION_CODES.P })
@RunWith(RobolectricTestRunner.class)
public class DatabaseImporterTest {
    private List<VaultEntry> _vectors;

    /**
     * The procedure for adding new importer tests is as follows:
     * 1. Generate QR codes for each test vector:
     *     -> while read line; do (qrencode "$line" -o - | feh -); done < ./app/src/test/resources/com/beemdevelopment/aegis/importers/plain
     * 2. Scan the QR codes with the app we want to test our import functionality of
     * 3. Create an export and add the file to the importers resource directory.
     * 4. Add a new test for it here.
     */

    @Before
    public void initVectors() throws EncodingException, OtpInfoException {
        _vectors = Lists.newArrayList(
                new VaultEntry(new TotpInfo(Base32.decode("4SJHB4GSD43FZBAI7C2HLRJGPQ")), "Mason", "Deno"),
                new VaultEntry(new TotpInfo(Base32.decode("5OM4WOOGPLQEF6UGN3CPEOOLWU"), "SHA256", 7, 20), "James", "SPDX"),
                new VaultEntry(new TotpInfo(Base32.decode("7ELGJSGXNCCTV3O6LKJWYFV2RA"), "SHA512", 8, 50), "Elijah", "Airbnb"),
                new VaultEntry(new HotpInfo(Base32.decode("YOOMIXWS5GN6RTBPUFFWKTW5M4"), "SHA1", 6, 1), "James", "Issuu"),
                new VaultEntry(new HotpInfo(Base32.decode("KUVJJOM753IHTNDSZVCNKL7GII"), "SHA256", 7, 50), "Benjamin", "Air Canada"),
                new VaultEntry(new HotpInfo(Base32.decode("5VAML3X35THCEBVRLV24CGBKOY"), "SHA512", 8, 10300), "Mason", "WWE"),
                new VaultEntry(new SteamInfo(Base32.decode("JRZCL47CMXVOQMNPZR2F7J4RGI"), "SHA1", 5, 30), "Sophia", "Boeing")
        );
    }

    @Test
    public void testImportPlainText() throws IOException, DatabaseImporterException {
        List<VaultEntry> entries = importPlain(GoogleAuthUriImporter.class, "plain.txt");
        checkImportedEntries(entries);
    }

    @Test
    public void testImportAegisPlain() throws IOException, DatabaseImporterException {
        List<VaultEntry> entries = importPlain(AegisImporter.class, "aegis_plain.json");
        checkImportedEntries(entries);
    }

    @Test
    public void testImportAegisEncrypted() throws IOException, DatabaseImporterException {
        List<VaultEntry> entries = importEncrypted(AegisImporter.class, "aegis_encrypted.json", encryptedState -> {
            final char[] password = "test".toCharArray();
            return ((AegisImporter.EncryptedState) encryptedState).decrypt(password);
        });

        checkImportedEntries(entries);
    }

    @Test
    public void testImportWinAuth() throws IOException, DatabaseImporterException {
        List<VaultEntry> entries = importPlain(WinAuthImporter.class, "plain.txt");
        for (VaultEntry entry : entries) {
            VaultEntry entryVector = getEntryVectorBySecret(entry.getInfo().getSecret());
            entryVector.setIssuer(entryVector.getName());
            entryVector.setName("WinAuth");
            checkImportedEntry(entryVector, entry);
        }
    }

    @Test
    public void testImportAndOTP() throws IOException, DatabaseImporterException {
        List<VaultEntry> entries = importPlain(AndOtpImporter.class, "andotp_plain.json");
        checkImportedEntries(entries);
    }

    @Test
    public void testImportAndOTPEncrypted() throws IOException, DatabaseImporterException {
        List<VaultEntry> entries = importEncrypted(AndOtpImporter.class, "andotp_encrypted.bin", encryptedState -> {
            final char[] password = "test".toCharArray();
            return ((AndOtpImporter.EncryptedState) encryptedState).decryptNewFormat(password);
        });

        checkImportedEntries(entries);
    }

    @Test
    public void testImportAndOTPEncryptedOld() throws IOException, DatabaseImporterException {
        List<VaultEntry> entries = importEncrypted(AndOtpImporter.class, "andotp_encrypted_old.bin", encryptedState -> {
            final char[] password = "test".toCharArray();
            return ((AndOtpImporter.EncryptedState) encryptedState).decryptOldFormat(password);
        });

        for (VaultEntry entry : entries) {
            // old versions of andOTP have a bug where the issuer/name is not parsed correctly, so account for that here
            VaultEntry entryVector = getEntryVectorBySecret(entry.getInfo().getSecret());
            entryVector.setName(String.format("%s:%s", entryVector.getIssuer(), entryVector.getName()));
            checkImportedEntry(entryVector, entry);
        }
    }

    @Test
    public void testImportTotpAuthenticator() throws IOException, DatabaseImporterException, OtpInfoException {
        List<VaultEntry> entries = importEncrypted(TotpAuthenticatorImporter.class, "totp_authenticator.bin", encryptedState -> {
            final char[] password = "Testtest1".toCharArray();
            return ((TotpAuthenticatorImporter.EncryptedState) encryptedState).decrypt(password);
        });

        checkImportedTotpAuthenticatorEntries(entries);
    }

    @Test
    public void testImportTotpAuthenticatorInternal() throws IOException, DatabaseImporterException, OtpInfoException {
        List<VaultEntry> entries = importPlain(TotpAuthenticatorImporter.class, "totp_authenticator_internal.xml", true);
        checkImportedTotpAuthenticatorEntries(entries);
    }

    @Test
    public void testImportAuthy() throws IOException, DatabaseImporterException, OtpInfoException {
        List<VaultEntry> entries = importPlain(AuthyImporter.class, "authy_plain.xml");
        checkImportedAuthyEntries(entries);
    }

    @Test
    public void testImportAuthyEncrypted() throws IOException, DatabaseImporterException, OtpInfoException {
        List<VaultEntry> entries = importEncrypted(AuthyImporter.class, "authy_encrypted.xml", encryptedState -> {
            final char[] password = "testtest".toCharArray();
            return ((AuthyImporter.EncryptedState) encryptedState).decrypt(password);
        });

        checkImportedAuthyEntries(entries);
    }

    @Test
    public void testImportFreeOtp() throws IOException, DatabaseImporterException, OtpInfoException {
        List<VaultEntry> entries = importPlain(FreeOtpImporter.class, "freeotp.xml");
        checkImportedFreeOtpEntries(entries);
    }

    @Test
    public void testImportFreeOtpPlus() throws IOException, DatabaseImporterException, OtpInfoException {
        List<VaultEntry> entries = importPlain(FreeOtpPlusImporter.class, "freeotp_plus.json");
        checkImportedFreeOtpEntries(entries);
    }

    @Test
    public void testImportFreeOtpPlusInternal() throws IOException, DatabaseImporterException, OtpInfoException {
        List<VaultEntry> entries = importPlain(FreeOtpPlusImporter.class, "freeotp_plus_internal.xml", true);
        checkImportedFreeOtpEntries(entries);
    }

    @Test
    public void testImportGoogleAuthenticator() throws IOException, DatabaseImporterException, OtpInfoException {
        List<VaultEntry> entries = importPlain(GoogleAuthImporter.class, "google_authenticator.sqlite");
        for (VaultEntry entry : entries) {
            // Google Authenticator doesn't support different hash algorithms, periods or digits, so fix those up here
            VaultEntry entryVector = getEntryVectorBySecret(entry.getInfo().getSecret());
            entryVector.getInfo().setDigits(6);
            if (entryVector.getInfo() instanceof TotpInfo) {
                ((TotpInfo) entryVector.getInfo()).setPeriod(30);
            }
            entryVector.getInfo().setAlgorithm("SHA1");
            checkImportedEntry(entryVector, entry);
        }
    }

    @Test
    public void testImportMicrosoftAuthenticator() throws IOException, DatabaseImporterException, OtpInfoException {
        List<VaultEntry> entries = importPlain(MicrosoftAuthImporter.class, "microsoft_authenticator.sqlite");
        for (VaultEntry entry : entries) {
            // Microsoft Authenticator doesn't support HOTP, different hash algorithms, periods or digits, so fix those up here
            VaultEntry entryVector = getEntryVectorBySecret(entry.getInfo().getSecret());
            entryVector.setInfo(new TotpInfo(entryVector.getInfo().getSecret()));
            checkImportedEntry(entryVector, entry);
        }
    }

    @Test
    public void testImportSteam() throws IOException, DatabaseImporterException {
        List<VaultEntry> entries = importPlain(SteamImporter.class, "steam.json");
        for (VaultEntry entry : entries) {
            VaultEntry entryVector = getEntryVectorBySecret(entry.getInfo().getSecret());
            entryVector.setIssuer("Steam");
            checkImportedEntry(entryVector, entry);
        }
    }

    @Test
    public void testImportAuthenticatorPlus() throws IOException, DatabaseImporterException, OtpInfoException {
        List<VaultEntry> entries = importEncrypted(AuthenticatorPlusImporter.class, "authenticator_plus.zip", encryptedState -> {
            final char[] password = "testtesttest".toCharArray();
            return ((AuthenticatorPlusImporter.EncryptedState) encryptedState).decrypt(password);
        });

        checkImportedEntries(entries);
    }

    private List<VaultEntry> importPlain(Class<? extends DatabaseImporter> type, String resName)
            throws IOException, DatabaseImporterException {
        return importPlain(type, resName, false);
    }

    private List<VaultEntry> importPlain(Class<? extends DatabaseImporter> type, String resName, boolean isInternal)
            throws IOException, DatabaseImporterException {
        Context context = ApplicationProvider.getApplicationContext();
        DatabaseImporter importer = DatabaseImporter.create(context, type);
        try (InputStream stream = openResource(resName)) {
            DatabaseImporter.State state = importer.read(stream, isInternal);
            assertFalse(state.isEncrypted());
            DatabaseImporter.Result result = state.convert();
            return Lists.newArrayList(getEntries(result));
        }
    }

    private List<VaultEntry> importEncrypted(Class<? extends DatabaseImporter> type, String resName, Decryptor decryptor)
            throws IOException, DatabaseImporterException {
        return importEncrypted(type, resName, false, decryptor);
    }

    private List<VaultEntry> importEncrypted(Class<? extends DatabaseImporter> type, String resName, boolean isInternal, Decryptor decryptor)
            throws IOException, DatabaseImporterException {
        Context context = ApplicationProvider.getApplicationContext();
        DatabaseImporter importer = DatabaseImporter.create(context, type);
        try (InputStream stream = openResource(resName)) {
            DatabaseImporter.State state = importer.read(stream, isInternal);
            assertTrue(state.isEncrypted());
            DatabaseImporter.Result result = decryptor.decrypt(state).convert();
            return Lists.newArrayList(getEntries(result));
        }
    }

    private static UUIDMap<VaultEntry> getEntries(DatabaseImporter.Result result) {
        for (DatabaseImporterEntryException e : result.getErrors()) {
            fail(e.toString());
        }

        return result.getEntries();
    }

    private void checkImportedAuthyEntries(List<VaultEntry> entries) throws OtpInfoException {
        for (VaultEntry entry : entries) {
            // Authy doesn't support different hash algorithms or periods, so fix those up here
            VaultEntry entryVector = getEntryVectorBySecret(entry.getInfo().getSecret());
            entryVector.getInfo().setAlgorithm("SHA1");
            ((TotpInfo) entry.getInfo()).setPeriod(((TotpInfo) entryVector.getInfo()).getPeriod());
            checkImportedEntry(entryVector, entry);
        }
    }

    private void checkImportedTotpAuthenticatorEntries(List<VaultEntry> entries) throws OtpInfoException {
        for (VaultEntry entry : entries) {
            // TOTP Authenticator doesn't support different hash algorithms, periods or digits, so fix those up here
            VaultEntry entryVector = getEntryVectorBySecret(entry.getInfo().getSecret());
            entryVector.getInfo().setDigits(6);
            ((TotpInfo) entryVector.getInfo()).setPeriod(30);
            entryVector.getInfo().setAlgorithm("SHA1");
            entryVector.setName(entryVector.getName().toLowerCase());
            checkImportedEntry(entryVector, entry);
        }
    }

    private void checkImportedFreeOtpEntries(List<VaultEntry> entries) throws OtpInfoException {
        for (VaultEntry entry : entries) {
            // for some reason, FreeOTP adds -1 to the counter
            VaultEntry entryVector = getEntryVectorBySecret(entry.getInfo().getSecret());
            if (entryVector.getInfo() instanceof HotpInfo) {
                ((HotpInfo) entryVector.getInfo()).setCounter(((HotpInfo) entryVector.getInfo()).getCounter() - 1);
            }
            checkImportedEntry(entryVector, entry);
        }
    }

    private void checkImportedEntries(List<VaultEntry> entries) {
        for (VaultEntry entry : entries) {
            checkImportedEntry(entry);
        }
    }

    private void checkImportedEntry(VaultEntry entry) {
        VaultEntry entryVector = getEntryVectorBySecret(entry.getInfo().getSecret());
        checkImportedEntry(entryVector, entry);
    }

    private void checkImportedEntry(VaultEntry entryVector, VaultEntry entry) {
        String message = String.format("Entries are not equivalent: (%s) (%s)", entryVector.toJson().toString(), entry.toJson().toString());
        assertTrue(message, entryVector.equivalates(entry));
        assertEquals(message, entryVector.getInfo().getOtp(), entry.getInfo().getOtp());
    }

    private VaultEntry getEntryVectorBySecret(byte[] secret) {
        for (VaultEntry entry : _vectors) {
            if (Arrays.equals(entry.getInfo().getSecret(), secret)) {
                return entry;
            }
        }

        fail(String.format("No entry found for secret: %s", Base32.encode(secret)));
        return null;
    }

    private InputStream openResource(String name) {
        return getClass().getResourceAsStream(name);
    }

    private interface Decryptor {
        DatabaseImporter.State decrypt(DatabaseImporter.State encryptedState) throws DatabaseImporterException;
    }
}
