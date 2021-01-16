package com.beemdevelopment.aegis.ui.tasks;

import android.content.Context;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.crypto.CryptoUtils;
import com.beemdevelopment.aegis.crypto.MasterKey;
import com.beemdevelopment.aegis.vault.slots.PasswordSlot;
import com.beemdevelopment.aegis.vault.slots.Slot;
import com.beemdevelopment.aegis.vault.slots.SlotException;
import com.beemdevelopment.aegis.vault.slots.SlotIntegrityException;

import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

public class PasswordSlotDecryptTask extends ProgressDialogTask<PasswordSlotDecryptTask.Params, PasswordSlotDecryptTask.Result> {
    private Callback _cb;

    public PasswordSlotDecryptTask(Context context, Callback cb) {
        super(context, context.getString(R.string.unlocking_vault));
        _cb = cb;
    }

    @Override
    protected Result doInBackground(PasswordSlotDecryptTask.Params... args) {
        setPriority();

        Params params = args[0];
        return decrypt(params.getSlots(), params.getPassword());
    }

    public static Result decrypt(List<PasswordSlot> slots, char[] password) {
        for (PasswordSlot slot : slots) {
            try {
                return decryptPasswordSlot(slot, password);
            } catch (SlotException e) {
                throw new RuntimeException(e);
            } catch (SlotIntegrityException ignored) {

            }
        }

        return null;
    }

    public static Result decryptPasswordSlot(PasswordSlot slot, char[] password)
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

    public static MasterKey decryptPasswordSlot(PasswordSlot slot, SecretKey key)
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
        private List<PasswordSlot> _slots;
        private char[] _password;

        public Params(List<PasswordSlot> slots, char[] password) {
            _slots = slots;
            _password = password;
        }

        public List<PasswordSlot> getSlots() {
            return _slots;
        }

        public char[] getPassword() {
            return _password;
        }
    }

    public static class Result {
        private MasterKey _key;
        private PasswordSlot _slot;
        private boolean _repaired;

        public Result(MasterKey key, PasswordSlot slot, boolean repaired) {
            _key = key;
            _slot = slot;
            _repaired = repaired;
        }

        public Result(MasterKey key, PasswordSlot slot) {
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
