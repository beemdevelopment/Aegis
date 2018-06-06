package me.impy.aegis.ui.tasks;

import android.content.Context;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import me.impy.aegis.crypto.MasterKey;
import me.impy.aegis.db.slots.FingerprintSlot;
import me.impy.aegis.db.slots.PasswordSlot;
import me.impy.aegis.db.slots.Slot;
import me.impy.aegis.db.slots.SlotList;
import me.impy.aegis.db.slots.SlotException;
import me.impy.aegis.db.slots.SlotIntegrityException;

public class SlotCollectionTask<T extends Slot> extends ProgressDialogTask<SlotCollectionTask.Params, MasterKey> {
    private Callback _cb;
    private Class<T> _type;

    public SlotCollectionTask(Class<T> type, Context context, Callback cb) {
        super(context, "Decrypting database");
        _cb = cb;
        _type = type;
    }

    @Override
    protected MasterKey doInBackground(SlotCollectionTask.Params... args) {
        setPriority();

        Params params = args[0];
        try {
            if (!params.Slots.has(_type)) {
                throw new RuntimeException();
            }

            MasterKey masterKey = null;
            for (Slot slot : params.Slots.findAll(_type)) {
                try {
                    if (slot instanceof PasswordSlot) {
                        char[] password = (char[])params.Obj;
                        SecretKey key = ((PasswordSlot)slot).deriveKey(password);
                        Cipher cipher = slot.createDecryptCipher(key);
                        masterKey = slot.getKey(cipher);
                    } else if (slot instanceof FingerprintSlot) {
                        masterKey = slot.getKey((Cipher)params.Obj);
                    } else {
                        throw new RuntimeException();
                    }
                    break;
                } catch (SlotIntegrityException e) {

                }
            }

            if (masterKey == null) {
                throw new SlotIntegrityException();
            }

            return masterKey;
        } catch (SlotIntegrityException e) {
            return null;
        } catch (SlotException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onPostExecute(MasterKey masterKey) {
        super.onPostExecute(masterKey);
        _cb.onTaskFinished(masterKey);
    }

    public static class Params {
        public SlotList Slots;
        public Object Obj;
    }

    public interface Callback {
        void onTaskFinished(MasterKey key);
    }
}
