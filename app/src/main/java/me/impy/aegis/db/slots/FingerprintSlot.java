package me.impy.aegis.db.slots;

import java.util.UUID;

import me.impy.aegis.crypto.CryptParameters;

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
