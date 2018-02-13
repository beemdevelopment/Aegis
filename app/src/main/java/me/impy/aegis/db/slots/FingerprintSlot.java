package me.impy.aegis.db.slots;

public class FingerprintSlot extends RawSlot {
    @Override
    public byte getType() {
        return TYPE_FINGERPRINT;
    }
}
