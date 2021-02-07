package com.beemdevelopment.aegis.ui.tasks;

import android.content.Context;
import android.net.Uri;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.util.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * ImportFileTask reads an SAF file from a background thread and
 * writes it to a temporary file in the cache directory.
 */
public class ImportFileTask extends ProgressDialogTask<Uri, ImportFileTask.Result> {
    private final Callback _cb;

    public ImportFileTask(Context context, Callback cb) {
        super(context, context.getString(R.string.reading_file));
        _cb = cb;
    }

    @Override
    protected Result doInBackground(Uri... uris) {
        Context context = getDialog().getContext();

        try (InputStream inStream = context.getContentResolver().openInputStream(uris[0])) {
            File tempFile = File.createTempFile("import-", "", context.getCacheDir());
            try (FileOutputStream outStream = new FileOutputStream(tempFile)) {
                IOUtils.copy(inStream, outStream);
            }

            return new Result(tempFile, null);
        } catch (IOException e) {
            return new Result(null, e);
        }
    }

    @Override
    protected void onPostExecute(Result result) {
        super.onPostExecute(result);
        _cb.onTaskFinished(result);
    }

    public interface Callback {
        void onTaskFinished(Result result);
    }

    public static class Result {
        private final File _file;
        private final Exception _e;

        public Result(File file, Exception e) {
            _file = file;
            _e = e;
        }

        public File getFile() {
            return _file;
        }

        public Exception getException() {
            return _e;
        }
    }
}
