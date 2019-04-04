package com.beemdevelopment.aegis.ui.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Process;

import com.beemdevelopment.aegis.ui.Dialogs;

import androidx.annotation.CallSuper;

public abstract class ProgressDialogTask<Params, Result> extends AsyncTask<Params, Void, Result> {
    private ProgressDialog _dialog;

    public ProgressDialogTask(Context context, String message) {
        _dialog = new ProgressDialog(context);
        _dialog.setCancelable(false);
        _dialog.setMessage(message);
        Dialogs.secureDialog(_dialog);
    }

    @CallSuper
    @Override
    protected void onPreExecute() {
        _dialog.show();
    }

    @CallSuper
    @Override
    protected void onPostExecute(Result result) {
        if (_dialog.isShowing()) {
            _dialog.dismiss();
        }
    }

    protected void setPriority() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_MORE_FAVORABLE);
    }

    protected final ProgressDialog getDialog() {
        return _dialog;
    }
}
