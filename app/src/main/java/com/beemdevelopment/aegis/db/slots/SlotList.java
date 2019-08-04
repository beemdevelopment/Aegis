package com.beemdevelopment.aegis.db.slots;

import com.beemdevelopment.aegis.util.UUIDMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

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
}
