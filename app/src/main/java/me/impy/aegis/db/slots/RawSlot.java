package me.impy.aegis.db.slots;

import me.impy.aegis.crypto.CryptoUtils;
import me.impy.aegis.util.LittleByteBuffer;

public class RawSlot extends Slot {

    public RawSlot() {
        super();
    }

    @Override
    public byte[] serialize() {
        LittleByteBuffer buffer = LittleByteBuffer.allocate(getSize());
        buffer.put(getType());
        buffer.put(_id);
        buffer.put(_encryptedMasterKey);
        return buffer.array();
    }

    @Override
    public void deserialize(byte[] data) throws Exception {
        LittleByteBuffer buffer = LittleByteBuffer.wrap(data);
        if (buffer.get() != getType()) {
            throw new Exception("slot type mismatch");
        }
        _id = new byte[ID_SIZE];
        buffer.get(_id);
        _encryptedMasterKey = new byte[CryptoUtils.CRYPTO_KEY_SIZE];
        buffer.get(_encryptedMasterKey);
    }

    @Override
    public int getSize() {
        return 1 + ID_SIZE + CryptoUtils.CRYPTO_KEY_SIZE;
    }

    @Override
    public byte getType() {
        return TYPE_RAW;
    }
}
