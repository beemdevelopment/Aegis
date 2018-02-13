package me.impy.aegis;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import me.impy.aegis.crypto.KeyStoreHandle;
import me.impy.aegis.crypto.KeyStoreHandleException;
import me.impy.aegis.crypto.slots.FingerprintSlot;
import me.impy.aegis.crypto.slots.PasswordSlot;
import me.impy.aegis.crypto.slots.RawSlot;
import me.impy.aegis.crypto.slots.Slot;
import me.impy.aegis.helpers.FingerprintHelper;

public class SlotHolder extends RecyclerView.ViewHolder {
    private TextView _slotUsed;
    private TextView _slotName;
    private ImageView _slotImg;
    private LinearLayout _buttonEdit;
    private ImageView _buttonDelete;

    public SlotHolder(final View view) {
        super(view);
        _slotUsed = view.findViewById(R.id.text_slot_used);
        _slotName = view.findViewById(R.id.text_slot_name);
        _slotImg = view.findViewById(R.id.img_slot);
        _buttonEdit = view.findViewById(R.id.button_edit);
        _buttonDelete = view.findViewById(R.id.button_delete);
    }

    public void setData(Slot slot) {
        if (slot instanceof PasswordSlot) {
            _slotName.setText("Password");
            _slotImg.setImageResource(R.drawable.ic_create_black_24dp);
        } else if (slot instanceof FingerprintSlot) {
            _slotName.setText("Finger");
            _slotImg.setImageResource(R.drawable.ic_fingerprint_black_24dp);
            if (FingerprintHelper.isSupported()) {
                try {
                    KeyStoreHandle keyStore = new KeyStoreHandle();
                    if (keyStore.containsKey(slot.getID())) {
                        _slotUsed.setVisibility(View.VISIBLE);
                    }
                } catch (KeyStoreHandleException e) { }
            }
        } else if (slot instanceof RawSlot) {
            _slotName.setText("Raw");
            _slotImg.setImageResource(R.drawable.ic_vpn_key_black_24dp);
        } else {
            throw new RuntimeException();
        }
    }

    public void setOnEditClickListener(View.OnClickListener listener) {
        _buttonEdit.setOnClickListener(listener);
    }

    public void setOnDeleteClickListener(View.OnClickListener listener) {
        _buttonDelete.setOnClickListener(listener);
    }
}
