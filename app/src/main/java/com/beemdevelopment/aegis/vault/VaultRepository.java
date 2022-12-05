package com.beemdevelopment.aegis.vault;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.AtomicFile;

import com.beemdevelopment.aegis.otp.GoogleAuthInfo;
import com.beemdevelopment.aegis.util.IOUtils;
import com.google.zxing.WriterException;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.util.Collection;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

public class VaultRepository {
    public static final String FILENAME = "aegis.json";
    public static final String FILENAME_PREFIX_EXPORT = "aegis-export";
    public static final String FILENAME_PREFIX_EXPORT_PLAIN = "aegis-export-plain";
    public static final String FILENAME_PREFIX_EXPORT_URI = "aegis-export-uri";
    public static final String FILENAME_PREFIX_EXPORT_HTML = "aegis-export-html";

    @NonNull
    private final Vault _vault;

    @Nullable
    private VaultFileCredentials _creds;

    @NonNull
    private final Context _context;

    public VaultRepository(@NonNull Context context, @NonNull Vault vault, @Nullable VaultFileCredentials creds) {
        _context = context;
        _vault = vault;
        _creds = creds;
    }

    private static AtomicFile getAtomicFile(Context context) {
        return new AtomicFile(new File(context.getFilesDir(), FILENAME));
    }

    public static boolean fileExists(Context context) {
        File file = getAtomicFile(context).getBaseFile();
        return file.exists() && file.isFile();
    }

    public static void deleteFile(Context context) {
        getAtomicFile(context).delete();
    }

    public static VaultFile readVaultFile(Context context) throws VaultRepositoryException {
        AtomicFile file = getAtomicFile(context);

        try {
            byte[] fileBytes = file.readFully();
            return VaultFile.fromBytes(fileBytes);
        } catch (IOException | VaultFileException e) {
            throw new VaultRepositoryException(e);
        }
    }

    public static void writeToFile(Context context, InputStream inStream) throws IOException {
        AtomicFile file = VaultRepository.getAtomicFile(context);

        FileOutputStream outStream = null;
        try {
            outStream = file.startWrite();
            IOUtils.copy(inStream, outStream);
            file.finishWrite(outStream);
        } catch (IOException e) {
            if (outStream != null) {
                file.failWrite(outStream);
            }
            throw e;
        }
    }

    public static VaultRepository fromFile(Context context, VaultFile file, VaultFileCredentials creds) throws VaultRepositoryException {
        if (file.isEncrypted() && creds == null) {
            throw new IllegalArgumentException("The VaultFile is encrypted but the given VaultFileCredentials is null");
        }

        Vault vault;
        try {
            JSONObject obj;
            if (!file.isEncrypted()) {
                obj = file.getContent();
            } else {
                obj = file.getContent(creds);
            }

            vault = Vault.fromJson(obj);
        } catch (VaultException | VaultFileException e) {
            throw new VaultRepositoryException(e);
        }

        return new VaultRepository(context, vault, creds);
    }

    void save() throws VaultRepositoryException {
        try {
            JSONObject obj = _vault.toJson();

            VaultFile file = new VaultFile();
            if (isEncryptionEnabled()) {
                file.setContent(obj, _creds);
            } else {
                file.setContent(obj);
            }

            try {
                byte[] bytes = file.toBytes();
                writeToFile(_context, new ByteArrayInputStream(bytes));
            } catch (IOException e) {
                throw new VaultRepositoryException(e);
            }
        } catch (VaultFileException e) {
            throw new VaultRepositoryException(e);
        }
    }

    /**
     * Exports the vault by serializing it and writing it to the given OutputStream. If encryption
     * is enabled, the vault will be encrypted automatically.
     */
    public void export(OutputStream stream) throws VaultRepositoryException {
        export(stream, getCredentials());
    }

    /**
     * Exports the vault by serializing it and writing it to the given OutputStream. If creds is
     * not null, it will be used to encrypt the vault first.
     */
    public void export(OutputStream stream, @Nullable VaultFileCredentials creds) throws VaultRepositoryException {
        exportFiltered(stream, creds, null);
    }

    /**
     * Exports the vault by serializing it and writing it to the given OutputStream. If encryption
     * is enabled, the vault will be encrypted automatically. If filter is not null only specified
     * entries will be exported
     */
    public void exportFiltered(OutputStream stream, @Nullable Vault.EntryFilter filter) throws VaultRepositoryException {
        exportFiltered(stream, getCredentials(), filter);
    }

    /**
     * Exports the vault by serializing it and writing it to the given OutputStream. If creds is
     * not null, it will be used to encrypt the vault first. If filter is not null only specified
     * entries will be exported
     */
    public void exportFiltered(OutputStream stream, @Nullable VaultFileCredentials creds, @Nullable Vault.EntryFilter filter) throws VaultRepositoryException {
        if (creds != null) {
            creds = creds.exportable();
        }

        try {
            VaultFile vaultFile = new VaultFile();

            if (creds != null) {
                vaultFile.setContent(_vault.toJson(filter), creds);
            } else {
                vaultFile.setContent(_vault.toJson(filter));
            }

            byte[] bytes = vaultFile.toBytes();
            stream.write(bytes);
        } catch (IOException | VaultFileException e) {
            throw new VaultRepositoryException(e);
        }
    }

    /**
     * Exports the vault by serializing the list of entries to a newline-separated list of
     * Google Authenticator URI's and writing it to the given OutputStream.
     */
    public void exportGoogleUris(OutputStream outStream, @Nullable Vault.EntryFilter filter) throws VaultRepositoryException {
        try (PrintStream stream = new PrintStream(outStream, false, StandardCharsets.UTF_8.name())) {
            for (VaultEntry entry : getEntries()) {
                if (filter == null || filter.includeEntry(entry)) {
                    GoogleAuthInfo info = new GoogleAuthInfo(entry.getInfo(), entry.getName(), entry.getIssuer());
                    stream.println(info.getUri().toString());
                }
            }
        } catch (IOException e) {
            throw new VaultRepositoryException(e);
        }
    }

    /**
     * Exports the vault by serializing the list of entries to an HTML file containing the Issuer,
     * Username and QR Code and writing it to the given OutputStream.
     */
    public void exportHtml(OutputStream outStream, @Nullable Vault.EntryFilter filter) throws VaultRepositoryException {
        Collection<VaultEntry> entries = getEntries();
        if (filter != null) {
            entries = entries.stream()
                    .filter(filter::includeEntry)
                    .collect(Collectors.toList());
        }

        try (PrintStream ps = new PrintStream(outStream, false, StandardCharsets.UTF_8.name())) {
            VaultHtmlExporter.export(_context, ps, entries);
        } catch (WriterException | IOException e) {
            throw new VaultRepositoryException(e);
        }
    }

    public void addEntry(VaultEntry entry) {
        _vault.getEntries().add(entry);
    }

    public VaultEntry getEntryByUUID(UUID uuid) {
        return _vault.getEntries().getByUUID(uuid);
    }

    public VaultEntry removeEntry(VaultEntry entry) {
        return _vault.getEntries().remove(entry);
    }

    public void wipeEntries() {
        _vault.getEntries().wipe();
    }

    public VaultEntry replaceEntry(VaultEntry entry) {
        return _vault.getEntries().replace(entry);
    }

    public void swapEntries(VaultEntry entry1, VaultEntry entry2) {
        _vault.getEntries().swap(entry1, entry2);
    }

    public boolean isEntryDuplicate(VaultEntry entry) {
        return _vault.getEntries().has(entry);
    }

    public Collection<VaultEntry> getEntries() {
        return _vault.getEntries().getValues();
    }

    public TreeSet<String> getGroups() {
        TreeSet<String> groups = new TreeSet<>(Collator.getInstance());
        for (VaultEntry entry : getEntries()) {
            String group = entry.getGroup();
            if (group != null) {
                groups.add(group);
            }
        }
        return groups;
    }

    public VaultFileCredentials getCredentials() {
        return _creds == null ? null : _creds.clone();
    }

    public void setCredentials(VaultFileCredentials creds) {
        _creds = creds == null ? null : creds.clone();
    }

    public boolean isEncryptionEnabled() {
        return _creds != null;
    }

    public boolean isBackupPasswordSet() {
        if (!isEncryptionEnabled()) {
            return false;
        }

        return getCredentials().getSlots().findBackupPasswordSlots().size() > 0;
    }
}
