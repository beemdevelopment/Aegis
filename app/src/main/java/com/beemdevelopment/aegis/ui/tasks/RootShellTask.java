package com.beemdevelopment.aegis.ui.tasks;

import android.content.Context;

import com.beemdevelopment.aegis.R;
import com.topjohnwu.superuser.Shell;

public class RootShellTask extends ProgressDialogTask<Object, Shell> {
    private final Callback _cb;

    public RootShellTask(Context context, Callback cb) {
        super(context, context.getString(R.string.requesting_root_access));
        _cb = cb;
    }

    @Override
    protected Shell doInBackground(Object... params) {
        // To access other app's internal storage directory, run libsu commands inside the global mount namespace
        return Shell.Builder.create().setFlags(Shell.FLAG_MOUNT_MASTER).build();
    }

    @Override
    protected void onPostExecute(Shell shell) {
        super.onPostExecute(shell);
        _cb.onTaskFinished(shell);
    }

    public interface Callback {
        void onTaskFinished(Shell shell);
    }
}
