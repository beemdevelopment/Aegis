package com.beemdevelopment.aegis.helpers;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.beemdevelopment.aegis.vault.VaultEntry;
import com.beemdevelopment.aegis.ui.views.EntryAdapter;

public class SimpleItemTouchHelperCallback extends ItemTouchHelper.Callback {

    private VaultEntry _selectedEntry;

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

    public void setSelectedEntry(VaultEntry entry) {
        _selectedEntry = entry;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlags = 0;

        int position = viewHolder.getAdapterPosition();
        EntryAdapter adapter = (EntryAdapter)recyclerView.getAdapter();
        if (adapter.getEntryAt(position) != _selectedEntry)
        {
            dragFlags = 0;
        }

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
