package com.beemdevelopment.aegis.ui.glide;

import androidx.annotation.NonNull;

import com.beemdevelopment.aegis.icons.IconType;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

import java.nio.ByteBuffer;

public class IconLoader implements ModelLoader<VaultEntry, ByteBuffer> {
    public static final Option<IconType> ICON_TYPE = Option.memory("ICON_TYPE", IconType.INVALID);

    @Override
    public LoadData<ByteBuffer> buildLoadData(@NonNull VaultEntry model, int width, int height, @NonNull Options options) {
        return new LoadData<>(new UUIDKey(model.getUUID()), new Fetcher(model));
    }

    @Override
    public boolean handles(@NonNull VaultEntry model) {
        return true;
    }

    public static class Fetcher implements DataFetcher<ByteBuffer> {
        private final VaultEntry _model;

        private Fetcher(VaultEntry model) {
            _model = model;
        }

        @Override
        public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super ByteBuffer> callback) {
            byte[] bytes = _model.getIcon();
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            callback.onDataReady(buf);
        }

        @Override
        public void cleanup() {

        }

        @Override
        public void cancel() {

        }

        @NonNull
        @Override
        public Class<ByteBuffer> getDataClass() {
            return ByteBuffer.class;
        }

        @NonNull
        @Override
        public DataSource getDataSource() {
            return DataSource.MEMORY_CACHE;
        }
    }

    public static class Factory implements ModelLoaderFactory<VaultEntry, ByteBuffer> {
        @NonNull
        @Override
        public ModelLoader<VaultEntry, ByteBuffer> build(@NonNull MultiModelLoaderFactory unused) {
            return new IconLoader();
        }

        @Override
        public void teardown() {

        }
    }
}
