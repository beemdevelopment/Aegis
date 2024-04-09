package com.beemdevelopment.aegis.ui.views;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.vault.VaultGroup;

public class GroupHolder extends RecyclerView.ViewHolder {
    private TextView _slotName;
    private ImageView _buttonEdit;
    private ImageView _buttonDelete;

    public GroupHolder(final View view) {
        super(view);
        _slotName = view.findViewById(R.id.text_group_name);
        _buttonEdit = view.findViewById(R.id.button_edit);
        _buttonDelete = view.findViewById(R.id.button_delete);
    }

    public void setData(VaultGroup group) {
        _slotName.setText(group.getName());
    }

    public void setOnEditClickListener(View.OnClickListener listener) {
        _buttonEdit.setOnClickListener(listener);
    }

    public void setOnDeleteClickListener(View.OnClickListener listener) {
        _buttonDelete.setOnClickListener(listener);
    }
}
