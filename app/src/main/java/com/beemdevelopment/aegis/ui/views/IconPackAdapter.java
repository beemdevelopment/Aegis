package com.beemdevelopment.aegis.ui.views;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.icons.IconPack;

import java.util.ArrayList;
import java.util.List;

public class IconPackAdapter extends RecyclerView.Adapter<IconPackHolder> {
    private IconPackAdapter.Listener _listener;
    private List<IconPack> _iconPacks;

    public IconPackAdapter(IconPackAdapter.Listener listener) {
        _listener = listener;
        _iconPacks = new ArrayList<>();
    }

    public void addIconPack(IconPack pack) {
        _iconPacks.add(pack);

        int position = getItemCount() - 1;
        if (position == 0) {
            notifyDataSetChanged();
        } else {
            notifyItemInserted(position);
        }
    }

    public void removeIconPack(IconPack pack) {
        int position = _iconPacks.indexOf(pack);
        if (position >= 0) {
            _iconPacks.remove(position);
            notifyItemRemoved(position);
        }
    }

    @NonNull
    @Override
    public IconPackHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_icon_pack, parent, false);
        return new IconPackHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IconPackHolder holder, int position) {
        holder.setData(_iconPacks.get(position));
        holder.setOnDeleteClickListener(v -> {
            int position12 = holder.getAdapterPosition();
            _listener.onRemoveIconPack(_iconPacks.get(position12));
        });
    }

    @Override
    public int getItemCount() {
        return _iconPacks.size();
    }

    public interface Listener {
        void onRemoveIconPack(IconPack pack);
    }
}
