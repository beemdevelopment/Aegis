package me.impy.aegis;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.CallSuper;

public abstract class ProgressDialogTask<Params, Result> extends AsyncTask<Params, Void, Result> {
    private ProgressDialog _dialog;

    public ProgressDialogTask(Context context, String message) {
        _dialog = new ProgressDialog(context);
        _dialog.setCancelable(false);
        _dialog.setMessage(message);
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

    protected final ProgressDialog getDialog() {
        return _dialog;
    }
}
