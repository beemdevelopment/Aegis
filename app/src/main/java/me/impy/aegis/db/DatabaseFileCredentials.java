package me.impy.aegis.db;

import java.io.Serializable;

import me.impy.aegis.crypto.CryptParameters;
import me.impy.aegis.crypto.CryptResult;
import me.impy.aegis.crypto.MasterKey;
import me.impy.aegis.crypto.MasterKeyException;
import me.impy.aegis.db.slots.SlotList;

public class DatabaseFileCredentials implements Serializable {
    private MasterKey _key;
    private SlotList _slots;

    public DatabaseFileCredentials() {
        _key = MasterKey.generate();
        _slots = new SlotList();
    }

    public DatabaseFileCredentials(MasterKey key, SlotList slots) {
        _key = key;
        _slots = slots;
    }

    public CryptResult encrypt(byte[] bytes) throws MasterKeyException {
        return _key.encrypt(bytes);
    }

    public CryptResult decrypt(byte[] bytes, CryptParameters params) throws MasterKeyException {
        return _key.decrypt(bytes, params);
    }

    public MasterKey getKey() {
        return _key;
    }

    public SlotList getSlots() {
        return _slots;
    }
}
