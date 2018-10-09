package me.impy.aegis.ui.dialogs;

import android.app.Activity;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;

import me.impy.aegis.R;

public class Dialogs {
    private Dialogs() {

    }

    public static AlertDialog showDeleteEntryDialog(Activity activity, DialogInterface.OnClickListener onDelete) {
        return new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.delete_entry))
                .setMessage(activity.getString(R.string.delete_entry_description))
                .setPositiveButton(android.R.string.yes, onDelete)
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    public static AlertDialog showDiscardDialog(Activity activity, DialogInterface.OnClickListener onSave, DialogInterface.OnClickListener onDiscard) {
        return new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.discard_changes))
                .setMessage(activity.getString(R.string.discard_changes_description))
                .setPositiveButton(R.string.save, onSave)
                .setNegativeButton(R.string.discard, onDiscard)
                .show();
    }
}
