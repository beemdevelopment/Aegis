package me.impy.aegis.db.slots;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SlotList implements Iterable<Slot>, Serializable {
    private List<Slot> _slots = new ArrayList<>();

    public JSONArray toJson() {
        JSONArray array = new JSONArray();
        for (Slot slot : this) {
            array.put(slot.toJson());
        }

        return array;
    }

    public static SlotList fromJson(JSONArray array) throws SlotListException {
        SlotList slots = new SlotList();

        try {
            for (int i = 0; i < array.length(); i++) {
                Slot slot;
                JSONObject slotObj = array.getJSONObject(i);

                switch (slotObj.getInt("type")) {
                    case Slot.TYPE_RAW:
                        slot = new RawSlot();
                        break;
                    case Slot.TYPE_DERIVED:
                        slot = new PasswordSlot();
                        break;
                    case Slot.TYPE_FINGERPRINT:
                        slot = new FingerprintSlot();
                        break;
                    default:
                        throw new SlotException("unrecognized slot type");
                }

                slot.deserialize(slotObj);
                slots.add(slot);
            }
        } catch (SlotException | JSONException e) {
            throw new SlotListException(e);
        }

        return slots;
    }

    public void add(Slot slot) {
        for (Slot s : this) {
            if (s.getUUID().equals(slot.getUUID())) {
                throw new AssertionError("slot found with the same uuid");
            }
        }
        _slots.add(slot);
    }

    public void remove(Slot slot) {
        _slots.remove(slot);
    }

    public int size() {
        return _slots.size();
    }

    public <T extends Slot> T find(Class<T> type) {
        for (Slot slot : this) {
            if (slot.getClass() == type) {
                return type.cast(slot);
            }
        }
        return null;
    }

    public <T extends Slot> List<T> findAll(Class<T> type) {
        ArrayList<T> list = new ArrayList<>();
        for (Slot slot : this) {
            if (slot.getClass() == type) {
                list.add(type.cast(slot));
            }
        }
        return list;
    }

    public <T extends Slot> boolean has(Class<T> type) {
        return find(type) != null;
    }

    @Override
    public Iterator<Slot> iterator() {
        return _slots.iterator();
    }
}
