package me.impy.aegis.ui.views;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import me.impy.aegis.R;
import me.impy.aegis.crypto.KeyStoreHandle;
import me.impy.aegis.crypto.KeyStoreHandleException;
import me.impy.aegis.db.slots.FingerprintSlot;
import me.impy.aegis.db.slots.PasswordSlot;
import me.impy.aegis.db.slots.RawSlot;
import me.impy.aegis.db.slots.Slot;
import me.impy.aegis.helpers.FingerprintHelper;

public class GroupHolder extends RecyclerView.ViewHolder {
    private TextView _slotName;
    private ImageView _buttonDelete;

    public GroupHolder(final View view) {
        super(view);
        _slotName = view.findViewById(R.id.text_slot_name);
        _buttonDelete = view.findViewById(R.id.button_delete);
    }

    public void setData(String groupName) {
        _slotName.setText(groupName);
    }

    public void setOnDeleteClickListener(View.OnClickListener listener) {
        _buttonDelete.setOnClickListener(listener);
    }
}
