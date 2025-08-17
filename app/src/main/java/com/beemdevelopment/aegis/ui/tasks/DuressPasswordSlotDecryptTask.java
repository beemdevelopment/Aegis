package com.beemdevelopment.aegis.ui.tasks;

import android.content.Context;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.crypto.CryptoUtils;
import com.beemdevelopment.aegis.crypto.MasterKey;
import com.beemdevelopment.aegis.vault.slots.DuressPasswordSlot;
import com.beemdevelopment.aegis.vault.slots.Slot;
import com.beemdevelopment.aegis.vault.slots.SlotException;
import com.beemdevelopment.aegis.vault.slots.SlotIntegrityException;

import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

public class DuressPasswordSlotDecryptTask extends ProgressDialogTask<DuressPasswordSlotDecryptTask.Params, DuressPasswordSlotDecryptTask.Result> {
    private Callback _cb;

    public DuressPasswordSlotDecryptTask(Context context, Callback cb) {
        super(context, context.getString(R.string.unlocking_vault));
        _cb = cb;
    }

    @Override
    protected Result doInBackground(DuressPasswordSlotDecryptTask.Params... args) {
        setPriority();

        Params params = args[0];
        return decrypt(params.getSlots(), params.getPassword());
    }

    public static Result decrypt(List<DuressPasswordSlot> slots, char[] password) {
        for (DuressPasswordSlot slot : slots) {
            try {
                return decryptDuressPasswordSlot(slot, password);
            } catch (SlotException e) {
                throw new RuntimeException(e);
            } catch (SlotIntegrityException ignored) {

            }
        }

        return null;
    }

    public static Result decryptDuressPasswordSlot(DuressPasswordSlot slot, char[] password)
            throws SlotIntegrityException, SlotException {
        MasterKey masterKey;
        SecretKey key = slot.deriveKey(password);
        byte[] oldPasswordBytes = CryptoUtils.toBytesOld(password);

        try {
            masterKey = decryptDuressPasswordSlot(slot, key);
        } catch (SlotIntegrityException e) {
            if (slot.isRepaired() || oldPasswordBytes.length <= 64) {
                throw e;
            }

            SecretKey oldKey = slot.deriveKey(oldPasswordBytes);
            masterKey = decryptDuressPasswordSlot(slot, oldKey);
        }

        boolean repaired = false;
        if (!slot.isRepaired()) {
            Cipher cipher = Slot.createEncryptCipher(key);
            slot.setKey(masterKey, cipher);
            repaired = true;
        }

        return new Result(masterKey, slot, repaired);
    }

    public static MasterKey decryptDuressPasswordSlot(DuressPasswordSlot slot, SecretKey key)
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
        private List<DuressPasswordSlot> _slots;
        private char[] _password;

        public Params(List<DuressPasswordSlot> slots, char[] password) {
            _slots = slots;
            _password = password;
        }

        public List<DuressPasswordSlot> getSlots() {
            return _slots;
        }

        public char[] getPassword() {
            return _password;
        }
    }

    public static class Result {
        private MasterKey _key;
        private DuressPasswordSlot _slot;
        private boolean _repaired;

        public Result(MasterKey key, DuressPasswordSlot slot, boolean repaired) {
            _key = key;
            _slot = slot;
            _repaired = repaired;
        }

        public Result(MasterKey key, DuressPasswordSlot slot) {
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
