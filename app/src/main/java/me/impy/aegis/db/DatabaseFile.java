package me.impy.aegis.db;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;

import me.impy.aegis.crypto.CryptParameters;
import me.impy.aegis.crypto.slots.SlotCollection;
import me.impy.aegis.crypto.CryptoUtils;
import me.impy.aegis.util.LittleByteBuffer;

public class DatabaseFile {
    private static final byte bSectionEncryptionParameters = 0x00;
    private static final byte bSectionSlots = 0x01;
    private static final byte bSectionEnd = (byte) 0xFF;
    private static final byte bVersion = 1;
    private static final String dbFilename = "aegis.db";

    private final byte[] bHeader;

    private byte[] content;
    private CryptParameters cryptParameters;
    private SlotCollection slots;

    public DatabaseFile() {
        try {
            bHeader = "AEGIS".getBytes("US_ASCII");
        } catch (Exception e) {
            throw new UndeclaredThrowableException(e);
        }
        slots = new SlotCollection();
    }

    public byte[] serialize() throws IOException {
        CryptParameters cryptParams = getCryptParameters();
        byte[] content = getContent();

        // this is dumb, java doesn't provide an endianness-aware data stream
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(byteStream);
        stream.write(bHeader);
        stream.write(bVersion);

        if (cryptParams != null) {
            LittleByteBuffer paramBuffer = LittleByteBuffer.allocate(CryptoUtils.CRYPTO_NONCE_SIZE + CryptoUtils.CRYPTO_TAG_SIZE);
            paramBuffer.put(cryptParams.Nonce);
            paramBuffer.put(cryptParams.Tag);
            writeSection(stream, bSectionEncryptionParameters, paramBuffer.array());
        }

        if (slots != null) {
            byte[] bytes = SlotCollection.serialize(slots);
            writeSection(stream, bSectionSlots, bytes);
        }

        writeSection(stream, bSectionEnd, null);
        stream.write(content);
        return byteStream.toByteArray();
    }

    public void deserialize(byte[] data) throws Exception {
        LittleByteBuffer buffer = LittleByteBuffer.wrap(data);

        byte[] header = new byte[bHeader.length];
        buffer.get(header);
        if (!Arrays.equals(header, bHeader)) {
            throw new Exception("Bad header");
        }

        // TODO: support different version deserialization providers
        byte version = buffer.get();
        if (version != bVersion) {
            throw new Exception("Unsupported version");
        }

        CryptParameters cryptParams = null;
        SlotCollection slots = null;

        for (section s = readSection(buffer); s.ID != bSectionEnd; s = readSection(buffer)) {
            LittleByteBuffer sBuff = LittleByteBuffer.wrap(s.Data);
            switch (s.ID) {
                case bSectionEncryptionParameters:
                    assertLength(s.Data, CryptoUtils.CRYPTO_NONCE_SIZE + CryptoUtils.CRYPTO_TAG_SIZE);

                    byte[] nonce = new byte[CryptoUtils.CRYPTO_NONCE_SIZE];
                    byte[] tag = new byte[CryptoUtils.CRYPTO_TAG_SIZE];
                    sBuff.get(nonce);
                    sBuff.get(tag);

                    cryptParams = new CryptParameters() {{
                        Nonce = nonce;
                        Tag = tag;
                    }};
                    break;
                case bSectionSlots:
                    slots = SlotCollection.deserialize(s.Data);
                    break;
            }
        }

        setCryptParameters(cryptParams);
        setSlots(slots);

        byte[] content = new byte[buffer.remaining()];
        buffer.get(content);
        setContent(content);
    }

    public boolean isEncrypted() {
        return slots != null && cryptParameters != null;
    }

    public void save(Context context) throws IOException {
        byte[] data = serialize();

        FileOutputStream file = context.openFileOutput(dbFilename, Context.MODE_PRIVATE);
        file.write(data);
        file.close();
    }

    public static DatabaseFile load(Context context) throws Exception {
        FileInputStream file = context.openFileInput(dbFilename);
        byte[] data = new byte[(int) file.getChannel().size()];
        file.read(data);
        file.close();

        DatabaseFile db = new DatabaseFile();
        db.deserialize(data);
        return db;
    }

    private static void writeSection(DataOutputStream stream, byte id, byte[] data) throws IOException {
        stream.write(id);

        LittleByteBuffer buffer = LittleByteBuffer.allocate(/* sizeof uint32_t */ 4);
        if (data == null) {
            buffer.putInt(0);
        } else {
            buffer.putInt(data.length);
        }
        stream.write(buffer.array());

        if (data != null) {
            stream.write(data);
        }
    }

    private static section readSection(LittleByteBuffer buffer) {
        section s = new section();
        s.ID = buffer.get();

        int len = buffer.getInt();
        s.Data = new byte[len];
        buffer.get(s.Data);

        return s;
    }

    private static void assertLength(byte[] bytes, int length) throws Exception {
        if (bytes.length != length) {
            throw new Exception("Bad length");
        }
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public CryptParameters getCryptParameters() {
        return cryptParameters;
    }

    public void setCryptParameters(CryptParameters parameters) {
        this.cryptParameters = parameters;
    }

    public SlotCollection getSlots() {
        return slots;
    }

    public void setSlots(SlotCollection slots) {
        this.slots = slots;
    }

    private static class section {
        public byte ID;
        public byte[] Data;
    }
}
