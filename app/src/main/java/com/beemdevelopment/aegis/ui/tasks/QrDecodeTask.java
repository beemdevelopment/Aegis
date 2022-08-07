package com.beemdevelopment.aegis.ui.tasks;

import android.content.Context;
import android.net.Uri;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.helpers.QrCodeHelper;
import com.google.zxing.Result;

import java.io.IOException;
import java.io.InputStream;

public class QrDecodeTask extends ProgressDialogTask<Uri, QrDecodeTask.Response> {
    private final Callback _cb;

    public QrDecodeTask(Context context, Callback cb) {
        super(context, context.getString(R.string.analyzing_qr));
        _cb = cb;
    }

    @Override
    protected Response doInBackground(Uri... params) {
        Context context = getDialog().getContext();

        Uri uri = params[0];
        try (InputStream inStream = context.getContentResolver().openInputStream(uri)) {
            Result result = QrCodeHelper.decodeFromStream(inStream);
            return new Response(result, null);
        } catch (QrCodeHelper.DecodeError | IOException e) {
            e.printStackTrace();
            return new Response(null, e);
        }
    }

    @Override
    protected void onPostExecute(Response result) {
        super.onPostExecute(result);
        _cb.onTaskFinished(result);
    }

    public interface Callback {
        void onTaskFinished(Response result);
    }

    public static class Response {
        private final Result _result;
        private final Exception _e;

        public Response(Result result, Exception e) {
            _result = result;
            _e = e;
        }

        public Result getResult() {
            return _result;
        }

        public Exception getException() {
            return _e;
        }
    }
}
