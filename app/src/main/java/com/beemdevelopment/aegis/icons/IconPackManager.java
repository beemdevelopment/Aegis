package com.beemdevelopment.aegis.icons;

import android.content.Context;

import androidx.annotation.Nullable;

import com.beemdevelopment.aegis.util.IOUtils;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;

import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class IconPackManager {
    private static final String _packDefFilename = "pack.json";

    private File _iconsBaseDir;
    private List<IconPack> _iconPacks;

    public IconPackManager(Context context) {
        _iconPacks = new ArrayList<>();
        _iconsBaseDir = new File(context.getFilesDir(), "icons");
        rescanIconPacks();
    }

    private IconPack getIconPackByUUID(UUID uuid) {
        List<IconPack> packs = _iconPacks.stream().filter(i -> i.getUUID().equals(uuid)).collect(Collectors.toList());
        if (packs.size() == 0) {
            return null;
        }

        return packs.get(0);
    }

    public boolean hasIconPack() {
        return _iconPacks.size() > 0;
    }

    public List<IconPack> getIconPacks() {
        return new ArrayList<>(_iconPacks);
    }

    public void removeIconPack(IconPack pack) throws IconPackException {
        try {
            File dir = getIconPackDir(pack);
            deleteDir(dir);
        } catch (IOException e) {
            throw new IconPackException(e);
        }

        _iconPacks.remove(pack);
    }

    public IconPack importPack(File inFile) throws IconPackException {
        try {
            // read and parse the icon pack definition file of the icon pack
            ZipFile zipFile = new ZipFile(inFile);
            FileHeader packHeader = zipFile.getFileHeader(_packDefFilename);
            if (packHeader == null) {
                throw new IOException("Unable to find pack.json in the root of the ZIP file");
            }
            IconPack pack;
            byte[] defBytes;
            try (ZipInputStream inStream = zipFile.getInputStream(packHeader)) {
                defBytes = IOUtils.readAll(inStream);
                pack = IconPack.fromBytes(defBytes);
            }

            // create a new directory to store the icon pack, based on the UUID and version
            File packDir = getIconPackDir(pack);
            if (!packDir.getCanonicalPath().startsWith(_iconsBaseDir.getCanonicalPath() + File.separator)) {
                throw new IOException("Attempted to write outside of the parent directory");
            }
            if (packDir.exists()) {
                throw new IconPackExistsException(pack);
            }
            IconPack existingPack = getIconPackByUUID(pack.getUUID());
            if (existingPack != null) {
                throw new IconPackExistsException(existingPack);
            }
            if (!packDir.exists() && !packDir.mkdirs()) {
                throw new IOException(String.format("Unable to create directories: %s", packDir.toString()));
            }

            // extract each of the defined icons to the icon pack directory
            for (IconPack.Icon icon : pack.getIcons()) {
                File destFile = new File(packDir, icon.getRelativeFilename());
                FileHeader iconHeader = zipFile.getFileHeader(icon.getRelativeFilename());
                if (iconHeader == null) {
                    throw new IOException(String.format("Unable to find %s relative to the root of the ZIP file", icon.getRelativeFilename()));
                }

                // create new directories for this file if needed
                File parent = destFile.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    throw new IOException(String.format("Unable to create directories: %s", packDir.toString()));
                }

                try (ZipInputStream inStream = zipFile.getInputStream(iconHeader);
                     FileOutputStream outStream = new FileOutputStream(destFile)) {
                    IOUtils.copy(inStream, outStream);
                }

                // after successful copy of the icon, store the new filename
                icon.setFile(destFile);
            }

            // write the icon pack definition file to the newly created directory
            try (FileOutputStream outStream = new FileOutputStream(new File(packDir, _packDefFilename))) {
                outStream.write(defBytes);
            }

            // after successful extraction of the icon pack, store the new directory
            pack.setDirectory(packDir);
            _iconPacks.add(pack);
            return pack;
        } catch (IOException | JSONException e) {
            throw new IconPackException(e);
        }
    }

    private void rescanIconPacks() {
        _iconPacks.clear();

        File[] dirs = _iconsBaseDir.listFiles();
        if (dirs == null) {
            return;
        }

        for (File dir : dirs) {
            if (!dir.isDirectory()) {
                 continue;
            }

            UUID uuid;
            try {
                uuid = UUID.fromString(dir.getName());
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                continue;
            }

            File versionDir = getLatestVersionDir(dir);
            if (versionDir != null) {
                IconPack pack;
                try (FileInputStream inStream = new FileInputStream(new File(versionDir, _packDefFilename))) {
                    byte[] bytes = IOUtils.readAll(inStream);
                    pack = IconPack.fromBytes(bytes);
                    pack.setDirectory(versionDir);
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                    continue;
                }

                for (IconPack.Icon icon : pack.getIcons()) {
                    icon.setFile(new File(versionDir, icon.getRelativeFilename()));
                }

                // do a sanity check on the UUID and version
                if (pack.getUUID().equals(uuid) && pack.getVersion() == Integer.parseInt(versionDir.getName())) {
                    _iconPacks.add(pack);
                }
            }
        }
    }

    private File getIconPackDir(IconPack pack) {
        return new File(_iconsBaseDir, pack.getUUID() + File.separator + pack.getVersion());
    }

    @Nullable
    private static File getLatestVersionDir(File packDir) {
        File[] dirs = packDir.listFiles();
        if (dirs == null) {
            return null;
        }

        int latestVersion = -1;
        for (File versionDir : dirs) {
            int version;
            try {
                version = Integer.parseInt(versionDir.getName());
            } catch (NumberFormatException ignored) {
                continue;
            }

            if (latestVersion == -1 || version > latestVersion) {
                latestVersion = version;
            }
        }

        if (latestVersion == -1) {
            return null;
        }

        return new File(packDir, Integer.toString(latestVersion));
    }

    private static void deleteDir(File dir) throws IOException {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteDir(child);
                }
            }
        }

        if (!dir.delete()) {
            throw new IOException(String.format("Unable to delete directory: %s", dir));
        }
    }
}
