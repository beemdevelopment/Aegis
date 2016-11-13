package me.impy.aegis.db;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;

import me.impy.aegis.crypto.CryptParameters;
import me.impy.aegis.crypto.CryptoUtils;
import me.impy.aegis.crypto.DerivationParameters;

public class DatabaseFile {
    public static final byte SEC_LEVEL_NONE = 0x00;
    public static final byte SEC_LEVEL_DERIVED = 0x01;
    public static final byte SEC_LEVEL_KEYSTORE = 0x02;
    private static final byte bSectionEncryptionParameters = 0x00;
    private static final byte bSectionDerivationParameters = 0x01;
    private static final byte bSectionEnd = (byte) 0xFF;
    private static final byte bVersion = 1;
    private static final String dbFilename = "aegis.db";

    private final byte[] bHeader;
    private final Context context;

    private byte level;
    private byte[] content;
    private CryptParameters cryptParameters;
    private DerivationParameters derivationParameters;

    public DatabaseFile(Context ctx) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
        context = ctx;
        bHeader = "AEGIS".getBytes("US_ASCII");
    }

    public byte[] serialize() throws IOException {
        CryptParameters cryptParams = getCryptParameters();
        DerivationParameters derParams = getDerivationParameters();
        byte[] content = getContent();
        byte level = getLevel();

        // this is dumb, java doesn't provide an endianness-aware data stream
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(byteStream);
        stream.write(bHeader);
        stream.write(bVersion);
        stream.write(level);

        // we assume that all of the needed params for the security level are set
        // if that's not the case, a NullPointerException will be thrown.
        switch (level) {
            case SEC_LEVEL_DERIVED:
                ByteBuffer paramBuffer = newBuffer(/* iterations */ 8 + CryptoUtils.CRYPTO_SALT_SIZE);
                paramBuffer.putLong(derParams.IterationCount);
                paramBuffer.put(derParams.Salt);
                writeSection(stream, bSectionDerivationParameters, paramBuffer.array());
                // intentional fallthrough
            case SEC_LEVEL_KEYSTORE:
                paramBuffer = newBuffer(CryptoUtils.CRYPTO_NONCE_SIZE + CryptoUtils.CRYPTO_TAG_SIZE);
                paramBuffer.put(cryptParams.Nonce);
                paramBuffer.put(cryptParams.Tag);
                writeSection(stream, bSectionEncryptionParameters, paramBuffer.array());
                break;
        }

        writeSection(stream, bSectionEnd, null);
        stream.write(content);
        return byteStream.toByteArray();
    }

    public void deserialize(byte[] data) throws Exception {
        ByteBuffer buffer = newBuffer(data);

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

        byte level = buffer.get();
        if (level > SEC_LEVEL_KEYSTORE) {
            throw new Exception("Unsupported security level");
        }
        setLevel(level);

        CryptParameters cryptParams = null;
        DerivationParameters derParams = null;

        for (section s = readSection(buffer); s.ID != bSectionEnd; s = readSection(buffer)) {
            ByteBuffer sBuff = newBuffer(s.Data);
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
                case bSectionDerivationParameters:
                    assertLength(s.Data, /* iterations */ 8 + CryptoUtils.CRYPTO_SALT_SIZE);

                    long iterations = sBuff.getLong();
                    byte[] salt = new byte[CryptoUtils.CRYPTO_SALT_SIZE];
                    sBuff.get(salt);

                    derParams = new DerivationParameters() {{
                        IterationCount = iterations;
                        Salt = salt;
                    }};
                    break;
            }
        }

        if ((level == SEC_LEVEL_DERIVED && (cryptParams == null || derParams == null))
                || (level == SEC_LEVEL_KEYSTORE && cryptParams == null)) {
            throw new Exception("Security level parameters missing");
        }

        setCryptParameters(cryptParams);
        setDerivationParameters(derParams);

        byte[] content = new byte[buffer.remaining()];
        buffer.get(content);
        setContent(content);
    }

    public void save() throws IOException {
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

        DatabaseFile db = new DatabaseFile(context);
        db.deserialize(data);
        return db;
    }

    private static void writeSection(DataOutputStream stream, byte id, byte[] data) throws IOException {
        stream.write(id);

        ByteBuffer buffer = newBuffer(/* sizeof uint32_t */ 4);
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

    private static section readSection(ByteBuffer buffer) {
        section s = new section();
        s.ID = buffer.get();

        int len = buffer.getInt();
        s.Data = new byte[len];
        buffer.get(s.Data);

        return s;
    }

    private static ByteBuffer newBuffer(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer;
    }

    private static ByteBuffer newBuffer(int size) {
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer;
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

    public DerivationParameters getDerivationParameters() {
        return derivationParameters;
    }

    public void setDerivationParameters(DerivationParameters derivationParameters) {
        this.derivationParameters = derivationParameters;
    }

    public byte getLevel() {
        return level;
    }

    public void setLevel(byte level) {
        this.level = level;
    }

    private static class section {
        public byte ID;
        public byte[] Data;
    }
}
