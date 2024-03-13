package com.beemdevelopment.aegis.ui.tasks;

import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Process;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public abstract class ProgressDialogTask<Params, Result> extends AsyncTask<Params, String, Result> {
    private final AlertDialog _dialog;
    private final TextView _textProgress;

    public ProgressDialogTask(Context context, String message) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_progress, null);
        _textProgress = view.findViewById(R.id.text_progress);
        _textProgress.setText(message);

        _dialog = new MaterialAlertDialogBuilder(context)
                .setView(view)
                .setCancelable(false)
                .create();

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
            _textProgress.setText(values[0]);
        }
    }

    protected void setPriority() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_MORE_FAVORABLE);
    }

    protected final AlertDialog getDialog() {
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
