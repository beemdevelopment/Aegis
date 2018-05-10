package me.impy.aegis.ui.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

public class Dialogs {
    private Dialogs() {

    }

    public static AlertDialog showDeleteEntryDialog(Context context, DialogInterface.OnClickListener onDelete) {
        return new AlertDialog.Builder(context)
                .setTitle("Delete entry")
                .setMessage("Are you sure you want to delete this entry?")
                .setPositiveButton(android.R.string.yes, onDelete)
                .setNegativeButton(android.R.string.no, null)
                .show();
    }
}
