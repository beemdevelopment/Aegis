package com.beemdevelopment.aegis.util;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtils {
    private IOUtils() {

    }

    public static byte[] readFile(FileInputStream inStream) throws IOException {
        try (DataInputStream outStream = new DataInputStream(inStream)) {
            byte[] fileBytes = new byte[(int) inStream.getChannel().size()];
            outStream.readFully(fileBytes);
            return fileBytes;
        }
    }

    public static byte[] readAll(InputStream inStream) throws IOException {
        try (ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
            copy(inStream, outStream);
            return outStream.toByteArray();
        }
    }

    public static void copy(InputStream inStream, OutputStream outStream) throws IOException {
        int read;
        byte[] buf = new byte[4096];
        while ((read = inStream.read(buf, 0, buf.length)) != -1) {
            outStream.write(buf, 0, read);
        }
    }

    public static void clearDirectory(File dir, boolean deleteRoot) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    clearDirectory(file, true);
                } else {
                    file.delete();
                }
            }
        }

        if (deleteRoot) {
            dir.delete();
        }
    }
}
