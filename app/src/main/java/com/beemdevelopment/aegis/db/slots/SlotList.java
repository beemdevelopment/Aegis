package com.beemdevelopment.aegis.db.slots;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

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
                JSONObject obj = array.getJSONObject(i);
                Slot slot = Slot.fromJson(obj);
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

    public void replace(Slot newSlot) {
        Slot oldSlot = mustGetByUUID(newSlot.getUUID());
        _slots.set(_slots.indexOf(oldSlot), newSlot);
    }

    public Slot getByUUID(UUID uuid) {
        for (Slot slot : _slots) {
            if (slot.getUUID().equals(uuid)) {
                return slot;
            }
        }
        return null;
    }

    private Slot mustGetByUUID(UUID uuid) {
        Slot slot = getByUUID(uuid);
        if (slot == null) {
            throw new AssertionError(String.format("no slot found with UUID: %s", uuid.toString()));
        }
        return slot;
    }

    @Override
    public Iterator<Slot> iterator() {
        return _slots.iterator();
    }
}
