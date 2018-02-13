package me.impy.aegis.db.slots;

public class FingerprintSlot extends RawSlot {

    public FingerprintSlot() {
        super();
    }

    @Override
    public byte getType() {
        return TYPE_FINGERPRINT;
    }
}
