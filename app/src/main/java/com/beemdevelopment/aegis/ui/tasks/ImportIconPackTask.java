package com.beemdevelopment.aegis.ui.tasks;

import android.content.Context;
import android.net.Uri;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.icons.IconPack;
import com.beemdevelopment.aegis.icons.IconPackException;
import com.beemdevelopment.aegis.icons.IconPackManager;
import com.beemdevelopment.aegis.util.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImportIconPackTask extends ProgressDialogTask<ImportIconPackTask.Params, ImportIconPackTask.Result> {
    private final ImportIconPackTask.Callback _cb;

    public ImportIconPackTask(Context context, ImportIconPackTask.Callback cb) {
        super(context, context.getString(R.string.importing_icon_pack));
        _cb = cb;
    }

    @Override
    protected ImportIconPackTask.Result doInBackground(ImportIconPackTask.Params... params) {
        Context context = getDialog().getContext();
        ImportIconPackTask.Params param = params[0];

        File tempFile = null;
        try {
            tempFile = File.createTempFile("icon-pack-", "", context.getCacheDir());
            try (InputStream inStream = context.getContentResolver().openInputStream(param.getUri());
                 FileOutputStream outStream = new FileOutputStream(tempFile)) {
                if (inStream == null) {
                    throw new IOException("openInputStream returned null");
                }
                IOUtils.copy(inStream, outStream);
            }

            IconPack pack = param.getManager().importPack(tempFile);
            return new Result(pack, null);
        } catch (IOException | IconPackException e) {
            e.printStackTrace();
            return new ImportIconPackTask.Result(null, e);
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    @Override
    protected void onPostExecute(ImportIconPackTask.Result result) {
        super.onPostExecute(result);
        _cb.onTaskFinished(result);
    }

    public interface Callback {
        void onTaskFinished(ImportIconPackTask.Result result);
    }

    public static class Params {
        private final IconPackManager _manager;
        private final Uri _uri;

        public Params(IconPackManager manager, Uri uri) {
            _manager = manager;
            _uri = uri;
        }

        public IconPackManager getManager() {
            return _manager;
        }

        public Uri getUri() {
            return _uri;
        }
    }

    public static class Result {
        private final IconPack _pack;
        private final Exception _e;

        public Result(IconPack pack, Exception e) {
            _pack = pack;
            _e = e;
        }

        public IconPack getIconPack() {
            return _pack;
        }

        public Exception getException() {
            return _e;
        }
    }
}
