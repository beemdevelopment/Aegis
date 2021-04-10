package com.beemdevelopment.aegis.ui.views;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.icons.IconPack;

public class IconPackHolder extends RecyclerView.ViewHolder {
    private final TextView _iconPackName;
    private final TextView _iconPackInfo;
    private final ImageView _buttonDelete;

    public IconPackHolder(final View view) {
        super(view);
        _iconPackName = view.findViewById(R.id.text_icon_pack_name);
        _iconPackInfo = view.findViewById(R.id.text_icon_pack_info);
        _buttonDelete = view.findViewById(R.id.button_delete);
    }

    public void setData(IconPack pack) {
        _iconPackName.setText(String.format("%s (v%d)", pack.getName(), pack.getVersion()));
        _iconPackInfo.setText(itemView.getResources().getQuantityString(R.plurals.icon_pack_info, pack.getIcons().size(), pack.getIcons().size()));
    }

    public void setOnDeleteClickListener(View.OnClickListener listener) {
        _buttonDelete.setOnClickListener(listener);
    }
}
