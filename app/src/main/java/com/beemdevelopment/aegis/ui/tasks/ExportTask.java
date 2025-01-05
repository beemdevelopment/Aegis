package com.beemdevelopment.aegis.ui.tasks;

import android.content.Context;
import android.net.Uri;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.util.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ExportTask extends ProgressDialogTask<ExportTask.Params, Exception> {
    private final Callback _cb;

    public ExportTask(Context context, Callback cb) {
        super(context, context.getString(R.string.exporting_vault));
        _cb = cb;
    }

    @Override
    protected Exception doInBackground(ExportTask.Params... args) {
        setPriority();

        ExportTask.Params params = args[0];
        try (InputStream inStream = new FileInputStream(params.getFile());
             OutputStream outStream = getDialog().getContext().getContentResolver().openOutputStream(params.getDestUri(), "w")) {
            if (outStream == null) {
                throw new IOException("openOutputStream returned null");
            }
            IOUtils.copy(inStream, outStream);
            return null;
        } catch (IOException e) {
            return e;
        } finally {
            boolean ignored = params.getFile().delete();
        }
    }

    @Override
    protected void onPostExecute(Exception e) {
        super.onPostExecute(e);
        _cb.onTaskFinished(e);
    }

    public static class Params {
        private final File _file;
        private final Uri _destUri;

        public Params(File file, Uri destUri) {
            _file = file;
            _destUri = destUri;
        }

        public File getFile() {
            return _file;
        }

        public Uri getDestUri() {
            return _destUri;
        }
    }

    public interface Callback {
        void onTaskFinished(Exception e);
    }
}
