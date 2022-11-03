package com.beemdevelopment.aegis.ui.tasks;

import android.content.Context;
import android.net.Uri;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.helpers.QrCodeHelper;
import com.beemdevelopment.aegis.helpers.SafHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class QrDecodeTask extends ProgressDialogTask<List<Uri>, List<QrDecodeTask.Result>> {
    private final Callback _cb;

    public QrDecodeTask(Context context, Callback cb) {
        super(context, context.getString(R.string.analyzing_qr));
        _cb = cb;
    }

    @Override
    protected List<Result> doInBackground(List<Uri>... params) {
        List<Result> res = new ArrayList<>();
        Context context = getDialog().getContext();

        List<Uri> uris = params[0];
        for (Uri uri : uris) {
            String fileName = SafHelper.getFileName(context, uri);
            if (uris.size() > 1) {
                publishProgress(context.getString(R.string.analyzing_qr_multiple, uris.indexOf(uri) + 1, uris.size(), fileName));
            }

            try (InputStream inStream = context.getContentResolver().openInputStream(uri)) {
                if (inStream == null) {
                    throw new IOException("openInputStream returned null");
                }
                com.google.zxing.Result result = QrCodeHelper.decodeFromStream(inStream);
                res.add(new Result(uri, fileName, result, null));
            } catch (QrCodeHelper.DecodeError | IOException e) {
                e.printStackTrace();
                res.add(new Result(uri, fileName, null, e));
            }
        }

        return res;
    }

    @Override
    protected void onPostExecute(List<Result> results) {
        super.onPostExecute(results);
        _cb.onTaskFinished(results);
    }

    public interface Callback {
        void onTaskFinished(List<Result> results);
    }

    public static class Result {
        private final Uri _uri;
        private final String _fileName;
        private final com.google.zxing.Result _result;
        private final Exception _e;

        public Result(Uri uri, String fileName, com.google.zxing.Result result, Exception e) {
            _uri = uri;
            _fileName = fileName;
            _result = result;
            _e = e;
        }

        public Uri getUri() {
            return _uri;
        }

        public String getFileName() {
            return _fileName;
        }

        public com.google.zxing.Result getResult() {
            return _result;
        }

        public Exception getException() {
            return _e;
        }
    }
}
