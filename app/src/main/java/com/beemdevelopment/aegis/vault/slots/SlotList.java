package com.beemdevelopment.aegis.vault.slots;

import com.beemdevelopment.aegis.util.UUIDMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SlotList extends UUIDMap<Slot> {
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

    @Override
    public void add(Slot slot) {
        if (has(slot.getClass())) {
            throw new AssertionError(String.format("Only one slot of type %s is allowed in a SlotList", slot.getClass().getSimpleName()));
        }

        super.add(slot);
    }

    public <T extends Slot> T get(Class<T> type) {
        for (Slot slot : this) {
            if (slot.getClass() == type) {
                return type.cast(slot);
            }
        }
        return null;
    }

    public <T extends Slot> boolean has(Class<T> type) {
        return get(type) != null;
    }
}
