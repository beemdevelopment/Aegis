package me.impy.aegis.ui.dialogs;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

public class Dialogs {
    private Dialogs() {

    }

    public static AlertDialog showDeleteEntryDialog(Activity activity, DialogInterface.OnClickListener onDelete) {
        return new AlertDialog.Builder(activity)
                .setTitle("Delete entry")
                .setMessage("Are you sure you want to delete this entry?")
                .setPositiveButton(android.R.string.yes, onDelete)
                .setNegativeButton(android.R.string.no, null)
                .show();
    }
}
