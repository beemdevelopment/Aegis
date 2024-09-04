package com.beemdevelopment.aegis.ui.views;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.beemdevelopment.aegis.EventType;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.database.AuditLogEntry;
import com.beemdevelopment.aegis.ui.models.AuditLogEntryModel;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.google.android.material.color.MaterialColors;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;

public class AuditLogHolder extends RecyclerView.ViewHolder {

    private final CardView _cardView;
    private final ImageView _auditLogEntryIcon;
    private final TextView _auditLogEntryTitle;
    private final TextView _auditLogEntryDescription;
    private final TextView _auditLogEntryReference;
    private final TextView _auditLogEntryTimestamp;
    private final int _errorBackgroundColor;
    private final ColorStateList _initialBackgroundColor;
    private final int _initialIconColor;

    public AuditLogHolder(final View view) {
        super(view);

        _cardView = (CardView)view;
        _auditLogEntryIcon = view.findViewById(R.id.iv_icon_view);
        _auditLogEntryTitle = view.findViewById(R.id.text_audit_log_title);
        _auditLogEntryDescription = view.findViewById(R.id.text_audit_log_description);
        _auditLogEntryReference = view.findViewById(R.id.text_audit_log_reference);
        _auditLogEntryTimestamp = view.findViewById(R.id.text_audit_log_timestamp);

        _initialBackgroundColor = _cardView.getCardBackgroundColor();
        _initialIconColor = MaterialColors.getColor(view, com.google.android.material.R.attr.colorTertiaryContainer);
        _errorBackgroundColor = MaterialColors.getColor(view, com.google.android.material.R.attr.colorErrorContainer);
    }

    public void setData(AuditLogEntryModel auditLogEntryModel) {
        AuditLogEntry auditLogEntry = auditLogEntryModel.getAuditLogEntry();
        _auditLogEntryIcon.setImageResource(getIconResource(auditLogEntry.getEventType()));
        _auditLogEntryTitle.setText(EventType.getEventTitleRes(auditLogEntry.getEventType()));
        _auditLogEntryDescription.setText(getEventTypeDescriptionRes(auditLogEntry.getEventType()));

        _auditLogEntryTimestamp.setText(formatTimestamp(_cardView.getContext(), auditLogEntry.getTimestamp()).toLowerCase());

        if (auditLogEntryModel.getReferencedVaultEntry() != null) {
            VaultEntry referencedVaultEntry = auditLogEntryModel.getReferencedVaultEntry();
            _auditLogEntryReference.setText(String.format("%s (%s)", referencedVaultEntry.getIssuer(), referencedVaultEntry.getName()));
            _auditLogEntryReference.setVisibility(View.VISIBLE);
        } else if (auditLogEntryModel.getAuditLogEntry().getReference() != null) {
            _auditLogEntryReference.setText(R.string.audit_log_entry_deleted);
            _auditLogEntryReference.setVisibility(View.VISIBLE);
        } else {
            _auditLogEntryReference.setVisibility(View.GONE);
        }

        setCardBackgroundColor(auditLogEntry.getEventType());
    }

    private void setCardBackgroundColor(EventType eventType) {
        if (eventType == EventType.VAULT_UNLOCK_FAILED_PASSWORD || eventType == EventType.VAULT_UNLOCK_FAILED_BIOMETRICS) {
            _cardView.setCardBackgroundColor(_errorBackgroundColor);
            _auditLogEntryIcon.setBackgroundColor(_errorBackgroundColor);
        } else {
            _cardView.setCardBackgroundColor(_initialBackgroundColor);
            _auditLogEntryIcon.setBackgroundColor(_initialIconColor);
        }
    }

    private int getEventTypeDescriptionRes(EventType eventType) {
        switch (eventType) {
            case VAULT_UNLOCKED:
                return R.string.event_description_vault_unlocked;
            case VAULT_BACKUP_CREATED:
                return R.string.event_description_backup_created;
            case VAULT_ANDROID_BACKUP_CREATED:
                return R.string.event_description_android_backup_created;
            case VAULT_EXPORTED:
                return R.string.event_description_vault_exported;
            case ENTRY_SHARED:
                return R.string.event_description_entry_shared;
            case VAULT_UNLOCK_FAILED_PASSWORD:
                return R.string.event_description_vault_unlock_failed_password;
            case VAULT_UNLOCK_FAILED_BIOMETRICS:
                return R.string.event_description_vault_unlock_failed_biometrics;
            default:
                return R.string.event_unknown;
        }
    }

    private int getIconResource(EventType eventType) {
        switch(eventType) {
            case VAULT_UNLOCKED:
                return R.drawable.ic_lock_open;
            case VAULT_BACKUP_CREATED:
            case VAULT_ANDROID_BACKUP_CREATED:
                return R.drawable.ic_folder_zip;
            case VAULT_EXPORTED:
                return R.drawable.ic_export_notes;
            case ENTRY_SHARED:
                return R.drawable.ic_share;
            case VAULT_UNLOCK_FAILED_PASSWORD:
            case VAULT_UNLOCK_FAILED_BIOMETRICS:
                return R.drawable.ic_lock;
        }

        return -1;
    }

    private static String formatTimestamp(Context context, long epochMilli) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime timestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.systemDefault());

        long daysBetween = ChronoUnit.DAYS.between(timestamp.toLocalDate(), now.toLocalDate());
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");

        if (daysBetween < 1) {
            String formattedTime = timestamp.format(timeFormatter);
            return context.getString(R.string.today_at_time, formattedTime);
        } else if (daysBetween < 7) {
            DateTimeFormatter dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEEE");
            String dayOfWeek = timestamp.format(dayOfWeekFormatter);
            String formattedTime = timestamp.format(timeFormatter);
            return context.getString(R.string.day_of_week_at_time, dayOfWeek, formattedTime);
        } else {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
            return timestamp.format(dateFormatter);
        }
    }
}
