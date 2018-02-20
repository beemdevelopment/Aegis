package me.impy.aegis.ui.dialogs;

import android.content.Context;
import android.support.v4.app.DialogFragment;

import javax.crypto.Cipher;

import me.impy.aegis.db.slots.Slot;

public class SlotDialogFragment extends DialogFragment {
    private Listener _listener;

    protected Listener getListener() {
        return _listener;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            _listener = (Listener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement SlotDialogFragment.Listener");
        }
    }

    public interface Listener {
        void onSlotResult(Slot slot, Cipher cipher);
        void onException(Exception e);
    }
}
