package com.beemdevelopment.aegis.ui.tasks;

import android.app.ProgressDialog;
import android.content.Context;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.crypto.CryptoUtils;
import com.beemdevelopment.aegis.crypto.MasterKey;
import com.beemdevelopment.aegis.db.slots.BiometricSlot;
import com.beemdevelopment.aegis.db.slots.PasswordSlot;
import com.beemdevelopment.aegis.db.slots.Slot;
import com.beemdevelopment.aegis.db.slots.SlotException;
import com.beemdevelopment.aegis.db.slots.SlotIntegrityException;
import com.beemdevelopment.aegis.db.slots.SlotList;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

public class SlotListTask<T extends Slot> extends ProgressDialogTask<SlotListTask.Params, SlotListTask.Result> {
    private Callback _cb;
    private Class<T> _type;

    public SlotListTask(Class<T> type, Context context, Callback cb) {
        super(context, context.getString(R.string.unlocking_vault));
        _cb = cb;
        _type = type;
    }

    @Override
    protected Result doInBackground(SlotListTask.Params... args) {
        setPriority();

        Params params = args[0];
        SlotList slots = params.getSlots();

        for (Slot slot : slots.findAll(_type)) {
            try {
                if (slot instanceof PasswordSlot) {
                    char[] password = (char[]) params.getObj();
                    return decryptPasswordSlot((PasswordSlot) slot, password);
                } else if (slot instanceof BiometricSlot) {
                    return decryptBiometricSlot((BiometricSlot) slot, (Cipher) params.getObj());
                }
            } catch (SlotException e) {
                throw new RuntimeException(e);
            } catch (SlotIntegrityException ignored) {

            }
        }

        return null;
    }

    private Result decryptBiometricSlot(BiometricSlot slot, Cipher cipher)
            throws SlotException, SlotIntegrityException {
        MasterKey key = slot.getKey(cipher);
        return new Result(key, slot);
    }

    private Result decryptPasswordSlot(PasswordSlot slot, char[] password)
            throws SlotIntegrityException, SlotException {
        MasterKey masterKey;
        SecretKey key = slot.deriveKey(password);
        byte[] oldPasswordBytes = CryptoUtils.toBytesOld(password);

        try {
            masterKey = decryptPasswordSlot(slot, key);
        } catch (SlotIntegrityException e) {
            // a bug introduced in afb9e59 caused passwords longer than 64 bytes to produce a different key than before
            // so, try again with the old password encode function if the password is longer than 64 bytes
            if (slot.isRepaired() || oldPasswordBytes.length <= 64) {
                throw e;
            }

            ProgressDialog dialog = getDialog();
            dialog.setMessage(dialog.getContext().getString(R.string.unlocking_vault_repair));

            // try to decrypt the password slot with the old key
            SecretKey oldKey = slot.deriveKey(oldPasswordBytes);
            masterKey = decryptPasswordSlot(slot, oldKey);
        }

        // if necessary, repair the slot by re-encrypting the master key with the correct key
        // slots with passwords smaller than 64 bytes also get this treatment to make sure those also have 'repaired' set to true
        boolean repaired = false;
        if (!slot.isRepaired()) {
            Cipher cipher = Slot.createEncryptCipher(key);
            slot.setKey(masterKey, cipher);
            repaired = true;
        }

        return new Result(masterKey, slot, repaired);
    }

    private MasterKey decryptPasswordSlot(PasswordSlot slot, SecretKey key)
            throws SlotException, SlotIntegrityException {
        Cipher cipher = slot.createDecryptCipher(key);
        return slot.getKey(cipher);
    }

    @Override
    protected void onPostExecute(Result result) {
        super.onPostExecute(result);
        _cb.onTaskFinished(result);
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

    public static class Result {
        private MasterKey _key;
        private Slot _slot;
        private boolean _repaired;

        public Result(MasterKey key, Slot slot, boolean repaired) {
            _key = key;
            _slot = slot;
            _repaired = repaired;
        }

        public Result(MasterKey key, Slot slot) {
            this(key, slot, false);
        }

        public MasterKey getKey() {
            return _key;
        }

        public Slot getSlot() {
            return _slot;
        }

        public boolean isSlotRepaired() {
            return _repaired;
        }
    }

    public interface Callback {
        void onTaskFinished(Result result);
    }
}
