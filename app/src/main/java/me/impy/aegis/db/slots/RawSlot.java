package me.impy.aegis.db.slots;

public class RawSlot extends Slot {

    public RawSlot() {
        super();
    }

    @Override
    public byte getType() {
        return TYPE_RAW;
    }
}
