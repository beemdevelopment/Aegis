package com.beemdevelopment.aegis.vault.slots;

import com.beemdevelopment.aegis.util.UUIDMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    public List<PasswordSlot> findBackupPasswordSlots() {
        return findAll(PasswordSlot.class)
                .stream()
                .filter(PasswordSlot::isBackup)
                .collect(Collectors.toList());
    }

    public List<PasswordSlot> findRegularPasswordSlots() {
        return findAll(PasswordSlot.class)
                .stream()
                .filter(s -> !s.isBackup())
                .collect(Collectors.toList());
    }

    public <T extends Slot> boolean has(Class<T> type) {
        return find(type) != null;
    }

    /**
     * Returns a copy of this SlotList that is suitable for exporting.
     * Strips biometric slots.
     * In case there's a backup password slot, any regular password slots are stripped.
     */
    public SlotList exportable() {
        boolean hasBackupSlots = false;
        for (Slot slot : this) {
            if (slot instanceof PasswordSlot && ((PasswordSlot) slot).isBackup()) {
                hasBackupSlots = true;
                break;
            }
        }
        SlotList slots = new SlotList();
        for (Slot slot : this) {
            if (slot instanceof BiometricSlot) {
                continue;
            }
            if (hasBackupSlots && slot instanceof PasswordSlot && !((PasswordSlot) slot).isBackup()) {
                continue;
            }
            slots.add(slot);
        }
        return slots;
    }
}
