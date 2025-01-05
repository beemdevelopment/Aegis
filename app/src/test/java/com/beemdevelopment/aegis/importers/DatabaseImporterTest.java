package com.beemdevelopment.aegis.importers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.otp.HotpInfo;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.SteamInfo;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.beemdevelopment.aegis.util.UUIDMap;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.beemdevelopment.aegis.vectors.VaultEntries;
import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class DatabaseImporterTest {
    private List<VaultEntry> _vectors;

    /**
     * The procedure for adding new importer tests is as follows:
     * 1. Generate QR codes for each test vector:
     *     -> while read line; do (qrencode "$line" -o - | feh -); done < ./app/src/test/resources/com/beemdevelopment/aegis/importers/plain.txt
     * 2. Scan the QR codes with the app we want to test our import functionality of
     * 3. Create an export and add the file to the importers resource directory.
     * 4. Add a new test for it here.
     */

    @Before
    public void initVectors() {
        _vectors = VaultEntries.get();
    }

    @Test
    public void testImportPlainText() throws IOException, DatabaseImporterException, OtpInfoException {
        List<VaultEntry> entries = importPlain(GoogleAuthUriImporter.class, "plain.txt");
        checkImportedEntries(entries);
    }

    @Test
    public void testImportAegisPlain() throws IOException, DatabaseImporterException, OtpInfoException {
        List<VaultEntry> entries = importPlain(AegisImporter.class, "aegis_plain.json");
        checkImportedEntries(entries);
    }

    @Test
    public void testImportAegisEncrypted() throws IOException, DatabaseImporterException, OtpInfoException {
        List<VaultEntry> entries = importEncrypted(AegisImporter.class, "aegis_encrypted.json", encryptedState -> {
            final char[] password = "test".toCharArray();
            return ((AegisImporter.EncryptedState) encryptedState).decrypt(password);
        });

        checkImportedEntries(entries);
    }

    @Test
    public void testImportDuo() throws IOException, DatabaseImporterException, OtpInfoException {
        List<VaultEntry> entries = importPlain(DuoImporter.class, "duo.json");
        for (VaultEntry entry : entries) {
            VaultEntry entryVector = getEntryVectorBySecret(entry.getInfo().getSecret());
            entryVector.setIssuer("");
            if (entryVector.getInfo() instanceof HotpInfo) {
                ((HotpInfo) entry.getInfo()).setCounter(1);
            }
            checkImportedEntry(entryVector, entry);
        }
    }

    @Test
    public void testImportEnteAuth() throws IOException, DatabaseImporterException, OtpInfoException {
        List<VaultEntry> entries = importPlain(EnteAuthImporter.class, "ente_auth.txt");
        checkImportedEntries(entries);
    }

    @Test
    public void testImportWinAuth() throws IOException, DatabaseImporterException, OtpInfoException {
        List<VaultEntry> entries = importPlain(WinAuthImporter.class, "plain.txt");
        for (VaultEntry entry : entries) {
            VaultEntry entryVector = getEntryVectorBySecret(entry.getInfo().getSecret());
            entryVector.setIssuer(entryVector.getName());
            entryVector.setName("WinAuth");
            checkImportedEntry(entryVector, entry);
        }
    }

    @Test
    public void testImportAndOTP() throws IOException, DatabaseImporterException, OtpInfoException {
        List<VaultEntry> entries = importPlain(AndOtpImporter.class, "andotp_plain.json");
        checkImportedEntries(entries);
    }

    @Test
    public void testImportAndOTPEncrypted() throws IOException, DatabaseImporterException, OtpInfoException {
        List<VaultEntry> entries = importEncrypted(AndOtpImporter.class, "andotp_encrypted.bin", encryptedState -> {
            final char[] password = "test".toCharArray();
            return ((AndOtpImporter.EncryptedState) encryptedState).decryptNewFormat(password);
        });

        checkImportedEntries(entries);
    }

    @Test
    public void testImportAndOTPEncryptedOld() throws IOException, DatabaseImporterException, OtpInfoException {
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
    public void testImportStratumEncrypted() throws DatabaseImporterException, IOException, OtpInfoException {
        List<VaultEntry> entries = importEncrypted(StratumImporter.class, "stratum_encrypted.bin", state -> {
            char[] password = "test".toCharArray();
            return ((StratumImporter.EncryptedState) state).decrypt(password);
        });
        checkImportedEntries(entries);
    }

    @Test
    public void testImportStratumEncryptedLegacy() throws DatabaseImporterException, IOException, OtpInfoException {
        List<VaultEntry> entries = importEncrypted(StratumImporter.class, "stratum_encrypted_legacy.bin", state -> {
            char[] password = "test".toCharArray();
            return ((StratumImporter.LegacyEncryptedState) state).decrypt(password);
        });
        checkImportedEntries(entries);
    }

    @Test
    public void testImportStratumInternal() throws DatabaseImporterException, IOException, OtpInfoException {
        List<VaultEntry> entries = importPlain(StratumImporter.class, "stratum_internal.db", true);
        checkImportedEntries(entries);
    }

    @Test
    public void testImportStratumPlain() throws DatabaseImporterException, IOException, OtpInfoException {
        List<VaultEntry> entries = importPlain(StratumImporter.class, "stratum_plain.json");
        checkImportedEntries(entries);
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
    public void testImportBattleNetXml() throws DatabaseImporterException, IOException, OtpInfoException {
        List<VaultEntry> entries = importPlain(BattleNetImporter.class, "battle_net_authenticator.xml");

        for (VaultEntry entry : entries) {
            checkImportedEntry(entry);
        }
    }

    @Test
    public void testImportBitwardenJson() throws IOException, DatabaseImporterException, OtpInfoException {
        List<VaultEntry> entries = importPlain(BitwardenImporter.class, "bitwarden.json");
        checkImportedBitwardenEntries(entries);
    }

    @Test
    public void testImportBitwardenCsv() throws IOException, DatabaseImporterException, OtpInfoException {
        List<VaultEntry> entries = importPlain(BitwardenImporter.class, "bitwarden.csv");
        checkImportedBitwardenEntries(entries);
    }

    @Test
    public void testImportFreeOtpV1() throws IOException, DatabaseImporterException, OtpInfoException {
        List<VaultEntry> entries = importPlain(FreeOtpImporter.class, "freeotp.xml");
        checkImportedFreeOtpEntriesV1(entries);
    }

    @Test
    public void testImportFreeOtpV2Api23() throws IOException, DatabaseImporterException, OtpInfoException {
        List<VaultEntry> entries = importEncrypted(FreeOtpImporter.class, "freeotp_v2_api23.xml", encryptedState -> {
            final char[] password = "test".toCharArray();
            return ((FreeOtpImporter.EncryptedState) encryptedState).decrypt(password);
        });
        checkImportedEntries(entries);
    }

    @Test
    public void testImportFreeOtpV2Api25() throws IOException, DatabaseImporterException, OtpInfoException {
        List<VaultEntry> entries = importEncrypted(FreeOtpImporter.class, "freeotp_v2_api25.xml", encryptedState -> {
            final char[] password = "test".toCharArray();
            return ((FreeOtpImporter.EncryptedState) encryptedState).decrypt(password);
        });
        checkImportedEntries(entries);
    }

    @Test
    public void testImportFreeOtpV2Api27() throws IOException, DatabaseImporterException, OtpInfoException {
        List<VaultEntry> entries = importEncrypted(FreeOtpImporter.class, "freeotp_v2_api27.xml", encryptedState -> {
            final char[] password = "test".toCharArray();
            return ((FreeOtpImporter.EncryptedState) encryptedState).decrypt(password);
        });
        checkImportedEntries(entries);
    }

    @Test
    public void testImportFreeOtpV2Api34() throws IOException, DatabaseImporterException, OtpInfoException {
        List<VaultEntry> entries = importEncrypted(FreeOtpImporter.class, "freeotp_v2_api34.xml", encryptedState -> {
            final char[] password = "test".toCharArray();
            return ((FreeOtpImporter.EncryptedState) encryptedState).decrypt(password);
        });
        checkImportedEntries(entries);
    }

    @Test
    public void testImportFreeOtpPlus() throws IOException, DatabaseImporterException, OtpInfoException {
        List<VaultEntry> entries = importPlain(FreeOtpPlusImporter.class, "freeotp_plus.json");
        checkImportedFreeOtpEntriesV1(entries);
    }

    @Test
    public void testImportFreeOtpPlusInternal() throws IOException, DatabaseImporterException, OtpInfoException {
        List<VaultEntry> entries = importPlain(FreeOtpPlusImporter.class, "freeotp_plus_internal.xml", true);
        checkImportedFreeOtpEntriesV1(entries);
    }

    @Test
    public void testImportGoogleAuthenticator() throws IOException, DatabaseImporterException, OtpInfoException {
        List<VaultEntry> entries = importPlain(GoogleAuthImporter.class, "google_authenticator.sqlite");
        for (VaultEntry entry : entries) {
            // Google Authenticator doesn't support different hash algorithms, periods or digits, so fix those up here
            VaultEntry entryVector = getEntryVectorBySecret(entry.getInfo().getSecret());
            entryVector.getInfo().setDigits(OtpInfo.DEFAULT_DIGITS);
            if (entryVector.getInfo() instanceof TotpInfo) {
                ((TotpInfo) entryVector.getInfo()).setPeriod(TotpInfo.DEFAULT_PERIOD);
            }
            entryVector.getInfo().setAlgorithm(OtpInfo.DEFAULT_ALGORITHM);
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
    public void testImportSteam() throws IOException, DatabaseImporterException, OtpInfoException {
        List<VaultEntry> entries = importPlain(SteamImporter.class, "steam.json");
        for (VaultEntry entry : entries) {
            VaultEntry entryVector = getEntryVectorBySecret(entry.getInfo().getSecret());
            entryVector.setIssuer("Steam");
            checkImportedEntry(entryVector, entry);
        }
    }

    @Test
    public void testImportSteamOld() throws IOException, DatabaseImporterException, OtpInfoException {
        List<VaultEntry> entries = importPlain(SteamImporter.class, "steam_old.json");
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

    @Test
    public void testImportTwoFASAuthenticatorSchema1() throws DatabaseImporterException, IOException, OtpInfoException {
        List<VaultEntry> entries = importPlain(TwoFASImporter.class, "2fas_authenticator.json");
        for (VaultEntry entry : entries) {
            // 2FAS Authenticator schema v1 doesn't support HOTP, different hash algorithms, periods or digits, so fix those up here
            VaultEntry entryVector = getEntryVectorBySecret(entry.getInfo().getSecret());
            entryVector.setInfo(new TotpInfo(entryVector.getInfo().getSecret()));
            checkImportedEntry(entryVector, entry);
        }
    }

    @Test
    public void testImportTwoFASAuthenticatorSchema2Plain() throws DatabaseImporterException, IOException, OtpInfoException {
        List<VaultEntry> entries = importPlain(TwoFASImporter.class, "2fas_authenticator_plain.2fas");
        checkImportedTwoFASSchema2Entries(entries);
    }

    @Test
    public void testImportTwoFASAuthenticatorSchema2Encrypted() throws DatabaseImporterException, IOException, OtpInfoException {
        List<VaultEntry> entries = importEncrypted(TwoFASImporter.class, "2fas_authenticator_encrypted.2fas", encryptedState -> {
            final char[] password = "test".toCharArray();
            return ((TwoFASImporter.EncryptedState) encryptedState).decrypt(password);
        });
        checkImportedTwoFASSchema2Entries(entries);
    }

    @Test
    public void testImportTwoFASAuthenticatorSchema3Plain() throws DatabaseImporterException, IOException, OtpInfoException {
        List<VaultEntry> entries = importPlain(TwoFASImporter.class, "2fas_authenticator_plain_v3.2fas");
        checkImportedEntries(entries);
    }

    @Test
    public void testImportTwoFASAuthenticatorSchema3Encrypted() throws DatabaseImporterException, IOException, OtpInfoException {
        List<VaultEntry> entries = importEncrypted(TwoFASImporter.class, "2fas_authenticator_encrypted_v3.2fas", encryptedState -> {
            final char[] password = "test".toCharArray();
            return ((TwoFASImporter.EncryptedState) encryptedState).decrypt(password);
        });
        checkImportedEntries(entries);
    }

    @Test
    public void testImportTwoFASAuthenticatorSchema4Plain() throws DatabaseImporterException, IOException, OtpInfoException {
        List<VaultEntry> entries = importPlain(TwoFASImporter.class, "2fas_authenticator_plain_v4.2fas");
        checkImportedEntries(entries);
    }

    @Test
    public void testImportTwoFASAuthenticatorSchema4Encrypted() throws DatabaseImporterException, IOException, OtpInfoException {
        List<VaultEntry> entries = importEncrypted(TwoFASImporter.class, "2fas_authenticator_encrypted_v4.2fas", encryptedState -> {
            final char[] password = "test".toCharArray();
            return ((TwoFASImporter.EncryptedState) encryptedState).decrypt(password);
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

    private void checkImportedTwoFASSchema2Entries(List<VaultEntry> entries) throws OtpInfoException {
        for (VaultEntry entry : entries) {
            // 2FAS Authenticator doesn't support certain features, so fix those entries up here
            VaultEntry entryVector = getEntryVectorBySecret(entry.getInfo().getSecret());
            OtpInfo info = entryVector.getInfo();
            int period = TotpInfo.DEFAULT_PERIOD;
            if (info instanceof TotpInfo) {
                period = ((TotpInfo) info).getPeriod();
            }
            entryVector.setInfo(new TotpInfo(info.getSecret(), info.getAlgorithm(false), info.getDigits(), period));
            checkImportedEntry(entryVector, entry);
        }
    }

    private void checkImportedAuthyEntries(List<VaultEntry> entries) throws OtpInfoException {
        for (VaultEntry entry : entries) {
            // Authy doesn't support different hash algorithms or periods, so fix those up here
            VaultEntry entryVector = getEntryVectorBySecret(entry.getInfo().getSecret());
            entryVector.getInfo().setAlgorithm(OtpInfo.DEFAULT_ALGORITHM);
            ((TotpInfo) entry.getInfo()).setPeriod(((TotpInfo) entryVector.getInfo()).getPeriod());
            checkImportedEntry(entryVector, entry);
        }
    }

    private void checkImportedTotpAuthenticatorEntries(List<VaultEntry> entries) throws OtpInfoException {
        for (VaultEntry entry : entries) {
            // TOTP Authenticator doesn't support different hash algorithms, periods or digits, so fix those up here
            VaultEntry entryVector = getEntryVectorBySecret(entry.getInfo().getSecret());
            entryVector.getInfo().setDigits(OtpInfo.DEFAULT_DIGITS);
            ((TotpInfo) entryVector.getInfo()).setPeriod(TotpInfo.DEFAULT_PERIOD);
            entryVector.getInfo().setAlgorithm(OtpInfo.DEFAULT_ALGORITHM);
            entryVector.setName(entryVector.getName().toLowerCase());
            checkImportedEntry(entryVector, entry);
        }
    }

    private void checkImportedFreeOtpEntriesV1(List<VaultEntry> entries) throws OtpInfoException {
        for (VaultEntry entry : entries) {
            // for some reason, FreeOTP adds -1 to the counter
            VaultEntry entryVector = getEntryVectorBySecret(entry.getInfo().getSecret());
            if (entryVector.getInfo() instanceof HotpInfo) {
                ((HotpInfo) entryVector.getInfo()).setCounter(((HotpInfo) entryVector.getInfo()).getCounter() - 1);
            }
            checkImportedEntry(entryVector, entry);
        }
    }

    private void checkImportedBitwardenEntries(List<VaultEntry> entries) throws OtpInfoException {
        byte[] secret, vectorSecret;
        for (VaultEntry entry : entries) {
            if(entry.getInfo().getTypeId().equals(SteamInfo.ID)) {
                secret = entry.getInfo().getSecret();
                vectorSecret = getEntryVectorBySecret(secret).getInfo().getSecret();
                assertNotNull(String.format("Steam secret has not been found (%s)", vectorSecret));
            } else {
                checkImportedEntry(entry);
            }
        }
    }

    private void checkImportedEntries(List<VaultEntry> entries) throws OtpInfoException {
        for (VaultEntry entry : entries) {
            checkImportedEntry(entry);
        }
    }

    private void checkImportedEntry(VaultEntry entry) throws OtpInfoException {
        VaultEntry entryVector = getEntryVectorBySecret(entry.getInfo().getSecret());
        checkImportedEntry(entryVector, entry);
    }

    private void checkImportedEntry(VaultEntry entryVector, VaultEntry entry) throws OtpInfoException {
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
