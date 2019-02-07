package com.beemdevelopment.aegis.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ByteInputStream extends ByteArrayInputStream {
    private ByteInputStream(byte[] buf) {
        super(buf);
    }

    public static ByteInputStream create(InputStream fileStream) throws IOException {
        int read;
        byte[] buf = new byte[4096];
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        while ((read = fileStream.read(buf, 0, buf.length)) != -1) {
            outStream.write(buf, 0, read);
        }

        return new ByteInputStream(outStream.toByteArray());
    }

    public byte[] getBytes() {
        return this.buf;
    }
}
