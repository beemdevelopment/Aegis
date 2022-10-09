package com.beemdevelopment.aegis.ui.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// source (slightly modified for Aegis): https://github.com/chiuki/android-recyclerview/blob/745dc88/app/src/main/java/com/sqisland/android/recyclerview/AutofitRecyclerView.java
public class IconRecyclerView extends RecyclerView {
    private GridLayoutManager _manager;
    private int _columnWidth = -1;
    private int _spanCount;

    public IconRecyclerView(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public IconRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public IconRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @SuppressLint("ResourceType")
    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            int[] attrsArray = {
                    android.R.attr.columnWidth
            };
            TypedArray array = context.obtainStyledAttributes(attrs, attrsArray);
            _columnWidth = array.getDimensionPixelSize(0, -1);
            array.recycle();
        }

        _manager = new GridLayoutManager(getContext(), 1);
        setLayoutManager(_manager);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        if (_columnWidth > 0) {
            _spanCount = Math.max(1, getMeasuredWidth() / _columnWidth);
            _manager.setSpanCount(_spanCount);
        }
    }

    public GridLayoutManager getGridLayoutManager() {
        return _manager;
    }

    public int getSpanCount() {
        return _spanCount;
    }
}
