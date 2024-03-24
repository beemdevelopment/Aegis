package com.beemdevelopment.aegis.ui.glide;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amulyakhare.textdrawable.TextDrawable;
import com.beemdevelopment.aegis.helpers.TextDrawableHelper;
import com.beemdevelopment.aegis.icons.IconPack;
import com.beemdevelopment.aegis.icons.IconType;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.DrawableImageViewTarget;
import com.bumptech.glide.request.target.Target;

import java.io.File;

public class GlideHelper {
    private GlideHelper() {

    }

    public static void loadIconFile(RequestManager rm, File file, IconType iconType, ImageView targetView) {
        load(rm.load(file), iconType, targetView);
    }

    public static void loadIcon(RequestManager rm, IconPack.Icon icon, ImageView targetView) {
        loadIconFile(rm, icon.getFile(), icon.getIconType(), targetView);
    }

    public static void loadEntryIcon(RequestManager rm, VaultEntry entry, ImageView targetView) {
        if (entry.hasIcon()) {
            setCommonOptions(rm.load(entry.getIcon()), entry.getIcon().getType()).into(targetView);
        } else {
            // Clear any pending loads for targetView, so that the TextDrawable
            // we're about to display doesn't get overwritten when that pending load finishes
            rm.clear(targetView);

            setLayerType(targetView, IconType.INVALID);
            TextDrawable drawable = TextDrawableHelper.generate(entry.getIssuer(), entry.getName(), targetView);
            targetView.setImageDrawable(drawable);
        }
    }

    private static void load(RequestBuilder<Drawable> rb, IconType iconType, ImageView targetView) {
        setCommonOptions(rb, iconType).into(targetView);
    }

    public static RequestBuilder<Drawable> setCommonOptions(RequestBuilder<Drawable> rb, IconType iconType) {
        if (iconType != null) {
            rb = rb.set(VaultEntryIconLoader.ICON_TYPE, iconType)
                    .listener(new ViewReadyListener<>(targetView -> {
                        targetView.setImageTintList(null);
                        setLayerType(targetView, iconType);
                    }));
        }

        return rb.diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(false);
    }

    /**
     * Sets the layer type of the given ImageView based on the given IconType. If the
     * icon type is SVG and SDK <= 27, the layer type is set to software. Otherwise, it
     * is set to hardware.
     */
    private static void setLayerType(ImageView view, IconType iconType) {
        if (iconType == IconType.SVG && Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
            view.setLayerType(ImageView.LAYER_TYPE_SOFTWARE, null);
            return;
        }

        view.setLayerType(ImageView.LAYER_TYPE_HARDWARE, null);
    }

    private static class ViewReadyListener<T> implements RequestListener<T> {
        private final Listener<T> _listener;

        public ViewReadyListener(Listener<T> listener) {
            _listener = listener;
        }

        @Override
        public boolean onLoadFailed(@Nullable GlideException e, @Nullable Object model, @NonNull Target<T> target, boolean isFirstResource) {
            return false;
        }

        @Override
        public boolean onResourceReady(@NonNull T resource, @NonNull Object model, Target<T> target, @NonNull DataSource dataSource, boolean isFirstResource) {
            if (target instanceof DrawableImageViewTarget) {
                DrawableImageViewTarget viewTarget = (DrawableImageViewTarget) target;
                if (_listener != null) {
                    _listener.onConfigureImageView(viewTarget.getView());
                }
            }
            return false;
        }

        public interface Listener<T> {
            void onConfigureImageView(ImageView targetView);
        }
    }
}
