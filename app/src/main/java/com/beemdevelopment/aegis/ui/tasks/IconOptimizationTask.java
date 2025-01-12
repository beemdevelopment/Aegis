package com.beemdevelopment.aegis.ui.tasks;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.helpers.BitmapHelper;
import com.beemdevelopment.aegis.icons.IconType;
import com.beemdevelopment.aegis.vault.VaultEntryIcon;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class IconOptimizationTask extends ProgressDialogTask<Map<UUID, VaultEntryIcon>, Map<UUID, VaultEntryIcon>> {
    private final Callback _cb;

    public IconOptimizationTask(Context context, Callback cb) {
        super(context, context.getString(R.string.optimizing_icon));
        _cb = cb;
    }

    @Override
    protected Map<UUID, VaultEntryIcon> doInBackground(Map<UUID, VaultEntryIcon>... params) {
        Map<UUID, VaultEntryIcon> res = new HashMap<>();
        Context context = getDialog().getContext();

        int i = 0;
        Map<UUID, VaultEntryIcon> icons = params[0];
        for (Map.Entry<UUID, VaultEntryIcon> entry : icons.entrySet()) {
            if (icons.size() > 1) {
                publishProgress(context.getString(R.string.optimizing_icon_multiple, i + 1, icons.size()));
            }
            i++;

            VaultEntryIcon oldIcon = entry.getValue();
            if (oldIcon == null || oldIcon.getType().equals(IconType.SVG)) {
                continue;
            }
            if (BitmapHelper.isVaultEntryIconOptimized(oldIcon)) {
                continue;
            }

            Bitmap bitmap = BitmapFactory.decodeByteArray(oldIcon.getBytes(), 0, oldIcon.getBytes().length);
            VaultEntryIcon newIcon = BitmapHelper.toVaultEntryIcon(bitmap, oldIcon.getType());
            bitmap.recycle();
            res.put(entry.getKey(), newIcon);
        }

        return res;
    }

    @Override
    protected void onPostExecute(Map<UUID, VaultEntryIcon> results) {
        super.onPostExecute(results);
        _cb.onTaskFinished(results);
    }

    public interface Callback {
        void onTaskFinished(Map<UUID, VaultEntryIcon> results);
    }
}
