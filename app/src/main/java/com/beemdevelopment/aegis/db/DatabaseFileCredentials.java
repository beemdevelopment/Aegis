package com.beemdevelopment.aegis.db;

import com.beemdevelopment.aegis.crypto.CryptParameters;
import com.beemdevelopment.aegis.crypto.CryptResult;
import com.beemdevelopment.aegis.crypto.MasterKey;
import com.beemdevelopment.aegis.crypto.MasterKeyException;
import com.beemdevelopment.aegis.db.slots.SlotList;

import java.io.Serializable;

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
