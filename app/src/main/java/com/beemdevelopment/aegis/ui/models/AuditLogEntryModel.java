package com.beemdevelopment.aegis.ui.models;

import com.beemdevelopment.aegis.database.AuditLogEntry;
import com.beemdevelopment.aegis.vault.VaultEntry;

import javax.annotation.Nullable;

public class AuditLogEntryModel {
    private AuditLogEntry _auditLogEntry;
    private VaultEntry _referencedVaultEntry;

    public AuditLogEntryModel(AuditLogEntry auditLogEntry, @Nullable VaultEntry referencedVaultEntry) {
        _auditLogEntry = auditLogEntry;
        _referencedVaultEntry = referencedVaultEntry;
    }

    public AuditLogEntry getAuditLogEntry() {
        return _auditLogEntry;
    }

    public VaultEntry getReferencedVaultEntry() {
        return _referencedVaultEntry;
    }
}
