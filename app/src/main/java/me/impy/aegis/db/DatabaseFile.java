package me.impy.aegis.db;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
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
    private static final byte SECTION_ENCRYPTION_PARAMETERS = 0x00;
    private static final byte SECTION_SLOTS = 0x01;
    private static final byte SECTION_END = (byte) 0xFF;
    private static final byte VERSION = 1;

    private final byte[] HEADER;

    private byte[] _content;
    private CryptParameters _cryptParameters;
    private SlotCollection _slots;

    public DatabaseFile() {
        try {
            HEADER = "AEGIS".getBytes("US_ASCII");
        } catch (Exception e) {
            throw new UndeclaredThrowableException(e);
        }
        _slots = new SlotCollection();
    }

    public byte[] serialize() throws IOException {
        byte[] content = getContent();
        CryptParameters cryptParams = getCryptParameters();

        // this is dumb, java doesn't provide an endianness-aware data stream
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(byteStream);
        stream.write(HEADER);
        stream.write(VERSION);

        if (cryptParams != null) {
            LittleByteBuffer paramBuffer = LittleByteBuffer.allocate(CryptoUtils.CRYPTO_NONCE_SIZE + CryptoUtils.CRYPTO_TAG_SIZE);
            paramBuffer.put(cryptParams.Nonce);
            paramBuffer.put(cryptParams.Tag);
            writeSection(stream, SECTION_ENCRYPTION_PARAMETERS, paramBuffer.array());
        }

        if (!_slots.isEmpty()) {
            byte[] bytes = SlotCollection.serialize(_slots);
            writeSection(stream, SECTION_SLOTS, bytes);
        }

        writeSection(stream, SECTION_END, null);
        stream.write(content);
        return byteStream.toByteArray();
    }

    public void deserialize(byte[] data) throws Exception {
        LittleByteBuffer buffer = LittleByteBuffer.wrap(data);

        byte[] header = new byte[HEADER.length];
        buffer.get(header);
        if (!Arrays.equals(header, HEADER)) {
            throw new Exception("Bad header");
        }

        // TODO: support different version deserialization providers
        byte version = buffer.get();
        if (version != VERSION) {
            throw new Exception("Unsupported version");
        }

        CryptParameters cryptParams = null;
        SlotCollection slots = new SlotCollection();

        for (section s = readSection(buffer); s.ID != SECTION_END; s = readSection(buffer)) {
            LittleByteBuffer sBuff = LittleByteBuffer.wrap(s.Data);
            switch (s.ID) {
                case SECTION_ENCRYPTION_PARAMETERS:
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
                case SECTION_SLOTS:
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
        return !_slots.isEmpty() && _cryptParameters != null;
    }

    public void save(Context context, String filename) throws IOException {
        byte[] data = serialize();

        FileOutputStream file = context.openFileOutput(filename, Context.MODE_PRIVATE);
        file.write(data);
        file.close();
    }

    public static DatabaseFile load(Context context, String filename) throws Exception {
        byte[] bytes;
        FileInputStream file = null;

        try {
            file = context.openFileInput(filename);
            DataInputStream stream = new DataInputStream(file);
            bytes = new byte[(int) file.getChannel().size()];
            stream.readFully(bytes);
            stream.close();
        } finally {
            // always close the file
            // there is no need to close the DataInputStream
            if (file != null) {
                file.close();
            }
        }

        DatabaseFile db = new DatabaseFile();
        db.deserialize(bytes);
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
        return _content;
    }

    public void setContent(byte[] content) {
        _content = content;
    }

    public CryptParameters getCryptParameters() {
        return _cryptParameters;
    }

    public void setCryptParameters(CryptParameters parameters) {
        _cryptParameters = parameters;
    }

    public SlotCollection getSlots() {
        return _slots;
    }

    public void setSlots(SlotCollection slots) {
        _slots = slots;
    }

    private static class section {
        byte ID;
        byte[] Data;
    }
}
