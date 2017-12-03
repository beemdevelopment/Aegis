package me.impy.aegis.util;

import java.io.ByteArrayInputStream;

public class ByteInputStream extends ByteArrayInputStream {
    public ByteInputStream(byte[] buf) {
        super(buf);
    }

    public byte[] getBytes() {
        return this.buf;
    }
}
