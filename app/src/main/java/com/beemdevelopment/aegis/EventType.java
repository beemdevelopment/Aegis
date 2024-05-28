package com.beemdevelopment.aegis;

public enum EventType {

    VAULT_UNLOCKED,
    VAULT_BACKUP_CREATED,
    VAULT_ANDROID_BACKUP_CREATED,
    VAULT_EXPORTED,
    ENTRY_SHARED,
    VAULT_UNLOCK_FAILED_PASSWORD,
    VAULT_UNLOCK_FAILED_BIOMETRICS;
    private static EventType[] _values;

    static {
        _values = values();
    }

    public static EventType fromInteger(int x) {
        return _values[x];
    }

    public static int getEventTitleRes(EventType eventType) {
        switch (eventType) {
            case VAULT_UNLOCKED:
                return R.string.event_title_vault_unlocked;
            case VAULT_BACKUP_CREATED:
                return R.string.event_title_backup_created;
            case VAULT_ANDROID_BACKUP_CREATED:
                return R.string.event_title_android_backup_created;
            case VAULT_EXPORTED:
                return R.string.event_title_vault_exported;
            case ENTRY_SHARED:
                return R.string.event_title_entry_shared;
            case VAULT_UNLOCK_FAILED_PASSWORD:
                return R.string.event_title_vault_unlock_failed_password;
            case VAULT_UNLOCK_FAILED_BIOMETRICS:
                return R.string.event_title_vault_unlock_failed_biometrics;
            default:
                return R.string.event_unknown;
        }
    }
}
