package me.impy.aegis.crypto.slots;

public class FingerprintSlot extends RawSlot {
    @Override
    public byte getType() {
        return TYPE_FINGERPRINT;
    }
}
