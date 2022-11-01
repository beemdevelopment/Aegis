package com.beemdevelopment.aegis.helpers;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.beemdevelopment.aegis.vault.VaultEntry;
import com.beemdevelopment.aegis.ui.views.EntryAdapter;

public class SimpleItemTouchHelperCallback extends ItemTouchHelper.Callback {

    private VaultEntry _selectedEntry;

    private final EntryAdapter _adapter;
    private boolean _positionChanged = false;
    private boolean _isLongPressDragEnabled = true;

    public SimpleItemTouchHelperCallback(EntryAdapter adapter) {
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
        if (entry == null) {
            _selectedEntry = null;
            return;
        }

        if (!entry.isFavorite()) {
            _selectedEntry = entry;
        }
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlags = 0;

        if (viewHolder != null) {
            int position = viewHolder.getAdapterPosition();
            EntryAdapter adapter = (EntryAdapter)recyclerView.getAdapter();
            if (adapter.isPositionFooter(position)
                || adapter.getEntryAt(position) != _selectedEntry
                || !isLongPressDragEnabled())
            {
                dragFlags = 0;
            }
        }

        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                          RecyclerView.ViewHolder target) {
        if (target.getAdapterPosition() < _adapter.getShownFavoritesCount()){
            return false;
        }
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
