package com.beemdevelopment.aegis.ui.tasks;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Process;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.beemdevelopment.aegis.ui.dialogs.Dialogs;

public abstract class ProgressDialogTask<Params, Result> extends AsyncTask<Params, String, Result> {
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

    @Override
    protected void onProgressUpdate(String... values) {
        if (values.length == 1) {
            _dialog.setMessage(values[0]);
        }
    }

    protected void setPriority() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_MORE_FAVORABLE);
    }

    protected final ProgressDialog getDialog() {
        return _dialog;
    }

    @SafeVarargs
    public final void execute(@Nullable Lifecycle lifecycle, Params... params) {
        if (lifecycle != null) {
            LifecycleObserver observer = new Observer(getDialog());
            lifecycle.addObserver(observer);
        }
        execute(params);
    }

    private static class Observer implements LifecycleObserver {
        private Dialog _dialog;

        public Observer(Dialog dialog) {
            _dialog = dialog;
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        void onPause() {
            if (_dialog != null && _dialog.isShowing()) {
                _dialog.dismiss();
                _dialog = null;
            }
        }
    }
}
