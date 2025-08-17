package com.beemdevelopment.aegis.ui.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.vault.VaultEntry;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

public class TrashEntryAdapter extends RecyclerView.Adapter<TrashEntryAdapter.TrashEntryViewHolder> {

    private final Context context;
    private final List<VaultEntry> trashEntries;
    private final TrashEntryListener listener;

    public interface TrashEntryListener {
        void onRestore(VaultEntry entry);
        void onDeletePermanently(VaultEntry entry);
    }

    public TrashEntryAdapter(Context context, Collection<VaultEntry> trashEntries, TrashEntryListener listener) {
        this.context = context;
        this.trashEntries = new ArrayList<>(trashEntries);
        this.listener = listener;
    }

    @NonNull
    @Override
    public TrashEntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_trash_entry, parent, false);
        return new TrashEntryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrashEntryViewHolder holder, int position) {
        VaultEntry entry = trashEntries.get(position);
        holder.issuer.setText(entry.getIssuer());
        holder.name.setText(entry.getName());

        holder.restoreButton.setOnClickListener(v -> listener.onRestore(entry));
        holder.deletePermanentlyButton.setOnClickListener(v -> listener.onDeletePermanently(entry));
    }

    @Override
    public int getItemCount() {
        return trashEntries.size();
    }

    public void removeItem(VaultEntry entry) {
        int position = trashEntries.indexOf(entry);
        if (position > -1) {
            trashEntries.remove(position);
            notifyItemRemoved(position);
        }
    }

    static class TrashEntryViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView issuer;
        TextView name;
        Button restoreButton;
        Button deletePermanentlyButton;

        TrashEntryViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            issuer = itemView.findViewById(R.id.issuer);
            name = itemView.findViewById(R.id.name);
            restoreButton = itemView.findViewById(R.id.button_restore);
            deletePermanentlyButton = itemView.findViewById(R.id.button_delete_permanently);
        }
    }
}
