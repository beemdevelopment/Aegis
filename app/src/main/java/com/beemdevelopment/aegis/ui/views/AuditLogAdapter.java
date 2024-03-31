package com.beemdevelopment.aegis.ui.views;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.database.AuditLogEntry;
import com.beemdevelopment.aegis.ui.models.AuditLogEntryModel;
import com.beemdevelopment.aegis.vault.VaultEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuditLogAdapter extends RecyclerView.Adapter<AuditLogHolder> {
    private List<AuditLogEntryModel> _auditLogEntryModels;
    private List<VaultEntry> _referencedEntries;

    public AuditLogAdapter() {
        _auditLogEntryModels = new ArrayList<>();
        _referencedEntries = new ArrayList<>();
    }

    public void addAuditLogEntryModel(AuditLogEntryModel auditLogEntryModel) {
        _auditLogEntryModels.add(auditLogEntryModel);

        int position = getItemCount() - 1;
        if (position == 0) {
            notifyDataSetChanged();
        } else {
            notifyItemInserted(position);
        }
    }

    public void addReferencedEntry(VaultEntry vaultEntry) {
        _referencedEntries.add(vaultEntry);
    }

    @NonNull
    @Override
    public AuditLogHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_audit_log, parent, false);
        return new AuditLogHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AuditLogHolder holder, int position) {
        AuditLogEntryModel auditLogEntryModel = _auditLogEntryModels.get(position);

        VaultEntry referencedEntry = null;
        holder.setData(auditLogEntryModel);
    }

    @Override
    public int getItemCount() {
        return _auditLogEntryModels.size();
    }
}
