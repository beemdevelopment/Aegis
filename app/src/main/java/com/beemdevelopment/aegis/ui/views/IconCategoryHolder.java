package com.beemdevelopment.aegis.ui.views;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.beemdevelopment.aegis.R;

public class IconCategoryHolder extends RecyclerView.ViewHolder {
    private final TextView _textView;
    private final ImageView _imgView;

    public IconCategoryHolder(final View view) {
        super(view);
        _textView = view.findViewById(R.id.icon_category);
        _imgView = view.findViewById(R.id.icon_category_indicator);
    }

    public void setData(IconAdapter.CategoryHeader header) {
        _textView.setText(header.getCategory());
        _imgView.setRotation(getRotation(header.isCollapsed()));
    }

    public void setIsCollapsed(boolean collapsed) {
        _imgView.animate()
                .setDuration(200)
                .rotation(getRotation(collapsed))
                .start();
    }

    private static int getRotation(boolean collapsed) {
        return collapsed ? 90 : 0;
    }
}
