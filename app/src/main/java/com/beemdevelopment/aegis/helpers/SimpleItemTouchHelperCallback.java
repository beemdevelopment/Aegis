package com.beemdevelopment.aegis.helpers;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

public class SimpleItemTouchHelperCallback extends ItemTouchHelper.Callback {

    private final ItemTouchHelperAdapter _adapter;
    private boolean _positionChanged = false;
    private boolean _isLongPressDragEnabled = true;

    public SimpleItemTouchHelperCallback(ItemTouchHelperAdapter adapter) {
        _adapter = adapter;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return _isLongPressDragEnabled;
    }

    public void setIsLongPressDragEnabled(boolean enabled) {
        _isLongPressDragEnabled = enabled;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                          RecyclerView.ViewHolder target) {
        _adapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        _positionChanged = true;
        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        _adapter.onItemDismiss(viewHolder.getAdapterPosition());
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);

        if (_positionChanged) {
            _adapter.onItemDrop(viewHolder.getAdapterPosition());
            _positionChanged = false;
        }
    }
}
