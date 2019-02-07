package com.beemdevelopment.aegis.ui.tasks;

import android.content.Context;

import com.beemdevelopment.aegis.crypto.MasterKey;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import com.beemdevelopment.aegis.R;

import com.beemdevelopment.aegis.db.slots.FingerprintSlot;
import com.beemdevelopment.aegis.db.slots.PasswordSlot;
import com.beemdevelopment.aegis.db.slots.Slot;
import com.beemdevelopment.aegis.db.slots.SlotList;
import com.beemdevelopment.aegis.db.slots.SlotException;
import com.beemdevelopment.aegis.db.slots.SlotIntegrityException;

public class SlotListTask<T extends Slot> extends ProgressDialogTask<SlotListTask.Params, MasterKey> {
    private Callback _cb;
    private Class<T> _type;

    public SlotListTask(Class<T> type, Context context, Callback cb) {
        super(context, context.getString(R.string.unlocking_vault));
        _cb = cb;
        _type = type;
    }

    @Override
    protected MasterKey doInBackground(SlotListTask.Params... args) {
        setPriority();

        Params params = args[0];
        SlotList slots = params.getSlots();
        try {
            if (!slots.has(_type)) {
                throw new RuntimeException();
            }

            MasterKey masterKey = null;
            for (Slot slot : slots.findAll(_type)) {
                try {
                    if (slot instanceof PasswordSlot) {
                        char[] password = (char[])params.getObj();
                        SecretKey key = ((PasswordSlot)slot).deriveKey(password);
                        Cipher cipher = slot.createDecryptCipher(key);
                        masterKey = slot.getKey(cipher);
                    } else if (slot instanceof FingerprintSlot) {
                        masterKey = slot.getKey((Cipher)params.getObj());
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
        private SlotList _slots;
        private Object _obj;

        public Params(SlotList slots, Object obj) {
            _slots = slots;
            _obj = obj;
        }

        public SlotList getSlots() {
            return _slots;
        }

        public Object getObj() {
            return _obj;
        }
    }

    public interface Callback {
        void onTaskFinished(MasterKey key);
    }
}
