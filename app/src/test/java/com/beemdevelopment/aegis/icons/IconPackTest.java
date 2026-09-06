package com.beemdevelopment.aegis.icons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@RunWith(RobolectricTestRunner.class)
public class IconPackTest {
    @Test
    public void testMaliciousIconPackCannotOverwriteVaultFile() throws IOException, JSONException {
        final String VAULT_CONTENTS = "VAULT-CONTENTS";
        final String MALICIOUS_FILENAME = "../../../aegis.json";

        Context context = ApplicationProvider.getApplicationContext();
        File vaultFile = new File(context.getFilesDir(), "aegis.json");
        Files.write(vaultFile.toPath(), VAULT_CONTENTS.getBytes(StandardCharsets.UTF_8));

        JSONObject iconJson = new JSONObject();
        iconJson.put("filename", MALICIOUS_FILENAME);
        iconJson.put("issuer", new JSONArray().put("Test"));

        JSONObject packJson = new JSONObject();
        packJson.put("uuid", "11111111-1111-1111-1111-111111111111");
        packJson.put("name", "Test Pack");
        packJson.put("version", 1);
        packJson.put("icons", new JSONArray().put(iconJson));

        IconPack iconPack = IconPack.fromJson(packJson);
        assertEquals(MALICIOUS_FILENAME, iconPack.getIcons().get(0).getRelativeFilename());

        File zipFile = File.createTempFile("iconpack", ".zip");
        zipFile.delete();
        try (ZipFile zip = new ZipFile(zipFile)) {
            addEntry(zip, "pack.json", packJson.toString());
            addEntry(zip, MALICIOUS_FILENAME, "MALICIOUS-VAULT-CONTENTS");
        }

        IconPackManager manager = new IconPackManager(context);
        IconPackException importError = null;
        try {
            manager.importPack(zipFile);
        } catch (IconPackException e) {
            importError = e;
        }

        String contentsAfter = new String(Files.readAllBytes(vaultFile.toPath()), StandardCharsets.UTF_8);
        assertEquals("Vault file must not be overwritten by icon pack import", VAULT_CONTENTS, contentsAfter);
        assertNotNull("Expected the malicious icon pack import to be rejected", importError);
    }

    private static void addEntry(ZipFile zip, String nameInZip, String content) throws ZipException {
        ZipParameters params = new ZipParameters();
        params.setFileNameInZip(nameInZip);
        zip.addStream(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), params);
    }
}
