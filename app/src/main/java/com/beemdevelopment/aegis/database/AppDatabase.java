package com.beemdevelopment.aegis.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {AuditLogEntry.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract AuditLogDao auditLogDao();
}