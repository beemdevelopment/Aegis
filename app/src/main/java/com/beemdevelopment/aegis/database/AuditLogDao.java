package com.beemdevelopment.aegis.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AuditLogDao {
    @Insert
    void insert(AuditLogEntry log);

    @Query("SELECT * FROM audit_logs WHERE timestamp >= strftime('%s', 'now', '-30 days') ORDER BY timestamp DESC")
    LiveData<List<AuditLogEntry>> getAll();
}