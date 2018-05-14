package me.impy.aegis.db.slots;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.crypto.Cipher;
import me.impy.aegis.crypto.MasterKey;
import me.impy.aegis.encoding.Hex;
import me.impy.aegis.encoding.HexException;

public class SlotCollection implements Iterable<Slot>, Serializable {
    private List<Slot> _slots = new ArrayList<>();
    private byte[] _masterHash;

    public static JSONObject serialize(SlotCollection slots) throws SlotCollectionException {
        try {
            JSONObject obj = new JSONObject();
            obj.put("hash", Hex.encode(slots.getMasterHash()));

            JSONArray entries = new JSONArray();
            for (Slot slot : slots) {
                entries.put(slot.serialize());
            }

            obj.put("entries", entries);
            return obj;
        } catch (SlotException | JSONException e) {
            throw new SlotCollectionException(e);
        }
    }

    public static SlotCollection deserialize(JSONObject obj) throws SlotCollectionException {
        SlotCollection slots = new SlotCollection();

        try {
            byte[] masterHash = Hex.decode(obj.getString("hash"));
            slots.setMasterHash(masterHash);

            JSONArray entries = obj.getJSONArray("entries");
            for (int i = 0; i < entries.length(); i++) {
                Slot slot;
                JSONObject slotObj = entries.getJSONObject(i);

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
        } catch (SlotException | JSONException | HexException e) {
            throw new SlotCollectionException(e);
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

    public void encrypt(Slot slot, MasterKey key, Cipher cipher) throws SlotException {
        slot.setKey(key, cipher);
        setMasterHash(key.getHash());
    }

    public MasterKey decrypt(Slot slot, Cipher cipher) throws SlotException, SlotIntegrityException {
        byte[] hash = getMasterHash();
        MasterKey key = new MasterKey(slot.getKey(cipher));
        if (!Arrays.equals(hash, key.getHash())) {
            throw new SlotIntegrityException();
        }
        return key;
    }

    private void setMasterHash(byte[] masterHash) {
        _masterHash = masterHash;
    }

    private byte[] getMasterHash() {
        return _masterHash;
    }
}
