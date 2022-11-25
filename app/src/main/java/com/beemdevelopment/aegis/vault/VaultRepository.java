package com.beemdevelopment.aegis.vault;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.AtomicFile;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.helpers.QrCodeHelper;
import com.beemdevelopment.aegis.otp.GoogleAuthInfo;
import com.beemdevelopment.aegis.otp.HotpInfo;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.util.IOUtils;
import com.google.common.html.HtmlEscapers;
import com.google.zxing.WriterException;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.util.Collection;
import java.util.Objects;
import java.util.TreeSet;
import java.util.UUID;

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
        try {
            PrintStream printStream = new PrintStream(outStream, false, StandardCharsets.UTF_8.name());
            printStream.print("<html><head><title>");
            printStream.print(_context.getString(R.string.export_html_title));
            printStream.print("</title></head><body>");
            printStream.print("<h1>");
            printStream.print(_context.getString(R.string.export_html_title));
            printStream.print("</h1>");
            printStream.print("<table>");
            printStream.print("<tr>");
            printStream.print("<th>Issuer</th>");
            printStream.print("<th>Username</th>");
            printStream.print("<th>Type</th>");
            printStream.print("<th>QR Code</th>");
            printStream.print("<th>UUID</th>");
            printStream.print("<th>Note</th>");
            printStream.print("<th>Favorite</th>");
            printStream.print("<th>Algo</th>");
            printStream.print("<th>Digits</th>");
            printStream.print("<th>Secret</th>");
            printStream.print("<th>Counter</th>");
            printStream.print("</tr>");
            for (VaultEntry entry : getEntries()) {
                if (filter == null || filter.includeEntry(entry)) {
                    printStream.print("<tr>");
                    GoogleAuthInfo info = new GoogleAuthInfo(entry.getInfo(), entry.getName(), entry.getIssuer());
                    OtpInfo otpInfo = info.getOtpInfo();
                    printStream.print("<td>");
                    printStream.print(HtmlEscapers.htmlEscaper().escape(info.getIssuer()));
                    printStream.print("</td>");
                    printStream.print("<td>");
                    printStream.print(HtmlEscapers.htmlEscaper().escape(entry.getName()));
                    printStream.print("</td>");
                    printStream.print("<td>");
                    printStream.print(HtmlEscapers.htmlEscaper().escape(otpInfo.getType()));
                    printStream.print("</td>");
                    Bitmap bm = QrCodeHelper.encodeToBitmap(info.getUri().toString(),256, 256, Color.WHITE);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
                    byte[] b = baos.toByteArray();
                    String encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
                    printStream.print("<td class='qr'><img src=\"data:image/png;base64,");
                    printStream.print(encodedImage);
                    printStream.print("\"/></td>");
                    printStream.print("<td>");
                    printStream.print(HtmlEscapers.htmlEscaper().escape(entry.getUUID().toString()));
                    printStream.print("</td>");
                    printStream.print("<td>");
                    printStream.print(HtmlEscapers.htmlEscaper().escape(entry.getNote()));
                    printStream.print("</td>");
                    printStream.print("<td>");
                    printStream.print(HtmlEscapers.htmlEscaper().escape(entry.isFavorite() ? "true" : "false"));
                    printStream.print("</td>");
                    printStream.print("<td>");
                    printStream.print(HtmlEscapers.htmlEscaper().escape(otpInfo.getAlgorithm(false)));
                    printStream.print("</td>");
                    printStream.print("<td>");
                    printStream.print(HtmlEscapers.htmlEscaper().escape(Integer.toString(otpInfo.getDigits())));
                    printStream.print("</td>");
                    printStream.print("<td>");
                    printStream.print(HtmlEscapers.htmlEscaper().escape(Base32.encode(otpInfo.getSecret())));
                    printStream.print("</td>");
                    printStream.print("<td>");
                    if (Objects.equals(otpInfo.getTypeId(), HotpInfo.ID)) {
                        printStream.print(HtmlEscapers.htmlEscaper().escape(Long.toString(((HotpInfo) otpInfo).getCounter())));
                    } else {
                        printStream.print("-");
                    }
                    printStream.print("</td>");
                    printStream.print("</tr>");
                }
            };
            printStream.print("</table></body>");
            printStream.print("<style>table,td,th{border:1px solid #000;border-collapse:collapse;text-align:center}td:not(.qr),th{padding:1em}</style>");
            printStream.print("</html>");
            printStream.flush();
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
