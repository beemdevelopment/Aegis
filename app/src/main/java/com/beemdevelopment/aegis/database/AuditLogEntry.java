package com.beemdevelopment.aegis.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.beemdevelopment.aegis.EventType;

@Entity(tableName = "audit_logs")
public class AuditLogEntry {
    @PrimaryKey(autoGenerate = true)
    protected long id;

    @NonNull
    @ColumnInfo(name = "event_type")
    private final EventType _eventType;

    @ColumnInfo(name = "reference")
    private final String _reference;

    @ColumnInfo(name = "timestamp")
    private final long _timestamp;

    @Ignore
    public AuditLogEntry(@NonNull EventType eventType) {
        this(eventType, null);
    }

    @Ignore
    public AuditLogEntry(@NonNull EventType eventType, @Nullable String reference) {
        _eventType = eventType;
        _reference = reference;
        _timestamp = System.currentTimeMillis();
    }

    AuditLogEntry(long id, @NonNull EventType eventType, @Nullable String reference, long timestamp) {
        this.id = id;
        _eventType = eventType;
        _reference = reference;
        _timestamp = timestamp;
    }

    public long getId() {
        return id;
    }

    public EventType getEventType() {
        return _eventType;
    }

    public String getReference() {
        return _reference;
    }

    public long getTimestamp() {
        return _timestamp;
    }
}