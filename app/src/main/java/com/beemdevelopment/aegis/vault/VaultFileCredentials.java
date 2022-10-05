package com.beemdevelopment.aegis.vault;

import androidx.annotation.NonNull;

import com.beemdevelopment.aegis.crypto.CryptParameters;
import com.beemdevelopment.aegis.crypto.CryptResult;
import com.beemdevelopment.aegis.crypto.MasterKey;
import com.beemdevelopment.aegis.crypto.MasterKeyException;
import com.beemdevelopment.aegis.util.Cloner;
import com.beemdevelopment.aegis.vault.slots.SlotList;

import java.io.Serializable;

public class VaultFileCredentials implements Serializable {
    private MasterKey _key;
    private SlotList _slots;

    public VaultFileCredentials() {
        _key = MasterKey.generate();
        _slots = new SlotList();
    }

    public VaultFileCredentials(MasterKey key, SlotList slots) {
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

    /**
     * Returns a copy of these VaultFileCredentials that is suitable for exporting.
     * In case there's a backup password slot, any regular password slots are stripped.
     */
    public VaultFileCredentials exportable() {
        return new VaultFileCredentials(_key, _slots.exportable());
    }

    @NonNull
    @Override
    public VaultFileCredentials clone() {
        return Cloner.clone(this);
    }
}
