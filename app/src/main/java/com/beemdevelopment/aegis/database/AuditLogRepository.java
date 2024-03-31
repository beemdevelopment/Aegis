package com.beemdevelopment.aegis.database;

import androidx.lifecycle.LiveData;

import com.beemdevelopment.aegis.EventType;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AuditLogRepository {
    private final AuditLogDao _auditLogDao;
    private final Executor _executor;

    public AuditLogRepository(AuditLogDao auditLogDao) {
        _auditLogDao = auditLogDao;
        _executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<AuditLogEntry>> getAllAuditLogEntries() {
        return _auditLogDao.getAll();
    }

    public void addVaultUnlockedEvent() {
        AuditLogEntry auditLogEntry = new AuditLogEntry(EventType.VAULT_UNLOCKED);
        insert(auditLogEntry);
    }

    public void addBackupCreatedEvent() {
        AuditLogEntry auditLogEntry = new AuditLogEntry(EventType.VAULT_BACKUP_CREATED);
        insert(auditLogEntry);
    }

    public void addAndroidBackupCreatedEvent() {
        AuditLogEntry auditLogEntry = new AuditLogEntry(EventType.VAULT_ANDROID_BACKUP_CREATED);
        insert(auditLogEntry);
    }

    public void addVaultExportedEvent() {
        AuditLogEntry auditLogEntry = new AuditLogEntry(EventType.VAULT_EXPORTED);
        insert(auditLogEntry);
    }

    public void addEntrySharedEvent(String reference) {
        AuditLogEntry auditLogEntry = new AuditLogEntry(EventType.ENTRY_SHARED, reference);
        insert(auditLogEntry);
    }

    public void addVaultUnlockFailedPasswordEvent() {
        AuditLogEntry auditLogEntry = new AuditLogEntry(EventType.VAULT_UNLOCK_FAILED_PASSWORD);
        insert(auditLogEntry);

    }

    public void addVaultUnlockFailedBiometricsEvent() {
        AuditLogEntry auditLogEntry = new AuditLogEntry(EventType.VAULT_UNLOCK_FAILED_BIOMETRICS);
        insert(auditLogEntry);
    }

    public void insert(AuditLogEntry auditLogEntry) {
        _executor.execute(() -> {
            _auditLogDao.insert(auditLogEntry);
        });
    }

}
