package com.beemdevelopment.aegis.ui.views;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.ui.glide.GlideHelper;
import com.beemdevelopment.aegis.ui.models.AssignIconEntry;
import com.bumptech.glide.Glide;

public class AssignIconHolder extends RecyclerView.ViewHolder implements AssignIconEntry.Listener {
    private View _view;

    private AssignIconEntry _entry;
    private TextView _issuer;
    private TextView _accountName;
    private ImageView _oldIcon;
    private ImageView _newIcon;
    private ImageView _btnReset;

    public AssignIconHolder(final View view) {
        super(view);

        _view = view.findViewById(R.id.rlCardEntry);

        _issuer = view.findViewById(R.id.tvIssuer);
        _accountName = view.findViewById(R.id.tvAccountName);
        _oldIcon = view.findViewById(R.id.ivOldImage);
        _newIcon = view.findViewById(R.id.ivNewImage);
        _btnReset = view.findViewById(R.id.btnReset);
        _btnReset.setOnClickListener(l -> _entry.setNewIcon(null));
    }

    public void setData(AssignIconEntry entry) {
        _entry = entry;
        _issuer.setText(entry.getEntry().getIssuer());
        _accountName.setText(entry.getEntry().getName());

        GlideHelper.loadEntryIcon(Glide.with(_view.getContext()), _entry.getEntry(), _oldIcon);
        setNewIcon();
    }

    private void setNewIcon() {
        if (_entry.getNewIcon() != null) {
            GlideHelper.loadIcon(Glide.with(_view.getContext()), _entry.getNewIcon(), _newIcon);
        } else {
            Glide.with(_view.getContext()).clear(_newIcon);
            _newIcon.setImageResource(R.drawable.ic_unselected);
        }

        _btnReset.setVisibility(_entry.getNewIcon() != null ? View.VISIBLE : View.INVISIBLE);
    }

    public View getOldIconView() {
        return _oldIcon;
    }

    @Override
    public void onNewIconChanged() {
        setNewIcon();
    }
}