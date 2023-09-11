package com.beemdevelopment.aegis.ui.views;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.helpers.TextDrawableHelper;
import com.beemdevelopment.aegis.icons.IconType;
import com.beemdevelopment.aegis.ui.glide.IconLoader;
import com.beemdevelopment.aegis.ui.models.AssignIconEntry;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

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

        if(!entry.getEntry().hasIcon()) {
            TextDrawable drawable = TextDrawableHelper.generate(entry.getEntry().getIssuer(), entry.getEntry().getName(), _oldIcon);
            _oldIcon.setImageDrawable(drawable);
        } else {
            Glide.with(_view.getContext())
                    .asDrawable()
                    .load(entry.getEntry())
                    .set(IconLoader.ICON_TYPE, entry.getEntry().getIconType())
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(_oldIcon);
        }

        setNewIcon();
    }

    private void setNewIcon() {
        if (_entry.getNewIcon() != null) {
            Glide.with(_view.getContext())
                    .asDrawable()
                    .load(_entry.getNewIcon().getFile())
                    .set(IconLoader.ICON_TYPE, _entry.getNewIcon().getIconType())
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(false)
                    .into(_newIcon);
        } else {
            Glide.with(_view.getContext())
                    .asDrawable()
                    .load(R.drawable.ic_icon_unselected)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(false)
                    .into(_newIcon);
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