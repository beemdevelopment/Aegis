package com.beemdevelopment.aegis.db.slots;

import com.beemdevelopment.aegis.crypto.CryptParameters;

import java.util.UUID;

public class FingerprintSlot extends RawSlot {
    public FingerprintSlot() {
        super();
    }

    FingerprintSlot(UUID uuid, byte[] key, CryptParameters keyParams) {
        super(uuid, key, keyParams);
    }

    @Override
    public byte getType() {
        return TYPE_FINGERPRINT;
    }
}
