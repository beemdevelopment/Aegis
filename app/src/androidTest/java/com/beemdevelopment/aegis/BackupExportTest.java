package com.beemdevelopment.aegis;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.pressBack;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.isInternal;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.beemdevelopment.aegis.crypto.CryptoUtils;
import com.beemdevelopment.aegis.crypto.MasterKey;
import com.beemdevelopment.aegis.encoding.Hex;
import com.beemdevelopment.aegis.importers.DatabaseImporter;
import com.beemdevelopment.aegis.importers.DatabaseImporterException;
import com.beemdevelopment.aegis.importers.GoogleAuthUriImporter;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.rules.ScreenshotTestRule;
import com.beemdevelopment.aegis.ui.PreferencesActivity;
import com.beemdevelopment.aegis.util.IOUtils;
import com.beemdevelopment.aegis.vault.VaultBackupManager;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.beemdevelopment.aegis.vault.VaultFile;
import com.beemdevelopment.aegis.vault.VaultFileCredentials;
import com.beemdevelopment.aegis.vault.VaultFileException;
import com.beemdevelopment.aegis.vault.VaultRepository;
import com.beemdevelopment.aegis.vault.VaultRepositoryException;
import com.beemdevelopment.aegis.vault.slots.PasswordSlot;
import com.beemdevelopment.aegis.vault.slots.SlotException;
import com.beemdevelopment.aegis.vault.slots.SlotIntegrityException;
import com.beemdevelopment.aegis.vault.slots.SlotList;
import com.beemdevelopment.aegis.vectors.VaultEntries;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import dagger.hilt.android.testing.HiltAndroidTest;

@RunWith(AndroidJUnit4.class)
@HiltAndroidTest
@SmallTest
public class BackupExportTest extends AegisTest {
    private final ActivityScenarioRule<PreferencesActivity> _activityRule = new ActivityScenarioRule<>(PreferencesActivity.class);

    @Rule
    public final TestRule testRule = RuleChain.outerRule(_activityRule).around(new ScreenshotTestRule());

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void testPlainVaultExportPlainJson() {
        initPlainVault();

        openExportDialog();
        onView(withId(R.id.checkbox_export_encrypt)).perform(click());
        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(R.id.checkbox_accept)).perform(click());
        File file = doExport();

        readVault(file, null);
    }

    @Test
    public void testPlainVaultExportPlainTxt() {
        initPlainVault();

        openExportDialog();
        onView(withId(R.id.checkbox_export_encrypt)).perform(click());
        onView(withId(R.id.dropdown_export_format)).perform(click());
        onView(withText(R.string.export_format_google_auth_uri)).inRoot(RootMatchers.isPlatformPopup()).perform(click());
        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(R.id.checkbox_accept)).perform(click());
        File file = doExport();

        readTxtExport(file);
    }

    @Test
    public void testPlainVaultExportEncryptedJson() {
        initPlainVault();

        openExportDialog();
        File file = doExport();

        onView(withId(R.id.text_password)).perform(typeText(VAULT_PASSWORD), closeSoftKeyboard());
        onView(withId(R.id.text_password_confirm)).perform(typeText(VAULT_PASSWORD), closeSoftKeyboard());
        onView(withId(android.R.id.button1)).perform(click());

        readVault(file, VAULT_PASSWORD);
    }

    @Test
    public void testEncryptedVaultExportPlainJson() {
        initEncryptedVault();

        openExportDialog();
        onView(withId(R.id.checkbox_export_encrypt)).perform(click());
        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(R.id.checkbox_accept)).perform(click());
        File file = doExport();

        readVault(file, null);
    }

    @Test
    public void testEncryptedVaultExportPlainTxt() {
        initEncryptedVault();

        openExportDialog();
        onView(withId(R.id.checkbox_export_encrypt)).perform(click());
        onView(withId(R.id.dropdown_export_format)).perform(click());
        onView(withText(R.string.export_format_google_auth_uri)).inRoot(RootMatchers.isPlatformPopup()).perform(click());
        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(R.id.checkbox_accept)).perform(click());
        File file = doExport();

        readTxtExport(file);
    }

    @Test
    public void testEncryptedVaultExportEncryptedJson() {
        initEncryptedVault();

        openExportDialog();
        File file = doExport();

        readVault(file, VAULT_PASSWORD);
    }

    @Test
    public void testPlainVaultExportHtml() {
        initPlainVault();

        openExportDialog();
        onView(withId(R.id.checkbox_export_encrypt)).perform(click());
        onView(withId(R.id.dropdown_export_format)).perform(click());
        onView(withText(R.string.export_format_html)).inRoot(RootMatchers.isPlatformPopup()).perform(click());
        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(R.id.checkbox_accept)).perform(click());
        File file = doExport();

        checkHtmlExport(file);
    }

    @Test
    public void testEncryptedVaultExportHtml() {
        initEncryptedVault();

        openExportDialog();
        onView(withId(R.id.checkbox_export_encrypt)).perform(click());
        onView(withId(R.id.dropdown_export_format)).perform(click());
        onView(withText(R.string.export_format_html)).inRoot(RootMatchers.isPlatformPopup()).perform(click());
        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(R.id.checkbox_accept)).perform(click());
        File file = doExport();

        checkHtmlExport(file);
    }

    @Test
    public void testSeparateExportPassword() {
        initEncryptedVault();
        setSeparateBackupExportPassword();

        openExportDialog();
        File file = doExport();

        readVault(file, VAULT_BACKUP_PASSWORD);
    }

    @Test
    public void testChangeBackupPassword() throws SlotIntegrityException {
        initEncryptedVault();
        setSeparateBackupExportPassword();

        onView(withId(androidx.preference.R.id.recycler_view)).perform(RecyclerViewActions.actionOnItem(hasDescendant(withText(R.string.pref_section_security_title)), click()));
        onView(withId(androidx.preference.R.id.recycler_view)).perform(RecyclerViewActions.actionOnItem(hasDescendant(withText(R.string.pref_backup_password_change_title)), click()));
        onView(withId(R.id.text_password)).perform(typeText(VAULT_BACKUP_PASSWORD_CHANGED), closeSoftKeyboard());
        onView(withId(R.id.text_password_confirm)).perform(typeText(VAULT_BACKUP_PASSWORD_CHANGED), closeSoftKeyboard());
        onView(withId(android.R.id.button1)).perform(click());
        onView(isRoot()).perform(pressBack());

        VaultFileCredentials creds = _vaultManager.getVault().getCredentials();
        assertEquals(creds.getSlots().findRegularPasswordSlots().size(), 1);
        assertEquals(creds.getSlots().findBackupPasswordSlots().size(), 1);

        for (PasswordSlot slot : creds.getSlots().findBackupPasswordSlots()) {
            verifyPasswordSlotChange(creds, slot, VAULT_BACKUP_PASSWORD, VAULT_BACKUP_PASSWORD_CHANGED);
        }

        for (PasswordSlot slot : creds.getSlots().findRegularPasswordSlots()) {
            decryptPasswordSlot(slot, VAULT_PASSWORD);
        }

        openExportDialog();
        File file = doExport();
        readVault(file, VAULT_BACKUP_PASSWORD_CHANGED);
    }

    @Test
    public void testChangePasswordHavingBackupPassword() throws SlotIntegrityException {
        initEncryptedVault();
        setSeparateBackupExportPassword();

        onView(withId(androidx.preference.R.id.recycler_view)).perform(RecyclerViewActions.actionOnItem(hasDescendant(withText(R.string.pref_section_security_title)), click()));
        onView(withId(androidx.preference.R.id.recycler_view)).perform(RecyclerViewActions.actionOnItem(hasDescendant(withText(R.string.pref_set_password_title)), click()));
        onView(withId(R.id.text_password)).perform(typeText(VAULT_PASSWORD_CHANGED), closeSoftKeyboard());
        onView(withId(R.id.text_password_confirm)).perform(typeText(VAULT_PASSWORD_CHANGED), closeSoftKeyboard());
        onView(withId(android.R.id.button1)).perform(click());
        onView(isRoot()).perform(pressBack());

        VaultFileCredentials creds = _vaultManager.getVault().getCredentials();
        assertEquals(creds.getSlots().findRegularPasswordSlots().size(), 1);
        assertEquals(creds.getSlots().findBackupPasswordSlots().size(), 1);

        for (PasswordSlot slot : creds.getSlots().findRegularPasswordSlots()) {
            verifyPasswordSlotChange(creds, slot, VAULT_PASSWORD, VAULT_PASSWORD_CHANGED);
        }

        for (PasswordSlot slot : creds.getSlots().findBackupPasswordSlots()) {
            decryptPasswordSlot(slot, VAULT_BACKUP_PASSWORD);
        }

        openExportDialog();
        File file = doExport();
        readVault(file, VAULT_BACKUP_PASSWORD);
    }

    private void setSeparateBackupExportPassword() {
        VaultFileCredentials creds = _vaultManager.getVault().getCredentials();
        assertEquals(creds.getSlots().findRegularPasswordSlots().size(), 1);
        assertEquals(creds.getSlots().findBackupPasswordSlots().size(), 0);

        onView(withId(androidx.preference.R.id.recycler_view)).perform(RecyclerViewActions.actionOnItem(hasDescendant(withText(R.string.pref_section_security_title)), click()));
        onView(withId(androidx.preference.R.id.recycler_view)).perform(RecyclerViewActions.actionOnItem(hasDescendant(withText(R.string.pref_backup_password_title)), click()));
        onView(withId(R.id.text_password)).perform(typeText(VAULT_BACKUP_PASSWORD), closeSoftKeyboard());
        onView(withId(R.id.text_password_confirm)).perform(typeText(VAULT_BACKUP_PASSWORD), closeSoftKeyboard());
        onView(withId(android.R.id.button1)).perform(click());
        onView(isRoot()).perform(pressBack());

        creds = _vaultManager.getVault().getCredentials();
        assertEquals(creds.getSlots().findRegularPasswordSlots().size(), 1);
        assertEquals(creds.getSlots().findBackupPasswordSlots().size(), 1);
        for (PasswordSlot slot : creds.getSlots().findBackupPasswordSlots()) {
            verifyPasswordSlotChange(creds, slot, VAULT_PASSWORD, VAULT_BACKUP_PASSWORD);
        }
    }

    private void verifyPasswordSlotChange(VaultFileCredentials creds, PasswordSlot slot, String oldPassword, String newPassword) {
        assertThrows(SlotIntegrityException.class, () -> decryptPasswordSlot(slot, oldPassword));
        MasterKey masterKey;
        try {
            masterKey = decryptPasswordSlot(slot, newPassword);
        } catch (SlotIntegrityException e) {
            throw new RuntimeException("Unable to decrypt password slot", e);
        }

        assertArrayEquals(creds.getKey().getBytes(), masterKey.getBytes());
    }

    private File doExport() {
        File file = getExportFileUri();
        Intent resultData = new Intent();
        resultData.setData(Uri.fromFile(file));

        Instrumentation.ActivityResult result = new Instrumentation.ActivityResult(Activity.RESULT_OK, resultData);
        intending(not(isInternal())).respondWith(result);

        onView(withId(android.R.id.button1)).perform(click());
        return file;
    }

    private void openExportDialog() {
        onView(withId(androidx.preference.R.id.recycler_view)).perform(RecyclerViewActions.actionOnItem(hasDescendant(withText(R.string.pref_section_import_export_title)), click()));
        onView(withId(androidx.preference.R.id.recycler_view)).perform(RecyclerViewActions.actionOnItem(hasDescendant(withText(R.string.pref_export_title)), click()));
    }

    private MasterKey decryptPasswordSlot(PasswordSlot slot, String password) throws SlotIntegrityException {
        SecretKey derivedKey = slot.deriveKey(password.toCharArray());
        try {
            Cipher cipher = slot.createDecryptCipher(derivedKey);
            return slot.getKey(cipher);
        } catch (SlotException e) {
            throw new RuntimeException("Unable to decrypt password slot", e);
        }
    }

    private File getExportFileUri() {
        String dirName = Hex.encode(CryptoUtils.generateRandomBytes(8));
        File dir = new File(getInstrumentation().getTargetContext().getExternalCacheDir(), String.format("export-%s", dirName));
        if (!dir.mkdirs()) {
            throw new RuntimeException(String.format("Unable to create export directory: %s", dir));
        }

        VaultBackupManager.FileInfo fileInfo = new VaultBackupManager.FileInfo(VaultRepository.FILENAME_PREFIX_EXPORT);
        return new File(dir, fileInfo.toString());
    }

    private VaultRepository readVault(File file, @Nullable String password) {
        VaultRepository repo;
        try (InputStream inStream = new FileInputStream(file)) {
            byte[] bytes = IOUtils.readAll(inStream);
            VaultFile vaultFile = VaultFile.fromBytes(bytes);

            VaultFileCredentials creds = null;
            if (password != null) {
                SlotList slots = vaultFile.getHeader().getSlots();
                for (PasswordSlot slot : slots.findAll(PasswordSlot.class)) {
                    SecretKey derivedKey = slot.deriveKey(password.toCharArray());
                    Cipher cipher = slot.createDecryptCipher(derivedKey);
                    MasterKey masterKey = slot.getKey(cipher);
                    creds = new VaultFileCredentials(masterKey, slots);
                    break;
                }
            }

            repo = VaultRepository.fromFile(getInstrumentation().getContext(), vaultFile, creds);
        } catch (SlotException | SlotIntegrityException | VaultRepositoryException | VaultFileException | IOException e) {
            throw new RuntimeException("Unable to read back vault file", e);
        }

        checkReadEntries(repo.getEntries());
        return repo;
    }

    private void readTxtExport(File file) {
        GoogleAuthUriImporter importer = new GoogleAuthUriImporter(getInstrumentation().getContext());

        Collection<VaultEntry> entries;
        try (InputStream inStream = new FileInputStream(file)) {
            DatabaseImporter.State state = importer.read(inStream);
            DatabaseImporter.Result result = state.convert();
            entries = result.getEntries().getValues();
        } catch (DatabaseImporterException | IOException e) {
            throw new RuntimeException("Unable to read txt export file", e);
        }

        checkReadEntries(entries);
    }

    private void checkHtmlExport(File file) {
        try (InputStream inStream = new FileInputStream(file)) {
            Reader inReader = new InputStreamReader(inStream, StandardCharsets.UTF_8);
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(inReader);
            while (parser.getEventType() != XmlPullParser.START_TAG) {
                parser.next();
            }
            if (!parser.getName().toLowerCase(Locale.ROOT).equals("html")) {
                throw new RuntimeException("not an html document!");
            }
            while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
                parser.next();
            }
        } catch (IOException | XmlPullParserException e) {
            throw new RuntimeException("Unable to read html export file", e);
        }
    }

    private void checkReadEntries(Collection<VaultEntry> entries) {
        List<VaultEntry> vectors = VaultEntries.get();
        assertEquals(vectors.size(), entries.size());

        int i = 0;
        for (VaultEntry entry : entries) {
            VaultEntry vector = vectors.get(i);
            String message = String.format("Entries are not equivalent: (%s) (%s)", vector.toJson().toString(), entry.toJson().toString());
            assertTrue(message, vector.equivalates(entry));
            try {
                assertEquals(message, vector.getInfo().getOtp(), entry.getInfo().getOtp());
            } catch (OtpInfoException e) {
                throw new RuntimeException("Unable to generate OTP", e);
            }
            i++;
        }
    }
}
